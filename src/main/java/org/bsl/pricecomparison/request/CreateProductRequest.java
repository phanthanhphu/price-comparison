package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class CreateProductRequest {

    @ArraySchema(
            arraySchema = @Schema(description = "Product image files", type = "array"),
            minItems = 0,
            maxItems = 10, // Optional: Set a reasonable limit
            uniqueItems = false,
            schema = @Schema(type = "string", format = "binary")
    )
    private List<MultipartFile> files;

    @Schema(description = "Supplier code", example = "SUP123")
    private String supplierCode;

    @Schema(description = "Supplier name", example = "Công ty ABC")
    private String supplierName;

    @Schema(description = "SAP code", example = "SAP456")
    private String sapCode;

    @Schema(description = "Product full name", example = "Sản phẩm XYZ")
    private String productFullName;

    @Schema(description = "Product short name", example = "XYZ")
    private String productShortName;

    @Schema(description = "Size", example = "500ml")
    private String size;

    @Schema(description = "Price", example = "12000")
    private Double price;

    @Schema(description = "Unit", example = "chai")
    private String unit;

    @Schema(description = "Product Type 1 ID (optional)", example = "1")
    private String productType1Id;

    @Schema(description = "Product Type 2 ID (optional)", example = "2")
    private String productType2Id;

    // === Getters and Setters ===

    public List<MultipartFile> getFiles() {
        return files;
    }

    public void setFiles(List<MultipartFile> files) {
        this.files = files;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSapCode() {
        return sapCode;
    }

    public void setSapCode(String sapCode) {
        this.sapCode = sapCode;
    }

    public String getProductFullName() {
        return productFullName;
    }

    public void setProductFullName(String productFullName) {
        this.productFullName = productFullName;
    }

    public String getProductShortName() {
        return productShortName;
    }

    public void setProductShortName(String productShortName) {
        this.productShortName = productShortName;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getProductType1Id() {
        return productType1Id;
    }

    public void setProductType1Id(String productType1Id) {
        this.productType1Id = productType1Id;
    }

    public String getProductType2Id() {
        return productType2Id;
    }

    public void setProductType2Id(String productType2Id) {
        this.productType2Id = productType2Id;
    }
}