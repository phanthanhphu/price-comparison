package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "supplier_products")
public class SupplierProduct {

    @Id
    private String id;

    private String supplierCode;
    private String supplierName;
    private String sapCode;
    private String itemNo; // Renamed from productFullName
    private String itemDescription; // Renamed from productShortName
    private String fullDescription; // Added
    private String materialGroupFullDescription; // Added
    private String currency; // Added
    private String size;
    private Double price;
    private String unit;
    private List<String> imageUrls;
    private String productType1Id;
    private String productType2Id;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupplierProduct() {
    }

    public SupplierProduct(
            String id,
            String supplierCode,
            String supplierName,
            String sapCode,
            String itemNo,
            String itemDescription,
            String fullDescription,
            String materialGroupFullDescription,
            String currency,
            String size,
            Double price,
            String unit,
            List<String> imageUrls,
            String productType1Id,
            String productType2Id
    ) {
        this.id = id;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.sapCode = sapCode;
        this.itemNo = itemNo;
        this.itemDescription = itemDescription;
        this.fullDescription = fullDescription;
        this.materialGroupFullDescription = materialGroupFullDescription;
        this.currency = currency;
        this.size = size;
        this.price = price;
        this.unit = unit;
        this.imageUrls = imageUrls;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
    }

    // Getters and Setters
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