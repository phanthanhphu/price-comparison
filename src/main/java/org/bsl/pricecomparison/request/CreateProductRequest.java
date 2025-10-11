package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public class CreateProductRequest {

    @ArraySchema(
            arraySchema = @Schema(description = "Product image files", type = "array"),
            minItems = 0,
            maxItems = 10,
            uniqueItems = false,
            schema = @Schema(type = "string", format = "binary")
    )
    private List<MultipartFile> files;

    @Schema(description = "Supplier code", example = "SUP123")
    @NotBlank(message = "Supplier code is required")
    @Size(max = 50, message = "Supplier code must not exceed 50 characters")
    private String supplierCode;

    @Schema(description = "Supplier name", example = "Công ty ABC")
    @NotBlank(message = "Supplier name is required")
    @Size(max = 100, message = "Supplier name must not exceed 100 characters")
    private String supplierName;

    @Schema(description = "SAP code", example = "SAP456")
    @NotBlank(message = "SAP code is required")
    @Size(max = 50, message = "SAP code must not exceed 50 characters")
    private String sapCode;

    @Schema(description = "Item number", example = "ITEM123")
    @Size(max = 50, message = "Item number must not exceed 50 characters")
    private String itemNo;

    @Schema(description = "Item description", example = "XYZ")
    @NotBlank(message = "Item description is required")
    @Size(max = 200, message = "Item description must not exceed 200 characters")
    private String itemDescription;

    @Schema(description = "Full description", example = "Sản phẩm XYZ chi tiết")
    @Size(max = 1000, message = "Full description must not exceed 1000 characters")
    private String fullDescription;

    @Schema(description = "Currency", example = "VND")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(VND|USD|EURO)$", message = "Currency must be VND, USD, or EURO")
    private String currency;

    @Schema(description = "Good type", example = "Electronics")
    @NotBlank(message = "Good type is required")
    @Size(max = 100, message = "Good type must not exceed 100 characters")
    private String goodType;

    @Schema(description = "Size", example = "500ml")
    @Size(max = 50, message = "Size must not exceed 50 characters")
    private String size;

    @Schema(description = "Price", example = "12000")
    @NotNull(message = "Price is required")
    private BigDecimal price;

    @Schema(description = "Unit", example = "chai")
    @NotBlank(message = "Unit is required")
    @Size(max = 20, message = "Unit must not exceed 20 characters")
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

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGoodType() {
        return goodType;
    }

    public void setGoodType(String goodType) {
        this.goodType = goodType;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
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