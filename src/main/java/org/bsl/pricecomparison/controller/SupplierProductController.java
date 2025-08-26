package org.bsl.pricecomparison.controller;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bsl.pricecomparison.dto.SupplierProductDTO;
import org.bsl.pricecomparison.exception.DuplicateSupplierProductException;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.repository.ProductType1Repository;
import org.bsl.pricecomparison.repository.ProductType2Repository;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.bsl.pricecomparison.request.CreateProductRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bsl.pricecomparison.exception.DuplicateSupplierProductException;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/supplier-products")
public class SupplierProductController {

    @Autowired
    private SupplierProductRepository repository;

    @Autowired
    private ProductType1Repository productType1Repository;

    @Autowired
    private ProductType2Repository productType2Repository;

    private static final String UPLOAD_DIR = "uploads/";

    private boolean checkDuplicate(SupplierProduct product, String excludeId) {
        return repository.existsBySupplierCodeAndSapCodeAndPriceAndIdNot(
                product.getSupplierCode(), product.getSapCode(), product.getPrice(), excludeId);
    }


//    @GetMapping
//    @Operation(
//            summary = "Get all supplier products",
//            description = "Retrieve a list of all supplier products sorted by creation date in descending order."
//    )
//    public List<SupplierProductDTO> getAll() {
//        List<SupplierProduct> products = repository.findAllByOrderByCreatedAtDesc();
//
//        return products.stream().map(product -> {
//            SupplierProductDTO dto = new SupplierProductDTO();
//            BeanUtils.copyProperties(product, dto);
//
//            if (product.getImageUrls() == null) {
//                dto.setImageUrls(new ArrayList<>());
//            }
//
//            // Ánh xạ productType1Name
//            if (product.getProductType1Id() != null) {
//                productType1Repository.findById(product.getProductType1Id()).ifPresent(type1 -> {
//                    dto.setProductType1Name(type1.getName());
//                });
//            }
//
//            if (product.getProductType2Id() != null) {
//                productType2Repository.findById(product.getProductType2Id()).ifPresent(type2 -> {
//                    dto.setProductType2Name(type2.getName());
//                });
//            }
//
//            return dto;
//        }).collect(Collectors.toList());
//    }

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

//    @PostMapping
//    public SupplierProduct create(@RequestBody SupplierProduct product) {
//        boolean exists = repository.existsBySupplierCodeAndSapCodeAndPrice(product.getSupplierCode(), product.getSapCode(), product.getPrice());
//
//        if (exists) {
//            throw new DuplicateSupplierProductException(String.format(
//                    "Duplicate entry: supplierCode='%s', sapCode='%s', price=%.2f already exists",
//                    product.getSupplierCode(), product.getSapCode(), product.getPrice()));
//        }
//        return repository.save(product);
//    }


//    @PutMapping("/{id}")
//    public ResponseEntity<SupplierProduct> updateProduct(
//            @PathVariable String id,
//            @RequestBody SupplierProduct product) {
//
//        Optional<SupplierProduct> existingProductOpt = repository.findById(id);
//        if (existingProductOpt.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//
//        boolean exists = repository.existsBySupplierCodeAndSapCodeAndPriceAndIdNot(
//                product.getSupplierCode(), product.getSapCode(), product.getPrice(), id);
//
//        if (exists) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//
//        SupplierProduct existingProduct = existingProductOpt.get();
//        existingProduct.setSupplierName(product.getSupplierName());
//        existingProduct.setProductFullName(product.getProductFullName());
//        existingProduct.setProductShortName(product.getProductShortName());
//        existingProduct.setSize(product.getSize());
//        existingProduct.setPrice(product.getPrice());
//        existingProduct.setUnit(product.getUnit());
//
//        SupplierProduct updatedProduct = repository.save(existingProduct);
//        return ResponseEntity.ok(updatedProduct);
//    }


    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        repository.deleteById(id);
    }

    @GetMapping("/search")
    public List<SupplierProduct> searchByProductFullNameOrSapCode(
            @RequestParam(required = false) String productFullName,
            @RequestParam(required = false) String sapCode) {

        if (productFullName != null && sapCode != null) {
            return repository.findByProductFullNameContainingIgnoreCaseAndSapCodeContainingIgnoreCase(productFullName, sapCode);
        } else if (productFullName != null) {
            return repository.findByProductFullNameContainingIgnoreCase(productFullName);
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
    @Operation(summary = "Import from Excel", description = "Upload an Excel .xlsx file with columns in the order: supplierCode, supplierName, sapCode, productFullName, productShortName, size, price, unit")
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
                String productFullName = formatter.formatCellValue(row.getCell(3));
                String productShortName = formatter.formatCellValue(row.getCell(4));
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
                p.setProductFullName(productFullName);
                p.setProductShortName(productShortName);
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
            @RequestParam(required = false) String productFullName,
            @RequestParam(required = false) String supplierName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (Stream.of(sapCode, supplierCode, productFullName, supplierName)
                .allMatch(StringUtils::isBlank)) {
            return repository.findAll(PageRequest.of(page, size));
        }

        PageRequest pageRequest = PageRequest.of(page, size);

        return repository.findBySapCodeContainingIgnoreCaseOrSupplierCodeContainingIgnoreCaseOrProductFullNameContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
                sapCode, supplierCode, productFullName, supplierName, pageRequest);
    }

//    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(
//            summary = "Create a new product with an image upload",
//            description = "Create a product and upload an image using multipart/form-data.",
//            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    content = @Content(
//                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
//                            schema = @Schema(implementation = CreateProductRequest.class)
//                    )
//            )
//    )
//    public ResponseEntity<SupplierProduct> createProductWithFile(
//            @ModelAttribute CreateProductRequest request
//    ) {
//        try {
//            MultipartFile file = request.getFile();
//
//            if (repository.existsBySupplierCodeAndSapCodeAndPrice(request.getSupplierCode(), request.getSapCode(), request.getPrice())) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
//            }
//
//            String imageUrl = saveImage(file);
//
//            SupplierProduct product = new SupplierProduct();
//            product.setSupplierCode(request.getSupplierCode());
//            product.setSupplierName(request.getSupplierName());
//            product.setSapCode(request.getSapCode());
//            product.setProductFullName(request.getProductFullName());
//            product.setProductShortName(request.getProductShortName());
//            product.setSize(request.getSize());
//            product.setPrice(request.getPrice());
//            product.setUnit(request.getUnit());
//            product.setImageUrl(imageUrl);
//            product.setCreatedAt(LocalDateTime.now());
//
//            if (request.getProductType1Id() != null) {
//                product.setProductType1Id(request.getProductType1Id());
//            }
//            if (request.getProductType2Id() != null) {
//                product.setProductType2Id(request.getProductType2Id());
//            }
//
//            SupplierProduct savedProduct = repository.save(product);
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
//
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }

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

            // Tạo đối tượng SupplierProduct
            SupplierProduct product = new SupplierProduct();
            product.setSupplierCode(request.getSupplierCode());
            product.setSupplierName(request.getSupplierName());
            product.setSapCode(request.getSapCode());
            product.setProductFullName(request.getProductFullName());
            product.setProductShortName(request.getProductShortName());
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
//            summary = "Update product with optional image upload",
//            description = "Update product fields and optionally update image file.",
//            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    content = @Content(
//                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE
//                    )
//            )
//    )
//    public ResponseEntity<SupplierProduct> updateProduct(
//            @PathVariable String id,
//            @RequestParam(value = "file", required = false) MultipartFile file,
//            @RequestParam(value = "supplierCode", required = false) String supplierCode,
//            @RequestParam(value = "supplierName", required = false) String supplierName,
//            @RequestParam(value = "sapCode", required = false) String sapCode,
//            @RequestParam(value = "productFullName", required = false) String productFullName,
//            @RequestParam(value = "productShortName", required = false) String productShortName,
//            @RequestParam(value = "size", required = false) String size,
//            @RequestParam(value = "price", required = false) Double price,
//            @RequestParam(value = "unit", required = false) String unit,
//            @RequestParam(value = "productType1Id", required = false) String productType1Id,
//            @RequestParam(value = "productType2Id", required = false) String productType2Id
//
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
//            if (file != null && !file.isEmpty()) {
//                String imageUrl = saveImage(file);
//                existingProduct.setImageUrl(imageUrl);
//            }
//
//            existingProduct.setSupplierCode(supplierCode);
//            existingProduct.setSupplierName(supplierName);
//            existingProduct.setSapCode(sapCode);
//            existingProduct.setProductFullName(productFullName);
//            existingProduct.setProductShortName(productShortName);
//            existingProduct.setSize(size);
//            existingProduct.setPrice(price);
//            existingProduct.setUnit(unit);
//
//            // Chỉ cập nhật nếu có dữ liệu mới gửi lên
//            if (productType1Id != null && !productType1Id.isEmpty()) {
//                existingProduct.setProductType1Id(productType1Id);
//            }
//            if (productType2Id != null && !productType2Id.isEmpty()) {
//                existingProduct.setProductType2Id(productType2Id);
//            }
//
//            SupplierProduct updatedProduct = repository.save(existingProduct);
//            return ResponseEntity.ok(updatedProduct);
//
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update a product with optional multiple image uploads",
            description = "Update product fields and optionally upload multiple images using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CreateProductRequest.class)
                    )
            )
    )
    public ResponseEntity<SupplierProduct> updateProduct(
            @PathVariable String id,
            @ModelAttribute CreateProductRequest request
    ) {
        try {
            SupplierProduct existingProduct = repository.findById(id).orElse(null);
            if (existingProduct == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            if (checkDuplicate(existingProduct, id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
            }

            List<String> imageUrls = new ArrayList<>(existingProduct.getImageUrls() != null ? existingProduct.getImageUrls() : new ArrayList<>());
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        imageUrls.add(imageUrl);
                    }
                }
            }

            existingProduct.setSupplierCode(request.getSupplierCode() != null ? request.getSupplierCode() : existingProduct.getSupplierCode());
            existingProduct.setSupplierName(request.getSupplierName() != null ? request.getSupplierName() : existingProduct.getSupplierName());
            existingProduct.setSapCode(request.getSapCode() != null ? request.getSapCode() : existingProduct.getSapCode());
            existingProduct.setProductFullName(request.getProductFullName() != null ? request.getProductFullName() : existingProduct.getProductFullName());
            existingProduct.setProductShortName(request.getProductShortName() != null ? request.getProductShortName() : existingProduct.getProductShortName());
            existingProduct.setSize(request.getSize() != null ? request.getSize() : existingProduct.getSize());
            existingProduct.setPrice(request.getPrice() != null ? request.getPrice() : existingProduct.getPrice());
            existingProduct.setUnit(request.getUnit() != null ? request.getUnit() : existingProduct.getUnit());
            existingProduct.setImageUrls(imageUrls);
            existingProduct.setCreatedAt(LocalDateTime.now());

            if (request.getProductType1Id() != null) {
                existingProduct.setProductType1Id(request.getProductType1Id());
            }
            if (request.getProductType2Id() != null) {
                existingProduct.setProductType2Id(request.getProductType2Id());
            }

            SupplierProduct updatedProduct = repository.save(existingProduct);
            return ResponseEntity.ok(updatedProduct);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
//
//
//
//
//    // Phương thức để lưu hình ảnh
//    private String saveImage(MultipartFile file) throws IOException {
//        if (file.isEmpty()) {
//            throw new IOException("File is empty");
//        }
//
//        // Lưu tệp vào thư mục uploads
//        Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
//        Files.createDirectories(path.getParent());
//        file.transferTo(path);
//
//        return path.toString(); // Trả về URL hình ảnh
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

}
