package org.bsl.pricecomparison.dto;

import org.bsl.pricecomparison.model.DepartmentRequisitionMonthly;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class UpdateRequisitionMonthlyDTO {
    private String id;
    private String groupId;
    private String productType1Name;
    private String productType2Name;
    private String itemDescriptionEN;
    private String itemDescriptionVN;
    private String oldSAPCode;
    private String sapCodeNewSAP;
    private String unit;
    private List<DepartmentRequisitionMonthly> departmentRequisitions;

    @NotNull(message = "Total not issued quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total not issued quantity must be non-negative")
    private BigDecimal totalNotIssuedQty;

    @NotNull(message = "In hand quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "In hand quantity must be non-negative")
    private BigDecimal inHand;

    @NotNull(message = "Total requested quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total requested quantity must be non-negative")
    private BigDecimal totalRequestQty;

    @NotNull(message = "Actual in hand quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Actual in hand quantity must be non-negative")
    private BigDecimal actualInHand;

    @NotNull(message = "Order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Order quantity must be non-negative")
    private BigDecimal orderQty;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be non-negative")
    private BigDecimal amount;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    private String supplierName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String fullDescription;
    private String reason;
    private String remark;
    private String remarkComparison;
    private List<String> imageUrls;

    public UpdateRequisitionMonthlyDTO(
            String id,
            String groupId,
            String productType1Name,
            String productType2Name,
            String itemDescriptionEN,
            String itemDescriptionVN,
            String oldSAPCode,
            String sapCodeNewSAP,
            String unit,
            List<DepartmentRequisitionMonthly> departmentRequisitions,
            BigDecimal totalNotIssuedQty,
            BigDecimal inHand,
            BigDecimal totalRequestQty,
            BigDecimal actualInHand,
            BigDecimal orderQty,
            BigDecimal amount,
            BigDecimal price,
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
        this.sapCodeNewSAP = sapCodeNewSAP;
        this.unit = unit;
        this.departmentRequisitions = departmentRequisitions;
        this.totalNotIssuedQty = totalNotIssuedQty;
        this.inHand = inHand;
        this.totalRequestQty = totalRequestQty;
        this.actualInHand = actualInHand;
        this.orderQty = orderQty;
        this.amount = amount;
        this.price = price;
        this.supplierName = supplierName;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.fullDescription = fullDescription;
        this.reason = reason;
        this.remark = remark;
        this.remarkComparison = remarkComparison;
        this.imageUrls = imageUrls;
    }

    // Getters and setters
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

    public String getSapCodeNewSAP() {
        return sapCodeNewSAP;
    }

    public void setSapCodeNewSAP(String sapCodeNewSAP) {
        this.sapCodeNewSAP = sapCodeNewSAP;
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

    public BigDecimal getTotalNotIssuedQty() {
        return totalNotIssuedQty;
    }

    public void setTotalNotIssuedQty(BigDecimal totalNotIssuedQty) {
        this.totalNotIssuedQty = totalNotIssuedQty;
    }

    public BigDecimal getInHand() {
        return inHand;
    }

    public void setInHand(BigDecimal inHand) {
        this.inHand = inHand;
    }

    public BigDecimal getTotalRequestQty() {
        return totalRequestQty;
    }

    public void setTotalRequestQty(BigDecimal totalRequestQty) {
        this.totalRequestQty = totalRequestQty;
    }

    public BigDecimal getActualInHand() {
        return actualInHand;
    }

    public void setActualInHand(BigDecimal actualInHand) {
        this.actualInHand = actualInHand;
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