package org.bsl.pricecomparison.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "supplier_products")
public class SupplierProduct {

    @Id
    private String id;

    @NotBlank(message = "Supplier code is required")
    private String supplierCode;

    @NotBlank(message = "Supplier name is required")
    private String supplierName;

    @NotBlank(message = "SAP code is required")
    private String sapCode;

    // Đổi tên từ itemNo → hanaSapCode
    private String hanaSapCode;

    // Đổi tên từ itemDescription → itemDescriptionEN
    @NotBlank(message = "Item description (EN) is required")
    private String itemDescriptionEN;

    // Đổi tên từ fullDescription → itemDescriptionVN
    private String itemDescriptionVN;

    private String materialGroupFullDescription;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Good type is required")
    private String goodType;

    private String size;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    @NotBlank(message = "Unit is required")
    private String unit;

    private List<String> imageUrls;

    @Indexed
    private String productType1Id;

    @Indexed
    private String productType2Id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public SupplierProduct() {
    }

    // Constructor đã được cập nhật tên các trường
    public SupplierProduct(
            String id,
            String supplierCode,
            String supplierName,
            String sapCode,
            String hanaSapCode,
            String itemDescriptionEN,
            String itemDescriptionVN,
            String materialGroupFullDescription,
            String currency,
            String goodType,
            String size,
            BigDecimal price,
            String unit,
            List<String> imageUrls,
            String productType1Id,
            String productType2Id
    ) {
        this.id = id;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.sapCode = sapCode;
        this.hanaSapCode = hanaSapCode;
        this.itemDescriptionEN = itemDescriptionEN;
        this.itemDescriptionVN = itemDescriptionVN;
        this.materialGroupFullDescription = materialGroupFullDescription;
        this.currency = currency;
        this.goodType = goodType;
        this.size = size;
        this.price = price;
        this.unit = unit;
        this.imageUrls = imageUrls;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
    }

    // Getters and Setters (đã đổi tên)

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMaterialGroupFullDescription() {
        return materialGroupFullDescription;
    }

    public void setMaterialGroupFullDescription(String materialGroupFullDescription) {
        this.materialGroupFullDescription = materialGroupFullDescription;
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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}