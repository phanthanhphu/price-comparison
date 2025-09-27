package org.bsl.pricecomparison.controller;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bsl.pricecomparison.costom.SupplierProductRepositoryCustom;
import org.bsl.pricecomparison.dto.SupplierProductDTO;
import org.bsl.pricecomparison.exception.DuplicateSupplierProductException;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.repository.*;
import org.bsl.pricecomparison.request.CreateProductRequest;
import org.bsl.pricecomparison.request.UpdateProductRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/supplier-products")
public class SupplierProductController {

    @Autowired
    private SupplierProductRepository repository;

    @Autowired
    private ProductType1Repository productType1Repository;

    @Autowired
    private ProductType2Repository productType2Repository;

    @Autowired
    private SupplierProductRepositoryCustom supplierProductRepositoryCustom;

    @Autowired
    private SummaryRequisitionRepository summaryRequisitionRepository;

    @Autowired
    private RequisitionMonthlyRepository requisitionMonthlyRepository;

    private static final String UPLOAD_DIR = "uploads/";
    private static final Logger logger = LoggerFactory.getLogger(SupplierProductController.class);

    private boolean checkDuplicate(SupplierProduct product, String excludeId) {
        Double price = product.getPrice() != null ? product.getPrice() : 0.0;
        return repository.existsBySupplierCodeAndSapCodeAndPriceAndIdNot(
                product.getSupplierCode(),
                product.getSapCode(),
                price,
                excludeId != null ? excludeId : ""
        );
    }

    @GetMapping
    @Operation(
            summary = "Get all supplier products",
            description = "Retrieve a paginated list of all supplier products sorted by creation date in descending order."
    )
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<SupplierProduct> products = repository.findAll(pageable);

            Page<SupplierProductDTO> result = products.map(product -> {
                SupplierProductDTO dto = new SupplierProductDTO();
                BeanUtils.copyProperties(product, dto);

                if (product.getImageUrls() == null) {
                    dto.setImageUrls(new ArrayList<>());
                }

                if (product.getProductType1Id() != null) {
                    productType1Repository.findById(product.getProductType1Id()).ifPresent(type1 -> {
                        dto.setProductType1Name(type1.getName());
                    });
                }

                if (product.getProductType2Id() != null) {
                    productType2Repository.findById(product.getProductType2Id()).ifPresent(type2 -> {
                        dto.setProductType2Name(type2.getName());
                    });
                }

                return dto;
            });

            return ResponseEntity.ok(Map.of("message", "Supplier products retrieved successfully", "data", result));
        } catch (Exception e) {
            logger.error("Failed to fetch supplier products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve supplier products: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        try {
            Optional<SupplierProduct> optional = repository.findById(id);
            if (optional.isPresent()) {
                return ResponseEntity.ok(Map.of("message", "Supplier product retrieved successfully", "data", optional.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Supplier product not found with ID: " + id));
            }
        } catch (Exception e) {
            logger.error("Failed to fetch supplier product with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve supplier product: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid supplier product ID"));
            }

            Optional<SupplierProduct> optional = repository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Supplier product not found with ID: " + id));
            }

            SupplierProduct supplierProduct = optional.get();
            String supplierName = supplierProduct.getSupplierName();

            // Check if the product is used in urgent or monthly requests
            if (summaryRequisitionRepository.existsBySupplierId(supplierProduct.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", String.format("Cannot delete supplier '%s' as it is used in urgent requests.", supplierName)));
            }

            if (requisitionMonthlyRepository.existsBySupplierId(supplierProduct.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", String.format("Cannot delete supplier '%s' as it is used in monthly requests.", supplierName)));
            }

            // Delete associated image files
            List<String> imageUrls = supplierProduct.getImageUrls();
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (String imageUrl : imageUrls) {
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try {
                            Path path = Paths.get(UPLOAD_DIR + imageUrl.replace("/uploads/", ""));
                            Files.deleteIfExists(path);
                            logger.info("Deleted image file: {}", path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete image file {}: {}", imageUrl, e.getMessage());
                        }
                    }
                }
            }

            // Delete the product from the database
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Supplier product and associated images deleted successfully"));
        } catch (Exception e) {
            logger.error("Failed to delete supplier product with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete supplier product: " + e.getMessage()));
        }
    }

    @GetMapping("/check-usage/{id}")
    @Operation(
            summary = "Check if supplier product is used in requests",
            description = "Check if the supplier product with the given ID is used in urgent or monthly requests. Returns true if used, false otherwise."
    )
    public ResponseEntity<Map<String, Object>> checkSupplierUsage(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid supplier product ID", "data", false));
            }

            Optional<SupplierProduct> optional = repository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Supplier product not found with ID: " + id, "data", false));
            }

            SupplierProduct supplierProduct = optional.get();

            // Check if the product is used in urgent or monthly requests
            boolean isUsed = summaryRequisitionRepository.existsBySupplierId(supplierProduct.getId()) ||
                    requisitionMonthlyRepository.existsBySupplierId(supplierProduct.getId());

            return ResponseEntity.ok(Map.of(
                    "message", isUsed ? "Supplier product is used in requests" : "Supplier product is not used in requests",
                    "data", isUsed
            ));
        } catch (Exception e) {
            logger.error("Failed to check supplier product usage for ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to check supplier product usage: " + e.getMessage(), "data", false));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchByItemNoOrSapCode(
            @RequestParam(required = false) String itemNo,
            @RequestParam(required = false) String sapCode) {
        try {
            List<SupplierProduct> products;
            if (itemNo != null && sapCode != null) {
                products = repository.findByItemNoContainingIgnoreCaseAndSapCodeContainingIgnoreCase(itemNo, sapCode);
            } else if (itemNo != null) {
                products = repository.findByItemNoContainingIgnoreCase(itemNo);
            } else if (sapCode != null) {
                products = repository.findBySapCodeContainingIgnoreCase(sapCode);
            } else {
                products = repository.findAll();
            }
            return ResponseEntity.ok(Map.of("message", "Supplier products retrieved successfully", "data", products));
        } catch (Exception e) {
            logger.error("Failed to search supplier products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search supplier products: " + e.getMessage()));
        }
    }

    @GetMapping("/by-requisition")
    public ResponseEntity<Map<String, Object>> getByRequisition(
            @RequestParam String sapCode,
            @RequestParam String supplierId) {
        try {
            List<SupplierProduct> products = repository.findBySapCodeAndSupplierCode(sapCode, supplierId);
            return ResponseEntity.ok(Map.of("message", "Supplier products retrieved successfully", "data", products));
        } catch (Exception e) {
            logger.error("Failed to fetch supplier products by requisition: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve supplier products by requisition: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import from Excel", description = "Upload an Excel .xlsx file with columns in the order: supplierCode, supplierName, sapCode, itemNo, itemDescription, size, price, unit")
    public ResponseEntity<Map<String, Object>> importExcel(
            @Parameter(description = "Excel .xlsx file containing product data")
            @RequestPart("file") MultipartFile file) {
        try {
            List<SupplierProduct> products = new ArrayList<>();

            try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();
                boolean firstRow = true;

                for (Row row : sheet) {
                    if (firstRow) {
                        firstRow = false;
                        continue;
                    }

                    String supplierCode = formatter.formatCellValue(row.getCell(0));
                    String supplierName = formatter.formatCellValue(row.getCell(1));
                    String sapCode = formatter.formatCellValue(row.getCell(2));
                    String itemNo = formatter.formatCellValue(row.getCell(3));
                    String itemDescription = formatter.formatCellValue(row.getCell(4));
                    String size = formatter.formatCellValue(row.getCell(5));
                    String priceText = formatter.formatCellValue(row.getCell(6));
                    String unit = formatter.formatCellValue(row.getCell(7));

                    Double price = null;
                    if (priceText != null && !priceText.isEmpty()) {
                        priceText = priceText.replace(",", "");
                        try {
                            price = Double.parseDouble(priceText);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid price format at row " + (row.getRowNum() + 1));
                        }
                    }

                    boolean exists = repository.existsBySupplierCodeAndSapCodeAndPrice(supplierCode, sapCode, price);
                    if (exists) {
                        throw new DuplicateSupplierProductException(String.format(
                                "Duplicate entry at row %d: supplierCode='%s', sapCode='%s', price=%.2f already exists",
                                row.getRowNum() + 1, supplierCode, sapCode, price));
                    }

                    SupplierProduct p = new SupplierProduct();
                    p.setSupplierCode(supplierCode);
                    p.setSupplierName(supplierName);
                    p.setSapCode(sapCode);
                    p.setItemNo(itemNo);
                    p.setItemDescription(itemDescription);
                    p.setFullDescription(""); // Default value
                    p.setMaterialGroupFullDescription(""); // Default value
                    p.setCurrency("VND"); // Default value
                    p.setSize(size);
                    p.setPrice(price);
                    p.setUnit(unit);

                    products.add(p);
                }
            }

            List<SupplierProduct> savedProducts = repository.saveAll(products);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Supplier products imported successfully", "data", savedProducts));
        } catch (DuplicateSupplierProductException e) {
            logger.error("Duplicate supplier product error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to import supplier products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to import supplier products: " + e.getMessage()));
        }
    }

    @GetMapping("/search-by-name-or-sapcode")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String sapCode,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String itemNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (Stream.of(sapCode, supplierCode, itemNo, supplierName)
                    .allMatch(StringUtils::isBlank)) {
                Page<SupplierProduct> result = repository.findAll(PageRequest.of(page, size));
                return ResponseEntity.ok(Map.of("message", "Supplier products retrieved successfully", "data", result));
            }

            PageRequest pageRequest = PageRequest.of(page, size);

            Page<SupplierProduct> result = repository.findBySapCodeContainingIgnoreCaseOrSupplierCodeContainingIgnoreCaseOrItemNoContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
                    sapCode, supplierCode, itemNo, supplierName, pageRequest);
            return ResponseEntity.ok(Map.of("message", "Supplier products retrieved successfully", "data", result));
        } catch (Exception e) {
            logger.error("Failed to search supplier products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search supplier products: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new product with multiple image uploads",
            description = "Create a product and upload multiple images using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CreateProductRequest.class)
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> createProductWithFile(
            @ModelAttribute CreateProductRequest request
    ) {
        try {
            if (repository.existsBySupplierCodeAndSapCodeAndPrice(
                    request.getSupplierCode(), request.getSapCode(), request.getPrice())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Supplier product with this supplier code, SAP code, and price already exists"));
            }

            List<String> imageUrls = new ArrayList<>();
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        imageUrls.add(imageUrl);
                    }
                }
            }

            SupplierProduct product = new SupplierProduct();
            product.setSupplierCode(request.getSupplierCode());
            product.setSupplierName(request.getSupplierName());
            product.setSapCode(request.getSapCode());
            product.setItemNo(request.getItemNo());
            product.setItemDescription(request.getItemDescription());
            product.setFullDescription(request.getFullDescription());
            product.setMaterialGroupFullDescription(request.getMaterialGroupFullDescription());
            product.setCurrency(request.getCurrency());
            product.setSize(request.getSize());
            product.setPrice(request.getPrice());
            product.setUnit(request.getUnit());
            product.setImageUrls(imageUrls);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            if (request.getProductType1Id() != null) {
                product.setProductType1Id(request.getProductType1Id());
            }
            if (request.getProductType2Id() != null) {
                product.setProductType2Id(request.getProductType2Id());
            }

            SupplierProduct savedProduct = repository.save(product);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Supplier product created successfully", "data", savedProduct));
        } catch (IOException e) {
            logger.error("Failed to create supplier product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create supplier product: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update a product with optional multiple image uploads and deletions",
            description = "Update product fields, optionally upload new images, and delete existing images using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UpdateProductRequest.class)
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> updateProduct(
            @PathVariable String id,
            @ModelAttribute UpdateProductRequest request
    ) {
        try {
            // Find the existing product
            SupplierProduct existingProduct = repository.findById(id).orElse(null);
            if (existingProduct == null) {
                logger.warn("Product with ID {} not found", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Supplier product not found with ID: " + id));
            }

            // Create a temporary product for duplicate checking
            SupplierProduct tempProduct = new SupplierProduct();
            tempProduct.setSupplierCode(request.getSupplierCode() != null ? request.getSupplierCode() : existingProduct.getSupplierCode());
            tempProduct.setSapCode(request.getSapCode() != null ? request.getSapCode() : existingProduct.getSapCode());
            tempProduct.setPrice(request.getPrice() != null ? request.getPrice() : (existingProduct.getPrice() != null ? existingProduct.getPrice() : 0.0));

            // Check for duplicates, excluding the current product's ID
            if (checkDuplicate(tempProduct, id)) {
                logger.warn("Duplicate product found for supplierCode: {}, sapCode: {}, price: {}",
                        tempProduct.getSupplierCode(), tempProduct.getSapCode(), tempProduct.getPrice());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Supplier product with this supplier code, SAP code, and price already exists"));
            }

            // Update fields
            existingProduct.setSupplierCode(tempProduct.getSupplierCode());
            existingProduct.setSupplierName(request.getSupplierName() != null ? request.getSupplierName() : existingProduct.getSupplierName());
            existingProduct.setSapCode(tempProduct.getSapCode());
            existingProduct.setItemNo(request.getItemNo() != null ? request.getItemNo() : existingProduct.getItemNo());
            existingProduct.setItemDescription(request.getItemDescription() != null ? request.getItemDescription() : existingProduct.getItemDescription());
            existingProduct.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : existingProduct.getFullDescription());
            existingProduct.setMaterialGroupFullDescription(request.getMaterialGroupFullDescription() != null ? request.getMaterialGroupFullDescription() : existingProduct.getMaterialGroupFullDescription());
            existingProduct.setCurrency(request.getCurrency() != null ? request.getCurrency() : existingProduct.getCurrency());
            existingProduct.setSize(request.getSize() != null ? request.getSize() : existingProduct.getSize());
            existingProduct.setPrice(tempProduct.getPrice());
            existingProduct.setUnit(request.getUnit() != null ? request.getUnit() : existingProduct.getUnit());
            existingProduct.setUpdatedAt(LocalDateTime.now());

            // Update product type IDs if provided
            if (request.getProductType1Id() != null) {
                existingProduct.setProductType1Id(request.getProductType1Id());
            }
            if (request.getProductType2Id() != null) {
                existingProduct.setProductType2Id(request.getProductType2Id());
            }

            // Handle image uploads and deletions
            List<String> imageUrls = new ArrayList<>(existingProduct.getImageUrls() != null ? existingProduct.getImageUrls() : new ArrayList<>());

            // Remove images specified in imagesToDelete
            List<String> imagesToDelete = request.getImagesToDelete();
            if (imagesToDelete != null && !imagesToDelete.isEmpty()) {
                for (String imageUrl : imagesToDelete) {
                    if (imageUrl != null && imageUrls.contains(imageUrl)) {
                        imageUrls.remove(imageUrl);
                        try {
                            Path path = Paths.get(UPLOAD_DIR + imageUrl.replace("/uploads/", ""));
                            Files.deleteIfExists(path);
                            logger.info("Deleted image file: {}", path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete image file {}: {}", imageUrl, e.getMessage());
                        }
                    } else {
                        logger.warn("Image URL {} not found in product or invalid", imageUrl);
                    }
                }
            }

            // Add new images
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        imageUrls.add(imageUrl);
                        logger.info("Uploaded new image: {}", imageUrl);
                    }
                }
            }

            // Validate image count
            if (imageUrls.size() > 10) {
                logger.warn("Too many images for product ID {}: {}", id, imageUrls.size());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Too many images provided (maximum 10)"));
            }

            // Update image URLs
            existingProduct.setImageUrls(imageUrls);

            // Save the updated product
            SupplierProduct updatedProduct = repository.save(existingProduct);
            logger.info("Successfully updated product with ID {}", id);
            return ResponseEntity.ok(Map.of("message", "Supplier product updated successfully", "data", updatedProduct));

        } catch (IOException e) {
            logger.error("Error updating product with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update supplier product: " + e.getMessage()));
        }
    }

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IOException("Only JPEG, PNG, and GIF files are allowed");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());

        file.transferTo(path);

        return "/uploads/" + fileName;
    }

    @GetMapping("/filter")
    @Operation(
            summary = "Filter supplier products",
            description = "Retrieve a paginated list of supplier products filtered by the provided criteria."
    )
    public ResponseEntity<Map<String, Object>> filterSupplierProducts(
            @RequestParam(required = false, defaultValue = "") String supplierCode,
            @RequestParam(required = false, defaultValue = "") String supplierName,
            @RequestParam(required = false, defaultValue = "") String sapCode,
            @RequestParam(required = false, defaultValue = "") String itemNo,
            @RequestParam(required = false, defaultValue = "") String itemDescription,
            @RequestParam(required = false, defaultValue = "") String fullDescription,
            @RequestParam(required = false, defaultValue = "") String materialGroupFullDescription,
            @RequestParam(required = false, defaultValue = "") String productType1Id,
            @RequestParam(required = false, defaultValue = "") String productType2Id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Order.desc("updatedAt"))
            );

            Page<SupplierProduct> supplierProducts = supplierProductRepositoryCustom.filterSupplierProducts(
                    supplierCode,
                    supplierName,
                    sapCode,
                    itemNo,
                    itemDescription,
                    fullDescription,
                    materialGroupFullDescription,
                    productType1Id,
                    productType2Id,
                    pageable
            );

            Page<SupplierProductDTO> result = supplierProducts.map(product -> {
                SupplierProductDTO dto = new SupplierProductDTO();
                dto.setId(Objects.toString(product.getId(), ""));
                dto.setSupplierCode(Objects.toString(product.getSupplierCode(), ""));
                dto.setSupplierName(Objects.toString(product.getSupplierName(), ""));
                dto.setSapCode(Objects.toString(product.getSapCode(), ""));
                dto.setItemNo(Objects.toString(product.getItemNo(), ""));
                dto.setItemDescription(Objects.toString(product.getItemDescription(), ""));
                dto.setFullDescription(Objects.toString(product.getFullDescription(), ""));
                dto.setMaterialGroupFullDescription(Objects.toString(product.getMaterialGroupFullDescription(), ""));
                dto.setCurrency(Objects.toString(product.getCurrency(), ""));
                dto.setSize(Objects.toString(product.getSize(), ""));
                dto.setPrice(product.getPrice() != null ? product.getPrice() : 0.0);
                dto.setUnit(Objects.toString(product.getUnit(), ""));
                dto.setImageUrls(product.getImageUrls() != null ? product.getImageUrls() : new ArrayList<>());
                dto.setProductType1Id(Objects.toString(product.getProductType1Id(), ""));
                dto.setProductType2Id(Objects.toString(product.getProductType2Id(), ""));

                if (product.getProductType1Id() != null && !product.getProductType1Id().isEmpty()) {
                    productType1Repository.findById(product.getProductType1Id()).ifPresent(type1 -> {
                        dto.setProductType1Name(Objects.toString(type1.getName(), ""));
                    });
                } else {
                    dto.setProductType1Name("");
                }

                if (product.getProductType2Id() != null && !product.getProductType2Id().isEmpty()) {
                    productType2Repository.findById(product.getProductType2Id()).ifPresent(type2 -> {
                        dto.setProductType2Name(Objects.toString(type2.getName(), ""));
                    });
                } else {
                    dto.setProductType2Name("");
                }

                return dto;
            });

            return ResponseEntity.ok(Map.of("message", "Supplier products filtered successfully", "data", result));
        } catch (Exception e) {
            logger.error("Failed to filter supplier products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to filter supplier products: " + e.getMessage()));
        }
    }

    @GetMapping("/filter-by-sapcode")
    @Operation(
            summary = "Filter supplier products by SAP code, item number, and item description",
            description = "Retrieve a paginated list of supplier products filtered by SAP code, item number, and item description, sorted by price from low to high."
    )
    public ResponseEntity<Map<String, Object>> filterBySapCodeItemNoAndDescription(
            @RequestParam(required = false, defaultValue = "") String sapCode,
            @RequestParam(required = false, defaultValue = "") String itemNo,
            @RequestParam(required = false, defaultValue = "") String itemDescription,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(Sort.Order.asc("price"))
            );

            Page<SupplierProduct> supplierProducts = supplierProductRepositoryCustom.findBySapCodeWithPagination(
                    sapCode,
                    itemNo,
                    itemDescription,
                    pageable
            );

            Page<SupplierProductDTO> result = supplierProducts.map(product -> {
                SupplierProductDTO dto = new SupplierProductDTO();
                dto.setId(Objects.toString(product.getId(), ""));
                dto.setSupplierCode(Objects.toString(product.getSupplierCode(), ""));
                dto.setSupplierName(Objects.toString(product.getSupplierName(), ""));
                dto.setSapCode(Objects.toString(product.getSapCode(), ""));
                dto.setItemNo(Objects.toString(product.getItemNo(), ""));
                dto.setItemDescription(Objects.toString(product.getItemDescription(), ""));
                dto.setFullDescription(Objects.toString(product.getFullDescription(), ""));
                dto.setMaterialGroupFullDescription(Objects.toString(product.getMaterialGroupFullDescription(), ""));
                dto.setCurrency(Objects.toString(product.getCurrency(), ""));
                dto.setSize(Objects.toString(product.getSize(), ""));
                dto.setPrice(product.getPrice() != null ? product.getPrice() : 0.0);
                dto.setUnit(Objects.toString(product.getUnit(), ""));
                dto.setImageUrls(product.getImageUrls() != null ? product.getImageUrls() : new ArrayList<>());
                dto.setProductType1Id(Objects.toString(product.getProductType1Id(), ""));
                dto.setProductType2Id(Objects.toString(product.getProductType2Id(), ""));

                if (product.getProductType1Id() != null && !product.getProductType1Id().isEmpty()) {
                    productType1Repository.findById(product.getProductType1Id()).ifPresent(type1 -> {
                        dto.setProductType1Name(Objects.toString(type1.getName(), ""));
                    });
                } else {
                    dto.setProductType1Name("");
                }

                if (product.getProductType2Id() != null && !product.getProductType2Id().isEmpty()) {
                    productType2Repository.findById(product.getProductType2Id()).ifPresent(type2 -> {
                        dto.setProductType2Name(Objects.toString(type2.getName(), ""));
                    });
                } else {
                    dto.setProductType2Name("");
                }

                return dto;
            });

            return ResponseEntity.ok(Map.of("message", "Supplier products filtered successfully", "data", result));
        } catch (Exception e) {
            logger.error("Failed to filter supplier products by SAP code: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to filter supplier products by SAP code: " + e.getMessage()));
        }
    }
}