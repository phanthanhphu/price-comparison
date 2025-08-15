package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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

    private Map<String, Double> departmentRequestQty;

    private double stock;
    private double purchasingSuggest;
    private String reason;
    private String remark;
    private String supplierId;

    private String groupId;

    public SummaryRequisition() {
    }

    public SummaryRequisition(String id, int no, String englishName, String vietnameseName, String oldSapCode,
                              String newSapCode, Map<String, Double> departmentRequestQty,
                              double stock, double purchasingSuggest,
                              String reason, String remark, String supplierId, String groupId) {
        this.id = id;
        this.no = no;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.newSapCode = newSapCode;
        this.departmentRequestQty = departmentRequestQty;
        this.stock = stock;
        this.purchasingSuggest = purchasingSuggest;
        this.reason = reason;
        this.remark = remark;
        this.supplierId = supplierId;
        this.groupId = groupId;  // Gán giá trị cho groupId
    }

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

    public Map<String, Double> getDepartmentRequestQty() {
        return departmentRequestQty;
    }

    public void setDepartmentRequestQty(Map<String, Double> departmentRequestQty) {
        this.departmentRequestQty = departmentRequestQty;
    }

    public double getStock() {
        return stock;
    }

    public void setStock(double stock) {
        this.stock = stock;
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

    public double calculateTotalRequestQty() {
        if (departmentRequestQty == null) return 0;
        return departmentRequestQty.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double calculateAmount(double unitPrice) {
        return unitPrice * calculateTotalRequestQty();
    }
}