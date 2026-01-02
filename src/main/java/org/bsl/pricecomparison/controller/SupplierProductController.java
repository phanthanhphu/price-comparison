package org.bsl.pricecomparison.controller;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.bsl.pricecomparison.costom.SupplierProductRepositoryCustom;
import org.bsl.pricecomparison.dto.SupplierProductDTO;
import org.bsl.pricecomparison.exception.DuplicateSupplierProductException;
import org.bsl.pricecomparison.model.ProductType1;
import org.bsl.pricecomparison.model.ProductType2;
import org.bsl.pricecomparison.model.RequisitionMonthly;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.text.DecimalFormat;
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
        if (product.getPrice() == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        BigDecimal price = product.getPrice();
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
    @Operation(summary = "Import from Excel", description = "Upload an Excel .xlsx file with columns in the order: supplierCode, supplierName, sapCode, itemNo, itemDescription, size, price, unit, currency, goodType")
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
                    String currency = formatter.formatCellValue(row.getCell(8));
                    String goodType = formatter.formatCellValue(row.getCell(9));

                    // Validate currency
                    if (currency == null || currency.trim().isEmpty()) {
                        currency = "VND"; // Default to VND if not specified
                    }
                    currency = currency.trim().toUpperCase();
                    if (!"VND".equals(currency) && !"USD".equals(currency) && !"EURO".equals(currency)) {
                        throw new IllegalArgumentException("Invalid currency at row " + (row.getRowNum() + 1) + ": Must be VND, USD, or EURO");
                    }

                    // Validate goodType
                    if (goodType == null || goodType.trim().isEmpty()) {
                        goodType = "Common"; // Default to Common if not specified
                    }
                    goodType = goodType.trim();
                    if (!"Common".equals(goodType) && !"Special".equals(goodType) && !"Electronics".equals(goodType)) {
                        throw new IllegalArgumentException("Invalid goodType at row " + (row.getRowNum() + 1) + ": Must be Common, Special, or Electronics");
                    }

                    BigDecimal price = null;
                    if (priceText != null && !priceText.isEmpty()) {
                        logger.debug("Raw priceText at row {}: {}", row.getRowNum() + 1, priceText);
                        String cleanedPriceText = priceText.trim();
                        if ("VND".equals(currency)) {
                            // For VND, remove all commas and dots (thousands separators)
                            cleanedPriceText = cleanedPriceText.replaceAll("[,.]", "");
                        } else {
                            // For USD and EURO, replace comma with dot and ensure only one dot
                            cleanedPriceText = cleanedPriceText.replace(",", ".");
                            cleanedPriceText = cleanedPriceText.replaceAll("[^0-9.]", ""); // Keep only numbers and one dot
                            // Ensure only one decimal point
                            int dotCount = cleanedPriceText.length() - cleanedPriceText.replace(".", "").length();
                            if (dotCount > 1) {
                                throw new IllegalArgumentException("Invalid price format at row " + (row.getRowNum() + 1) +
                                        ": Multiple decimal points detected in " + priceText);
                            }
                        }
                        try {
                            price = new BigDecimal(cleanedPriceText);
                            logger.debug("Parsed price before validation at row {}: {}", row.getRowNum() + 1, price);
                            int scale = price.scale();
                            if ("VND".equals(currency) && scale > 0) {
                                throw new IllegalArgumentException("Invalid price format at row " + (row.getRowNum() + 1) +
                                        ": VND must have no decimal places, got " + priceText);
                            }
                            if (("USD".equals(currency) || "EURO".equals(currency)) && scale != 2) {
                                price = price.setScale(2, RoundingMode.HALF_UP); // Normalize to 2 decimal places
                                logger.warn("Price at row {} normalized to 2 decimal places: {}", row.getRowNum() + 1, price);
                            }
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid price format at row " + (row.getRowNum() + 1));
                        }
                    }

                    boolean exists = repository.existsBySupplierCodeAndSapCodeAndPrice(supplierCode, sapCode, price);
                    if (exists) {
                        throw new DuplicateSupplierProductException(String.format(
                                "Duplicate entry at row %d: supplierCode='%s', sapCode='%s', price=%s already exists",
                                row.getRowNum() + 1, supplierCode, sapCode, price != null ? price.toString() : "null"));
                    }

                    SupplierProduct p = new SupplierProduct();
                    p.setSupplierCode(supplierCode);
                    p.setSupplierName(supplierName);
                    p.setSapCode(sapCode);
                    p.setHanaSapCode(itemNo);
                    p.setItemDescriptionEN(itemDescription);
                    p.setItemDescriptionVN(itemDescription); // Use itemDescription as fullDescription if not provided
                    p.setMaterialGroupFullDescription(""); // Default value
                    p.setCurrency(currency);
                    p.setGoodType(goodType);
                    p.setSize(size);
                    p.setPrice(price);
                    p.setUnit(unit);
                    p.setCreatedAt(LocalDateTime.now());
                    p.setUpdatedAt(LocalDateTime.now());

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
            requestBody = @RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CreateProductRequest.class)
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> createProductWithFile(
            @Valid @ModelAttribute CreateProductRequest request
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
            product.setHanaSapCode(request.getHanaSapCode());
            product.setItemDescriptionEN(request.getItemDescriptionEN());
            product.setItemDescriptionVN(request.getItemDescriptionVN());
            product.setCurrency(request.getCurrency());
            product.setGoodType(request.getGoodType());
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
            @Valid @ModelAttribute UpdateProductRequest request
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
            tempProduct.setPrice(request.getPrice() != null ? request.getPrice() : (existingProduct.getPrice() != null ? existingProduct.getPrice() : BigDecimal.ZERO));

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
            existingProduct.setHanaSapCode(request.getHanaSapCode() != null ? request.getHanaSapCode() : existingProduct.getHanaSapCode());
            existingProduct.setItemDescriptionEN(request.getItemDescriptionEN() != null ? request.getItemDescriptionEN() : existingProduct.getItemDescriptionEN());
            existingProduct.setItemDescriptionVN(request.getItemDescriptionVN() != null ? request.getItemDescriptionVN() : existingProduct.getItemDescriptionVN());
            existingProduct.setCurrency(request.getCurrency() != null ? request.getCurrency() : existingProduct.getCurrency());
            existingProduct.setGoodType(request.getGoodType() != null ? request.getGoodType() : existingProduct.getGoodType());
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

            @RequestParam(required = false, defaultValue = "") String hanaSapCode,        // ← itemNo
            @RequestParam(required = false, defaultValue = "") String itemDescriptionEN,  // ← itemDescription
            @RequestParam(required = false, defaultValue = "") String itemDescriptionVN,  // ← fullDescription

            @RequestParam(required = false, defaultValue = "") String currency,
            @RequestParam(required = false, defaultValue = "") String goodType,
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
                    hanaSapCode,
                    itemDescriptionEN,
                    itemDescriptionVN,
                    currency,
                    goodType,
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

                dto.setHanaSapCode(Objects.toString(product.getHanaSapCode(), ""));
                dto.setItemDescriptionEN(Objects.toString(product.getItemDescriptionEN(), ""));
                dto.setItemDescriptionVN(Objects.toString(product.getItemDescriptionVN(), ""));

                dto.setCurrency(Objects.toString(product.getCurrency(), ""));
                dto.setGoodType(Objects.toString(product.getGoodType(), ""));
                dto.setSize(Objects.toString(product.getSize(), ""));
                dto.setUnit(Objects.toString(product.getUnit(), ""));
                dto.setImageUrls(product.getImageUrls() != null ? product.getImageUrls() : new ArrayList<>());
                dto.setProductType1Id(Objects.toString(product.getProductType1Id(), ""));
                dto.setProductType2Id(Objects.toString(product.getProductType2Id(), ""));
                dto.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
                dto.setCreatedAt(product.getCreatedAt());
                dto.setUpdatedAt(product.getUpdatedAt());

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

            return ResponseEntity.ok(Map.of(
                    "message", "Supplier products filtered successfully",
                    "data", result
            ));
        } catch (Exception e) {
            logger.error("Failed to filter supplier products: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to filter supplier products: " + e.getMessage()));
        }
    }

    @GetMapping("/filter-by-sapcode")
    @Operation(
            summary = "Filter supplier products by SAP code, Hana SAP code, item descriptions (VN/EN), supplier name, and currency",
            description = "Retrieve a paginated list of supplier products filtered by SAP code, Hana SAP code, item description VN, item description EN, supplier name, and currency. " +
                    "Sorted by: (has lastPurchaseDate desc) -> (lastPurchaseDate desc) -> (price asc)."
    )
    public ResponseEntity<Map<String, Object>> filterBySapCodeHanaAndDescriptions(
            @RequestParam(required = false, defaultValue = "") String sapCode,
            @RequestParam(required = false, defaultValue = "") String hanaSapCode,
            @RequestParam(required = false, defaultValue = "") String itemDescriptionVN,
            @RequestParam(required = false, defaultValue = "") String itemDescriptionEN,
            @RequestParam(required = false, defaultValue = "") String supplierName,   // ✅ NEW
            @RequestParam(required = false, defaultValue = "") String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("price")));

            Page<SupplierProduct> supplierProducts = supplierProductRepositoryCustom.findByFiltersWithPagination(
                    sapCode, hanaSapCode, itemDescriptionVN, itemDescriptionEN, supplierName, currency, pageable // ✅ NEW
            );

            final Map<String, RequisitionMonthly> lastPurchaseCache = new HashMap<>();
            final String currencyFilter = currency != null ? currency.trim() : "";

            List<SupplierProductDTO> dtoList = supplierProducts.getContent().stream().map(product -> {
                SupplierProductDTO dto = new SupplierProductDTO();
                dto.setId(Objects.toString(product.getId(), ""));
                dto.setSupplierCode(Objects.toString(product.getSupplierCode(), ""));
                dto.setSupplierName(Objects.toString(product.getSupplierName(), ""));
                dto.setSapCode(Objects.toString(product.getSapCode(), ""));
                dto.setHanaSapCode(Objects.toString(product.getHanaSapCode(), ""));
                dto.setItemDescriptionEN(Objects.toString(product.getItemDescriptionEN(), ""));
                dto.setItemDescriptionVN(Objects.toString(product.getItemDescriptionVN(), ""));
                dto.setMaterialGroupFullDescription(Objects.toString(product.getMaterialGroupFullDescription(), ""));
                dto.setCurrency(Objects.toString(product.getCurrency(), ""));
                dto.setGoodType(Objects.toString(product.getGoodType(), ""));
                dto.setSize(Objects.toString(product.getSize(), ""));
                dto.setPrice(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
                dto.setUnit(Objects.toString(product.getUnit(), ""));
                dto.setImageUrls(product.getImageUrls() != null ? product.getImageUrls() : new ArrayList<>());
                dto.setProductType1Id(Objects.toString(product.getProductType1Id(), ""));
                dto.setProductType2Id(Objects.toString(product.getProductType2Id(), ""));

                if (product.getProductType1Id() != null && !product.getProductType1Id().isEmpty()) {
                    productType1Repository.findById(product.getProductType1Id())
                            .ifPresent(type1 -> dto.setProductType1Name(Objects.toString(type1.getName(), "")));
                } else dto.setProductType1Name("");

                if (product.getProductType2Id() != null && !product.getProductType2Id().isEmpty()) {
                    productType2Repository.findById(product.getProductType2Id())
                            .ifPresent(type2 -> dto.setProductType2Name(Objects.toString(type2.getName(), "")));
                } else dto.setProductType2Name("");

                // ✅ add last purchase
                applyLastPurchaseForSupplierProduct(dto, product, currencyFilter, lastPurchaseCache);

                return dto;
            }).collect(Collectors.toList());

            Comparator<SupplierProductDTO> supplierSort = Comparator
                    .comparing((SupplierProductDTO d) -> d.getLastPurchaseDate() == null)
                    .thenComparing(SupplierProductDTO::getLastPurchaseDate,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(SupplierProductDTO::getPrice,
                            Comparator.nullsLast(BigDecimal::compareTo));

            dtoList.sort(supplierSort);

            Page<SupplierProductDTO> sortedPage =
                    new PageImpl<>(dtoList, pageable, supplierProducts.getTotalElements());

            return ResponseEntity.ok(Map.of(
                    "message", "Supplier products filtered successfully",
                    "data", sortedPage
            ));
        } catch (Exception e) {
            logger.error("Failed to filter supplier products: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to filter supplier products: " + e.getMessage()));
        }
    }

    private void applyLastPurchaseForSupplierProduct(
            SupplierProductDTO dto,
            SupplierProduct product,
            String currencyFilter,
            Map<String, RequisitionMonthly> cache
    ) {
        if (dto == null || product == null) return;

        final String supplierId = (product.getId() != null) ? product.getId().trim() : "";
        if (supplierId.isBlank()) return;

        String curr = (currencyFilter != null && !currencyFilter.isBlank())
                ? currencyFilter.trim()
                : (product.getCurrency() != null ? product.getCurrency().trim() : "");

        if (curr.isBlank()) return;

        String mode;
        String codeKey;
        if (product.getSapCode() != null && !product.getSapCode().trim().isBlank()) {
            mode = "OLD";
            codeKey = product.getSapCode().trim();
        } else if (product.getHanaSapCode() != null && !product.getHanaSapCode().trim().isBlank()) {
            mode = "HANA";
            codeKey = product.getHanaSapCode().trim();
        } else {
            return;
        }

        String cacheKey = supplierId + "|" + mode + "|" + codeKey + "|" + curr;

        RequisitionMonthly last = cache.get(cacheKey);
        if (!cache.containsKey(cacheKey)) {
            Optional<RequisitionMonthly> opt;

            if ("OLD".equals(mode)) {
                opt = requisitionMonthlyRepository
                        .findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueOrderByCompletedDateDesc(
                                supplierId, codeKey, curr
                        );
                if (opt.isEmpty()) {
                    opt = requisitionMonthlyRepository
                            .findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueOrderByUpdatedDateDesc(
                                    supplierId, codeKey, curr
                            );
                }
            } else {
                opt = requisitionMonthlyRepository
                        .findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueOrderByCompletedDateDesc(
                                supplierId, codeKey, curr
                        );
                if (opt.isEmpty()) {
                    opt = requisitionMonthlyRepository
                            .findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueOrderByUpdatedDateDesc(
                                    supplierId, codeKey, curr
                            );
                }
            }

            last = opt.orElse(null);
            cache.put(cacheKey, last);
        }

        if (last == null) return;

        dto.setLastPurchaseSupplierName(last.getSupplierName());

        LocalDateTime lastDate = last.getCompletedDate();
        if (lastDate == null) lastDate = last.getUpdatedDate();
        if (lastDate == null) lastDate = last.getCreatedDate();
        dto.setLastPurchaseDate(lastDate);

        dto.setLastPurchasePrice(last.getPrice());
        dto.setLastPurchaseOrderQty(last.getOrderQty() != null ? last.getOrderQty() : BigDecimal.ZERO);
    }


    private boolean isUsableCode(String code) {
        if (code == null) return false;
        String c = code.trim();
        return !c.isEmpty() && !c.equalsIgnoreCase("new");
    }

    @PostMapping(value = "/import-new-format", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import price list - Multiple suppliers horizontally (new format)",
            description = "Validates entire file first. Stops immediately on first duplicate based on rules (hana -> old -> supplier+price+currency)."
    )
    public ResponseEntity<Map<String, Object>> importNewFormatExcel(
            @RequestPart("file") MultipartFile file) {

        List<SupplierProduct> productsToSave = new ArrayList<>();
        Map<String, String> type1Cache = new HashMap<>();
        Map<String, String> type2Cache = new HashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Row 3 (index 2): supplier header row
            Row supplierHeaderRow = sheet.getRow(2);
            if (supplierHeaderRow == null) {
                throw new IllegalArgumentException("Supplier header row (row 3) not found.");
            }

            // NEW FORMAT columns (0-based)
            int UNIT_COL = 6;
            int CURRENCY_COL = 7;
            int SUPPLIER_PRICE_START_COL = 8;
            int GOODTYPE_COL = supplierHeaderRow.getLastCellNum() - 1;

            // Build supplier map: colIndex -> supplierName
            Map<Integer, String> priceColToSupplierName = new HashMap<>();
            for (int col = SUPPLIER_PRICE_START_COL; col < GOODTYPE_COL; col++) {
                String supplierName = formatter.formatCellValue(supplierHeaderRow.getCell(col)).trim();
                if (!supplierName.isEmpty()) {
                    priceColToSupplierName.put(col, supplierName);
                }
            }
            if (priceColToSupplierName.isEmpty()) {
                throw new IllegalArgumentException("No supplier columns found in the file.");
            }

            // Iterate data rows (start row 4 => index 3)
            for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String type1Name = formatter.formatCellValue(row.getCell(0)).trim();
                String type2Name = formatter.formatCellValue(row.getCell(1)).trim();
                String descriptionEn = formatter.formatCellValue(row.getCell(2));
                String descriptionVn = formatter.formatCellValue(row.getCell(3));

                // ✅ mapping đúng yêu cầu
                String oldSapCode = formatter.formatCellValue(row.getCell(4)).trim(); // old
                String hanaCode = formatter.formatCellValue(row.getCell(5)).trim();   // new (hana)

                String unit = formatter.formatCellValue(row.getCell(UNIT_COL)).trim();
                String currencyRaw = formatter.formatCellValue(row.getCell(CURRENCY_COL)).trim();
                String goodTypeRaw = formatter.formatCellValue(row.getCell(GOODTYPE_COL)).trim();

                boolean isEndOfData =
                        type1Name.isEmpty()
                                && type2Name.isEmpty()
                                && descriptionEn.isEmpty()
                                && descriptionVn.isEmpty()
                                && oldSapCode.isEmpty()
                                && hanaCode.isEmpty()
                                && unit.isEmpty();

                if (isEndOfData) {
                    break; // stop reading further rows
                }

                String currency = resolveCurrency(currencyRaw);

                String goodType = resolveGoodType(goodTypeRaw);
                String fullDescription = (descriptionVn == null || descriptionVn.isBlank()) ? descriptionEn : descriptionVn;

                // ✅ keep your existing flags (logic giữ nguyên)
                boolean useHana = isUsableCode(hanaCode);
                boolean useOld = !useHana && isUsableCode(oldSapCode);

                // ✅ ADD: flags để dùng trong duplicate-check 3 nhánh (hana -> old -> supplierName+currency+price)
                boolean hasHana = isUsableCode(hanaCode);
                boolean hasOld = isUsableCode(oldSapCode);

                // Resolve ProductType1
                String type1Id = type1Cache.computeIfAbsent(type1Name, name -> {
                    if (name == null || name.trim().isEmpty()) return null;
                    return productType1Repository.findByName(name.trim())
                            .map(ProductType1::getId)
                            .orElseGet(() ->
                                    productType1Repository.save(new ProductType1(name.trim(), LocalDateTime.now())).getId()
                            );
                });

                // Resolve ProductType2
                String type2Id = null;
                if (type1Id != null && type2Name != null && !type2Name.trim().isEmpty()) {
                    String key = type1Id + "|" + type2Name.trim();
                    type2Id = type2Cache.computeIfAbsent(key, k ->
                            productType2Repository.findByNameAndProductType1Id(type2Name.trim(), type1Id)
                                    .map(ProductType2::getId)
                                    .orElseGet(() -> {
                                        ProductType2 newType2 = new ProductType2();
                                        newType2.setName(type2Name.trim());
                                        newType2.setProductType1Id(type1Id);
                                        newType2.setCreatedDate(LocalDateTime.now());
                                        return productType2Repository.save(newType2).getId();
                                    })
                    );
                }

                // Loop supplier prices
                for (Map.Entry<Integer, String> entry : priceColToSupplierName.entrySet()) {
                    int priceColIdx = entry.getKey();
                    String supplierName = entry.getValue();

                    // ✅ model bắt buộc supplierCode; file chỉ có supplierName => tạm dùng supplierName
                    String supplierCode = supplierName;

                    String priceText = formatter.formatCellValue(row.getCell(priceColIdx));
                    if (priceText == null || priceText.trim().isEmpty()) continue;

                    BigDecimal price = parsePrice(priceText, currency, rowIndex + 1);

                    boolean duplicated;
                    String duplicateBy;
                    boolean codesAreEmptyOrNew = isBlankOrNew(oldSapCode) && isBlankOrNew(hanaCode);
                    // ✅ APPLY requested structure:
                    // if has hana -> check by hana
                    // else if has old -> check by old
                    // else -> check by supplierName + currency + price
                    if (hasHana) {
                        duplicated = repository.existsBySupplierCodeAndHanaSapCodeAndCurrencyAndPrice(
                                supplierCode, hanaCode, currency, price
                        );
                        duplicateBy = "hanaCode=" + hanaCode;
                    } else if (hasOld) {
                        duplicated = repository.existsBySupplierCodeAndSapCodeAndCurrencyAndPrice(
                                supplierCode, oldSapCode, currency, price
                        );
                        duplicateBy = "oldSapCode=" + oldSapCode;
                    } else if (codesAreEmptyOrNew) {
                        // ✅ Only here fallback is valid
                        duplicated = repository.existsFallbackBySupplierNameAndCurrencyAndPriceWhenCodesEmptyOrNew(
                                supplierName, currency, price
                        );
                        duplicateBy = "supplierName+currency+price (codes empty/NEW)";
                    } else {
                        // ✅ both codes are not usable but not NEW/empty (rare) -> treat as not duplicated by fallback
                        duplicated = false;
                        duplicateBy = "no-check (codes not usable but not NEW/empty)";
                    }

                    if (duplicated) {
                        String errorMsg = String.format(
                                "Import cancelled due to duplicate entry at row %d: Supplier \"%s\", %s, Currency \"%s\", Price %s",
                                rowIndex + 1, supplierName, duplicateBy, currency, price
                        );
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "Duplicate entry found",
                                "message", errorMsg,
                                "row", rowIndex + 1,
                                "supplierName", supplierName,
                                "supplierCode", supplierCode,
                                "duplicateKey", duplicateBy,
                                "currency", currency,
                                "price", price
                        ));
                    }

                    SupplierProduct product = new SupplierProduct();
                    product.setSupplierCode(supplierCode);
                    product.setSupplierName(supplierName);

                    // ✅ giữ nguyên như code bạn đưa (không đổi logic khác)
                    product.setSapCode(oldSapCode);
                    product.setHanaSapCode(hanaCode);

                    product.setItemDescriptionEN(descriptionEn);
                    product.setItemDescriptionVN(fullDescription);
                    product.setMaterialGroupFullDescription("");
                    product.setSize("");
                    product.setUnit(unit.isEmpty() ? "PC" : unit);
                    product.setCurrency(currency);
                    product.setGoodType(goodType);
                    product.setPrice(price);

                    product.setProductType1Id(type1Id);
                    product.setProductType2Id(type2Id);

                    product.setImageUrls(Collections.emptyList());
                    product.setCreatedAt(LocalDateTime.now());
                    product.setUpdatedAt(LocalDateTime.now());

                    productsToSave.add(product);
                }
            }

            List<SupplierProduct> saved = repository.saveAll(productsToSave);

            return ResponseEntity.ok(Map.of(
                    "message", "Import successful!",
                    "totalRecordsCreated", saved.size(),
                    "note", "Validation passed. No duplicates. Data saved in one batch."
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error", "message", "Import failed: " + e.getMessage()));
        }
    }

    private boolean isBlankOrNew(String s) {
        return s == null || s.trim().isEmpty() || "NEW".equalsIgnoreCase(s.trim());
    }

    // Helper methods remain the same
    private String resolveCurrency(String currencyRaw) {
        if (currencyRaw == null || currencyRaw.isEmpty()) {
            throw new IllegalArgumentException("Currency is missing or invalid.");
        }

        String currency = currencyRaw.trim().toUpperCase();

        // Only allow VND, USD, EURO
        if (!currency.equals("VND") && !currency.equals("USD") && !currency.equals("EURO")) {
            throw new IllegalArgumentException("Invalid currency: " + currency);
        }

        return currency;
    }

    private String resolveGoodType(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Common";
        String cleaned = raw.trim();
        return Set.of("Common", "Special", "Electronics").contains(cleaned) ? cleaned : "Common";
    }

    private BigDecimal parsePrice(String text, String currency, int row) {
        try {
            // Loại bỏ tất cả ký tự không phải số, dấu chấm hoặc dấu phẩy
            String cleaned = text.trim().replaceAll("[^0-9.,]", "");

            // Kiểm tra tiền tệ VND
            if ("VND".equals(currency)) {
                cleaned = cleaned.replaceAll("[,.]", ""); // Loại bỏ dấu phân cách trong VND
                return new BigDecimal(cleaned); // VND chỉ có số nguyên
            } else {
                // Kiểm tra tiền tệ EURO và USD
                cleaned = cleaned.replace(",", "."); // Đổi dấu phẩy thành dấu chấm nếu có

                // Chuyển đổi giá trị sang BigDecimal mà không làm tròn
                return new BigDecimal(cleaned); // Không làm tròn mệnh giá tiền nào hết
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid price at row " + row + ": " + text);
        }
    }


}