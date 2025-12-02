package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public class CompletedSupplierDTO {

    @Schema(description = "Supplier ID", example = "68d9e2d01892db0acac3571f", required = true)
    private String supplierId;

    @Schema(description = "Supplier name", example = "CÔNG TY TNHH CƠ KHÍ KHUÔN MẪU KHANG HUY", required = true)
    private String supplierName;

    @Schema(description = "Price of the supplier", example = "2000000")
    private BigDecimal price;

    @Schema(description = "Unit of the supplier", example = "PC")
    private String unit;

    @Schema(description = "Flag indicating whether this supplier is selected", example = "1", allowableValues = {"0", "1"})
    private Integer isSelected;

    @Schema(description = "Flag indicating whether this supplier has the best price", example = "true")
    private Boolean isBestPrice;

    // Constructors
    public CompletedSupplierDTO() {}

    public CompletedSupplierDTO(String supplierId, String supplierName, BigDecimal price,
                                String unit, Integer isSelected, Boolean isBestPrice) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.price = price;
        this.unit = unit;
        this.isSelected = isSelected;
        this.isBestPrice = isBestPrice;
    }

    // Getters và Setters
    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
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

    public Integer getIsSelected() {
        return isSelected;
    }

    public void setIsSelected(Integer isSelected) {
        this.isSelected = isSelected;
    }

    public Boolean getIsBestPrice() {
        return isBestPrice;
    }

    public void setIsBestPrice(Boolean isBestPrice) {
        this.isBestPrice = isBestPrice;
    }
}