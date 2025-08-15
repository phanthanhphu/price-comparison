package org.bsl.pricecomparison.controller;

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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/supplier-products")
public class SupplierProductController {

    @Autowired
    private SupplierProductRepository repository;

    @GetMapping
    public List<SupplierProduct> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<SupplierProduct> getById(@PathVariable String id) {
        return repository.findById(id);
    }

    @PostMapping
    public SupplierProduct create(@RequestBody SupplierProduct product) {
        boolean exists = repository.existsBySupplierCodeAndSapCodeAndPrice(product.getSupplierCode(), product.getSapCode(), product.getPrice());

        if (exists) {
            throw new DuplicateSupplierProductException(String.format(
                    "Duplicate entry: supplierCode='%s', sapCode='%s', price=%.2f already exists",
                    product.getSupplierCode(), product.getSapCode(), product.getPrice()));
        }
        return repository.save(product);
    }


    @PutMapping("/{id}")
    public ResponseEntity<SupplierProduct> updateProduct(
            @PathVariable String id,
            @RequestBody SupplierProduct product) {

        Optional<SupplierProduct> existingProductOpt = repository.findById(id);
        if (existingProductOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        boolean exists = repository.existsBySupplierCodeAndSapCodeAndPriceAndIdNot(
                product.getSupplierCode(), product.getSapCode(), product.getPrice(), id);

        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        SupplierProduct existingProduct = existingProductOpt.get();
        existingProduct.setSupplierName(product.getSupplierName());
        existingProduct.setProductFullName(product.getProductFullName());
        existingProduct.setProductShortName(product.getProductShortName());
        existingProduct.setSize(product.getSize());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setUnit(product.getUnit());

        SupplierProduct updatedProduct = repository.save(existingProduct);
        return ResponseEntity.ok(updatedProduct);
    }


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





}
