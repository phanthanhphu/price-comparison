
package org.bsl.pricecomparison.dto;

import org.bsl.pricecomparison.model.DepartmentRequisitionMonthly;

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
    private String sapCodeNewSAP;
    private String unit;
    private List<DepartmentRequisitionMonthly> departmentRequisitions;
    private Double totalNotIssuedQty;
    private Double inHand;
    private Double totalRequestQty;
    private Double actualInHand;
    private Double orderQty;
    private Double amount;
    private Double price;
    private String supplierName;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String fullDescription;
    private String reason;
    private String remark;
    private String remarkComparison;
    private List<String> imageUrls;

    // New fields for totals
    private Double totalSumNotIssuedQty;
    private Double totalSumInHand;
    private Double totalSumRequestQty;
    private Double totalSumActualInHand;
    private Double totalSumOrderQty;
    private Double totalSumAmount;
    private Double totalSumPrice;

    public RequisitionMonthlyDTO(
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
            Double totalNotIssuedQty,
            Double inHand,
            Double totalRequestQty,
            Double actualInHand,
            Double orderQty,
            Double amount,
            Double price,
            String supplierName,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            String fullDescription,
            String reason,
            String remark,
            String remarkComparison,
            List<String> imageUrls,
            Double totalSumNotIssuedQty,
            Double totalSumInHand,
            Double totalSumRequestQty,
            Double totalSumActualInHand,
            Double totalSumOrderQty,
            Double totalSumAmount,
            Double totalSumPrice
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
        this.totalSumNotIssuedQty = totalSumNotIssuedQty;
        this.totalSumInHand = totalSumInHand;
        this.totalSumRequestQty = totalSumRequestQty;
        this.totalSumActualInHand = totalSumActualInHand;
        this.totalSumOrderQty = totalSumOrderQty;
        this.totalSumAmount = totalSumAmount;
        this.totalSumPrice = totalSumPrice;
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

    public Double getTotalNotIssuedQty() {
        return totalNotIssuedQty;
    }

    public void setTotalNotIssuedQty(Double totalNotIssuedQty) {
        this.totalNotIssuedQty = totalNotIssuedQty;
    }

    public Double getInHand() {
        return inHand;
    }

    public void setInHand(Double inHand) {
        this.inHand = inHand;
    }

    public Double getTotalRequestQty() {
        return totalRequestQty;
    }

    public void setTotalRequestQty(Double totalRequestQty) {
        this.totalRequestQty = totalRequestQty;
    }

    public Double getActualInHand() {
        return actualInHand;
    }

    public void setActualInHand(Double actualInHand) {
        this.actualInHand = actualInHand;
    }

    public Double getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(Double orderQty) {
        this.orderQty = orderQty;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
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

    public Double getTotalSumNotIssuedQty() {
        return totalSumNotIssuedQty;
    }

    public void setTotalSumNotIssuedQty(Double totalSumNotIssuedQty) {
        this.totalSumNotIssuedQty = totalSumNotIssuedQty;
    }

    public Double getTotalSumInHand() {
        return totalSumInHand;
    }

    public void setTotalSumInHand(Double totalSumInHand) {
        this.totalSumInHand = totalSumInHand;
    }

    public Double getTotalSumRequestQty() {
        return totalSumRequestQty;
    }

    public void setTotalSumRequestQty(Double totalSumRequestQty) {
        this.totalSumRequestQty = totalSumRequestQty;
    }

    public Double getTotalSumActualInHand() {
        return totalSumActualInHand;
    }

    public void setTotalSumActualInHand(Double totalSumActualInHand) {
        this.totalSumActualInHand = totalSumActualInHand;
    }

    public Double getTotalSumOrderQty() {
        return totalSumOrderQty;
    }

    public void setTotalSumOrderQty(Double totalSumOrderQty) {
        this.totalSumOrderQty = totalSumOrderQty;
    }

    public Double getTotalSumAmount() {
        return totalSumAmount;
    }

    public void setTotalSumAmount(Double totalSumAmount) {
        this.totalSumAmount = totalSumAmount;
    }

    public Double getTotalSumPrice() {
        return totalSumPrice;
    }

    public void setTotalSumPrice(Double totalSumPrice) {
        this.totalSumPrice = totalSumPrice;
    }
}