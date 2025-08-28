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
import org.bsl.pricecomparison.repository.ProductType1Repository;
import org.bsl.pricecomparison.repository.ProductType2Repository;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
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

    private static final String UPLOAD_DIR = "uploads/";
    private static final Logger logger = LoggerFactory.getLogger(SupplierProductController.class);

    private boolean checkDuplicate(SupplierProduct product, String excludeId) {
        Double price = product.getPrice() != null ? product.getPrice() : 0.0;
        return repository.existsBySupplierCodeAndSapCodeAndPriceAndIdNot(
                product.getSupplierCode(),
                product.getSapCode(),
                price,
                excludeId
        );
    }

    @GetMapping
    @Operation(
            summary = "Get all supplier products",
            description = "Retrieve a paginated list of all supplier products sorted by creation date in descending order."
    )
    public Page<SupplierProductDTO> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupplierProduct> products = repository.findAll(pageable);

        return products.map(product -> {
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
    }

    @GetMapping("/{id}")
    public Optional<SupplierProduct> getById(@PathVariable String id) {
        return repository.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repository.deleteById(id);
    }

    @GetMapping("/search")
    public List<SupplierProduct> searchByItemNoOrSapCode(
            @RequestParam(required = false) String itemNo,
            @RequestParam(required = false) String sapCode) {

        if (itemNo != null && sapCode != null) {
            return repository.findByItemNoContainingIgnoreCaseAndSapCodeContainingIgnoreCase(itemNo, sapCode);
        } else if (itemNo != null) {
            return repository.findByItemNoContainingIgnoreCase(itemNo);
        } else if (sapCode != null) {
            return repository.findBySapCodeContainingIgnoreCase(sapCode);
        }
        return repository.findAll();
    }

    @GetMapping("/by-requisition")
    public List<SupplierProduct> getByRequisition(
            @RequestParam String sapCode,
            @RequestParam String supplierId) {
        return repository.findBySapCodeAndSupplierCode(sapCode, supplierId);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import from Excel", description = "Upload an Excel .xlsx file with columns in the order: supplierCode, supplierName, sapCode, itemNo, itemDescription, fullDescription, materialGroupFullDescription, currency, size, price, unit")
    public List<SupplierProduct> importExcel(
            @Parameter(description = "Excel .xlsx file containing product data")
            @RequestPart("file") MultipartFile file) throws Exception {

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
                String fullDescription = formatter.formatCellValue(row.getCell(5));
                String materialGroupFullDescription = formatter.formatCellValue(row.getCell(6));
                String currency = formatter.formatCellValue(row.getCell(7));
                String size = formatter.formatCellValue(row.getCell(8));
                String priceText = formatter.formatCellValue(row.getCell(9));
                String unit = formatter.formatCellValue(row.getCell(10));

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
                p.setFullDescription(fullDescription);
                p.setMaterialGroupFullDescription(materialGroupFullDescription);
                p.setCurrency(currency);
                p.setSize(size);
                p.setPrice(price);
                p.setUnit(unit);

                products.add(p);
            }
        }

        return repository.saveAll(products);
    }

    @GetMapping("/search-by-name-or-sapcode")
    public Page<SupplierProduct> search(
            @RequestParam(required = false) String sapCode,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String itemNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (Stream.of(sapCode, supplierCode, itemNo, supplierName)
                .allMatch(StringUtils::isBlank)) {
            return repository.findAll(PageRequest.of(page, size));
        }

        PageRequest pageRequest = PageRequest.of(page, size);

        return repository.findBySapCodeContainingIgnoreCaseOrSupplierCodeContainingIgnoreCaseOrItemNoContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
                sapCode, supplierCode, itemNo, supplierName, pageRequest);
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
    public ResponseEntity<SupplierProduct> createProductWithFile(
            @ModelAttribute CreateProductRequest request
    ) {
        try {
            if (repository.existsBySupplierCodeAndSapCodeAndPrice(
                    request.getSupplierCode(), request.getSapCode(), request.getPrice())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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

            if (request.getProductType1Id() != null) {
                product.setProductType1Id(request.getProductType1Id());
            }
            if (request.getProductType2Id() != null) {
                product.setProductType2Id(request.getProductType2Id());
            }

            SupplierProduct savedProduct = repository.save(product);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

//    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(
//            summary = "Update a product with optional multiple image uploads",
//            description = "Update product fields and optionally upload multiple images using multipart/form-data.",
//            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    content = @Content(
//                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
//                            schema = @Schema(implementation = UpdateProductRequest.class)
//                    )
//            )
//    )
//    public ResponseEntity<SupplierProduct> updateProduct(
//            @PathVariable String id,
//            @ModelAttribute UpdateProductRequest request
//    ) {
//        try {
//            SupplierProduct existingProduct = repository.findById(id).orElse(null);
//            if (existingProduct == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//            }
//
//            if (checkDuplicate(existingProduct, id)) {
//                return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
//            }
//
//            List<String> imageUrls = new ArrayList<>(existingProduct.getImageUrls() != null ? existingProduct.getImageUrls() : new ArrayList<>());
//            List<String> files = request.getImageUrls();
//            if (files != null && !files.isEmpty()) {
//                for (String file : files) {
//                    if (file != null && !file.isEmpty()) {
//                        String imageUrl = saveImage(file);
//                        imageUrls.add(imageUrl);
//                    }
//                }
//            }
//
//            // Cập nhật các trường với xử lý null cho price
//            existingProduct.setSupplierCode(request.getSupplierCode() != null ? request.getSupplierCode() : existingProduct.getSupplierCode());
//            existingProduct.setSupplierName(request.getSupplierName() != null ? request.getSupplierName() : existingProduct.getSupplierName());
//            existingProduct.setSapCode(request.getSapCode() != null ? request.getSapCode() : existingProduct.getSapCode());
//            existingProduct.setItemNo(request.getItemNo() != null ? request.getItemNo() : existingProduct.getItemNo());
//            existingProduct.setItemDescription(request.getItemDescription() != null ? request.getItemDescription() : existingProduct.getItemDescription());
//            existingProduct.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : existingProduct.getFullDescription());
//            existingProduct.setMaterialGroupFullDescription(request.getMaterialGroupFullDescription() != null ? request.getMaterialGroupFullDescription() : existingProduct.getMaterialGroupFullDescription());
//            existingProduct.setCurrency(request.getCurrency() != null ? request.getCurrency() : existingProduct.getCurrency());
//            existingProduct.setSize(request.getSize() != null ? request.getSize() : existingProduct.getSize());
//            existingProduct.setPrice(request.getPrice() != null ? request.getPrice() : (existingProduct.getPrice() != null ? existingProduct.getPrice() : 0.0));
//            existingProduct.setUnit(request.getUnit() != null ? request.getUnit() : existingProduct.getUnit());
//            existingProduct.setImageUrls(imageUrls);
//            existingProduct.setCreatedAt(LocalDateTime.now());
//
//            if (request.getProductType1Id() != null) {
//                existingProduct.setProductType1Id(request.getProductType1Id());
//            }
//            if (request.getProductType2Id() != null) {
//                existingProduct.setProductType2Id(request.getProductType2Id());
//            }
//
//            SupplierProduct updatedProduct = repository.save(existingProduct);
//            return ResponseEntity.ok(updatedProduct);
//
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

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
    public Page<SupplierProductDTO> filterSupplierProducts(
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
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createdAt"))
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

        return supplierProducts.map(product -> {
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
    }

}