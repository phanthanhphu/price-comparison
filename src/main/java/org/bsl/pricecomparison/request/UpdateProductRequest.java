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

public class UpdateProductRequest {

    @ArraySchema(
            arraySchema = @Schema(description = "Product image files to upload", type = "array"),
            minItems = 0,
            maxItems = 10,
            uniqueItems = false,
            schema = @Schema(type = "string", format = "binary")
    )
    private List<MultipartFile> files;

    @ArraySchema(
            arraySchema = @Schema(description = "List of existing image URLs to delete", type = "array"),
            minItems = 0,
            uniqueItems = true,
            schema = @Schema(type = "string", example = "/uploads/image.jpg")
    )
    private List<String> imagesToDelete;

    @Schema(description = "Supplier code", example = "SUP123")
    @Size(max = 50, message = "Supplier code must not exceed 50 characters")
    private String supplierCode;

    @Schema(description = "Supplier name", example = "Công ty ABC")
    @Size(max = 100, message = "Supplier name must not exceed 100 characters")
    private String supplierName;

    @Schema(description = "SAP code", example = "SAP456")
    @Size(max = 50, message = "SAP code must not exceed 50 characters")
    private String sapCode;

    // ĐÃ ĐỔI: itemNo → hanaSapCode
    @Schema(description = "HANA SAP code (Item number from HANA)", example = "ITEM123")
    @Size(max = 50, message = "HANA SAP code must not exceed 50 characters")
    private String hanaSapCode;

    // ĐÃ ĐỔI: itemDescription → itemDescriptionEN
    @Schema(description = "Item description (English)", example = "Premium Mineral Water 500ml")
    @Size(max = 200, message = "Item description (EN) must not exceed 200 characters")
    private String itemDescriptionEN;

    // ĐÃ ĐỔI: fullDescription → itemDescriptionVN
    @Schema(description = "Item description (Vietnamese)", example = "Nước khoáng cao cấp 500ml - Không ga")
    @Size(max = 1000, message = "Item description (VN) must not exceed 1000 characters")
    private String itemDescriptionVN;

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

    public List<String> getImagesToDelete() {
        return imagesToDelete;
    }

    public void setImagesToDelete(List<String> imagesToDelete) {
        this.imagesToDelete = imagesToDelete;
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

    public String getHanaSapCode() {
        return hanaSapCode;
    }

    public void setHanaSapCode(String hanaSapCode) {
        this.hanaSapCode = hanaSapCode;
    }

    public String getItemDescriptionEN() {
        return itemDescriptionEN;
    }

    public void setItemDescriptionEN(String itemDescriptionEN) {
        this.itemDescriptionEN = itemDescriptionEN;
    }

    public String getItemDescriptionVN() {
        return itemDescriptionVN;
    }

    public void setItemDescriptionVN(String itemDescriptionVN) {
        this.itemDescriptionVN = itemDescriptionVN;
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