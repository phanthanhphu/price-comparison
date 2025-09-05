package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class CreateSummaryRequisitionRequest {

    @ArraySchema(
            arraySchema = @Schema(description = "Requisition image files", type = "array"),
            minItems = 0,
            maxItems = 10,
            uniqueItems = false,
            schema = @Schema(type = "string", format = "binary")
    )
    private List<MultipartFile> files;

    @Schema(description = "English name", example = "Product XYZ")
    private String englishName;

    @Schema(description = "Vietnamese name", example = "Sản phẩm XYZ")
    private String vietnameseName;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSapCode;

    @Schema(description = "New SAP code", example = "NEW456")
    private String newSapCode;

    @Schema(description = "Department request quantities as JSON string", example = "{\"dept1\": 10.0, \"dept2\": 20.0}")
    private String departmentRequestQty;

    @Schema(description = "Stock quantity", example = "100.0")
    private Double stock;

    @Schema(description = "Purchasing suggestion", example = "50.0")
    private Double purchasingSuggest;

    @Schema(description = "Reason", example = "Urgent restock")
    private String reason;

    @Schema(description = "Remark", example = "High priority")
    private String remark;

    @Schema(description = "Supplier ID", example = "SUP123")
    private String supplierId;

    @Schema(description = "Group ID", example = "GRP456")
    private String groupId;

    @Schema(description = "Product Type 1 ID", example = "1")
    private String productType1Id;

    @Schema(description = "Product Type 2 ID", example = "2")
    private String productType2Id;

    @Schema(description = "Full description", example = "Detailed description of requisition")
    private String fullDescription;

    @Schema(description = "Total request quantity", example = "30.0")
    private Double totalRequestQty;

    @Schema(description = "Total price", example = "3000.0")
    private Double totalPrice;

    // --- Getters and Setters ---

    public List<MultipartFile> getFiles() {
        return files;
    }

    public void setFiles(List<MultipartFile> files) {
        this.files = files;
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

    public String getDepartmentRequestQty() {
        return departmentRequestQty;
    }

    public void setDepartmentRequestQty(String departmentRequestQty) {
        this.departmentRequestQty = departmentRequestQty;
    }

    public Double getStock() {
        return stock;
    }

    public void setStock(Double stock) {
        this.stock = stock;
    }

    public Double getPurchasingSuggest() {
        return purchasingSuggest;
    }

    public void setPurchasingSuggest(Double purchasingSuggest) {
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

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public Double getTotalRequestQty() {
        return totalRequestQty;
    }

    public void setTotalRequestQty(Double totalRequestQty) {
        this.totalRequestQty = totalRequestQty;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
}