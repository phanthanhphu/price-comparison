package org.bsl.pricecomparison.model;

import org.bsl.pricecomparison.request.DepartmentQty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "summary_requisition")
public class SummaryRequisition {

    @Id
    private String id;

    private int no;
    private String englishName;
    private String vietnameseName;
    private String oldSapCode;
    private String newSapCode;

    private Map<String, DepartmentQty> departmentRequestQty;

    private double stock;
    private LocalDateTime dateStock;
    private double purchasingSuggest;
    private String reason;
    private String remark;
    private String remarkComparison;
    private String supplierId;

    private String groupId;
    private String productType1Id;
    private String productType2Id;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String fullDescription;
    private List<String> imageUrls;

    // Constructors
    public SummaryRequisition() {
    }

    public SummaryRequisition(String id, int no, String englishName, String vietnameseName, String oldSapCode,
                              String newSapCode, Map<String, DepartmentQty> departmentRequestQty, double stock,
                              LocalDateTime dateStock, double purchasingSuggest, String reason, String remark,
                              String remarkComparison, String supplierId, String groupId, String productType1Id,
                              String productType2Id, LocalDateTime createdAt, LocalDateTime updatedAt,
                              String fullDescription, List<String> imageUrls) {
        this.id = id;
        this.no = no;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.newSapCode = newSapCode;
        this.departmentRequestQty = departmentRequestQty;
        this.stock = stock;
        this.dateStock = dateStock;
        this.purchasingSuggest = purchasingSuggest;
        this.reason = reason;
        this.remark = remark;
        this.remarkComparison = remarkComparison;
        this.supplierId = supplierId;
        this.groupId = groupId;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fullDescription = fullDescription;
        this.imageUrls = imageUrls;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public void setVietnameseName(String vietnameseName) {
        this.vietnameseName = vietnameseName;
    }

    public String getOldSapCode() {
        return oldSapCode;
    }

    public void setOldSapCode(String oldSapCode) {
        this.oldSapCode = oldSapCode;
    }

    public String getNewSapCode() {
        return newSapCode;
    }

    public void setNewSapCode(String newSapCode) {
        this.newSapCode = newSapCode;
    }

    public Map<String, DepartmentQty> getDepartmentRequestQty() {
        return departmentRequestQty;
    }

    public void setDepartmentRequestQty(Map<String, DepartmentQty> departmentRequestQty) {
        this.departmentRequestQty = departmentRequestQty;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
    }

    public LocalDateTime getDateStock() {
        return dateStock;
    }

    public void setDateStock(LocalDateTime dateStock) {
        this.dateStock = dateStock;
    }

    public double getPurchasingSuggest() {
        return purchasingSuggest;
    }

    public void setPurchasingSuggest(double purchasingSuggest) {
        this.purchasingSuggest = purchasingSuggest;
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

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}