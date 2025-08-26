package org.bsl.pricecomparison.model;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

public class RequisitionMonthly {

    private int id;
    private String itemDescriptionEN;
    private String itemDescriptionVN;
    private String oldSAPCode;
    private String sapCodeNewSAP;
    private String unit;
    private List<DepartmentRequisitionMonthly> departmentRequisitions;  // Đổi tên từ 'departments' thành 'departmentRequisitions'
    private BigDecimal totalNotIssuedQty;
    private BigDecimal inHand;
    private BigDecimal actualInHand;
    private BigDecimal purchasing;
    private BigDecimal amount;
    private int supplierId;
    private int productType1Id;
    private int productType2Id;
    private LocalDateTime createdDate;   // Ngày tạo
    private LocalDateTime updatedDate;   // Ngày cập nhật

    // Constructor
    public RequisitionMonthly(int id, String itemDescriptionEN, String itemDescriptionVN,
                              String oldSAPCode, String sapCodeNewSAP, String unit, List<DepartmentRequisitionMonthly> departmentRequisitions,
                              BigDecimal totalNotIssuedQty, BigDecimal inHand, BigDecimal actualInHand,
                              BigDecimal purchasing, BigDecimal amount, int supplierId, int productType1Id,
                              int productType2Id, LocalDateTime createdDate, LocalDateTime updatedDate) {
        this.id = id;
        this.itemDescriptionEN = itemDescriptionEN;
        this.itemDescriptionVN = itemDescriptionVN;
        this.oldSAPCode = oldSAPCode;
        this.sapCodeNewSAP = sapCodeNewSAP;
        this.unit = unit;
        this.departmentRequisitions = departmentRequisitions;
        this.totalNotIssuedQty = totalNotIssuedQty;
        this.inHand = inHand;
        this.actualInHand = actualInHand;
        this.purchasing = purchasing;
        this.amount = amount;
        this.supplierId = supplierId;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public BigDecimal getActualInHand() {
        return actualInHand;
    }

    public void setActualInHand(BigDecimal actualInHand) {
        this.actualInHand = actualInHand;
    }

    public BigDecimal getPurchasing() {
        return purchasing;
    }

    public void setPurchasing(BigDecimal purchasing) {
        this.purchasing = purchasing;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public int getProductType1Id() {
        return productType1Id;
    }

    public void setProductType1Id(int productType1Id) {
        this.productType1Id = productType1Id;
    }

    public int getProductType2Id() {
        return productType2Id;
    }

    public void setProductType2Id(int productType2Id) {
        this.productType2Id = productType2Id;
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
}
