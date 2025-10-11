package org.bsl.pricecomparison.dto;

import org.bsl.pricecomparison.model.DepartmentRequisitionMonthly;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RequisitionMonthlyDTO {
    private String id;
    private String groupId;
    private String productType1Name;
    private String productType2Name;
    private String itemDescriptionEN;
    private String itemDescriptionVN;
    private String oldSAPCode;
    private String hanaSAPCode;
    private String unit;
    private List<DepartmentRequisitionMonthly> departmentRequisitions;

    @NotNull(message = "Daily medical inventory is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily medical inventory must be non-negative")
    private BigDecimal dailyMedInventory;

    @NotNull(message = "Safe stock is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Safe stock must be non-negative")
    private BigDecimal safeStock;

    @NotNull(message = "Total requested quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total requested quantity must be non-negative")
    private BigDecimal totalRequestQty;

    @NotNull(message = "Use stock quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Use stock quantity must be non-negative")
    private BigDecimal useStockQty;

    @NotNull(message = "Order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Order quantity must be non-negative")
    private BigDecimal orderQty;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be non-negative")
    private BigDecimal amount;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    private String currency;
    private String goodType;
    private String supplierName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String fullDescription;
    private String reason;
    private String remark;
    private String remarkComparison;
    private List<String> imageUrls;

    public RequisitionMonthlyDTO(
            String id,
            String groupId,
            String productType1Name,
            String productType2Name,
            String itemDescriptionEN,
            String itemDescriptionVN,
            String oldSAPCode,
            String hanaSAPCode,
            String unit,
            List<DepartmentRequisitionMonthly> departmentRequisitions,
            BigDecimal dailyMedInventory,
            BigDecimal safeStock,
            BigDecimal totalRequestQty,
            BigDecimal useStockQty,
            BigDecimal orderQty,
            BigDecimal amount,
            BigDecimal price,
            String currency,
            String goodType,
            String supplierName,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            String fullDescription,
            String reason,
            String remark,
            String remarkComparison,
            List<String> imageUrls
    ) {
        this.id = id;
        this.groupId = groupId;
        this.productType1Name = productType1Name;
        this.productType2Name = productType2Name;
        this.itemDescriptionEN = itemDescriptionEN;
        this.itemDescriptionVN = itemDescriptionVN;
        this.oldSAPCode = oldSAPCode;
        this.hanaSAPCode = hanaSAPCode;
        this.unit = unit;
        this.departmentRequisitions = departmentRequisitions;
        this.dailyMedInventory = dailyMedInventory;
        this.safeStock = safeStock;
        this.totalRequestQty = totalRequestQty;
        this.useStockQty = useStockQty;
        this.orderQty = orderQty;
        this.amount = amount;
        this.price = price;
        this.currency = currency;
        this.goodType = goodType;
        this.supplierName = supplierName;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.fullDescription = fullDescription;
        this.reason = reason;
        this.remark = remark;
        this.remarkComparison = remarkComparison;
        this.imageUrls = imageUrls;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getProductType1Name() {
        return productType1Name;
    }

    public void setProductType1Name(String productType1Name) {
        this.productType1Name = productType1Name;
    }

    public String getProductType2Name() {
        return productType2Name;
    }

    public void setProductType2Name(String productType2Name) {
        this.productType2Name = productType2Name;
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

    public String getOldSAPCode() {
        return oldSAPCode;
    }

    public void setOldSAPCode(String oldSAPCode) {
        this.oldSAPCode = oldSAPCode;
    }

    public String getHanaSAPCode() {
        return hanaSAPCode;
    }

    public void setHanaSAPCode(String hanaSAPCode) {
        this.hanaSAPCode = hanaSAPCode;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public List<DepartmentRequisitionMonthly> getDepartmentRequisitions() {
        return departmentRequisitions;
    }

    public void setDepartmentRequisitions(List<DepartmentRequisitionMonthly> departmentRequisitions) {
        this.departmentRequisitions = departmentRequisitions;
    }

    public BigDecimal getDailyMedInventory() {
        return dailyMedInventory;
    }

    public void setDailyMedInventory(BigDecimal dailyMedInventory) {
        this.dailyMedInventory = dailyMedInventory;
    }

    public BigDecimal getSafeStock() {
        return safeStock;
    }

    public void setSafeStock(BigDecimal safeStock) {
        this.safeStock = safeStock;
    }

    public BigDecimal getTotalRequestQty() {
        return totalRequestQty;
    }

    public void setTotalRequestQty(BigDecimal totalRequestQty) {
        this.totalRequestQty = totalRequestQty;
    }

    public BigDecimal getUseStockQty() {
        return useStockQty;
    }

    public void setUseStockQty(BigDecimal useStockQty) {
        this.useStockQty = useStockQty;
    }

    public BigDecimal getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemarkComparison() {
        return remarkComparison;
    }

    public void setRemarkComparison(String remarkComparison) {
        this.remarkComparison = remarkComparison;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}