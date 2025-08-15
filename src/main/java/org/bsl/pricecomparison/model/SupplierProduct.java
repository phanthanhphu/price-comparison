package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "supplier_products")
public class SupplierProduct {

    @Id
    private String id;

    private String supplierCode;
    private String supplierName;
    private String sapCode;
    private String productFullName;
    private String productShortName;
    private String size;
    private Double price;
    private String unit;

    public SupplierProduct() {
    }

    public SupplierProduct(String id, String supplierCode, String supplierName, String sapCode,
                           String productFullName, String productShortName, String size,
                           Double price, String unit) {
        this.id = id;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.sapCode = sapCode;
        this.productFullName = productFullName;
        this.productShortName = productShortName;
        this.size = size;
        this.price = price;
        this.unit = unit;
    }

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
}
