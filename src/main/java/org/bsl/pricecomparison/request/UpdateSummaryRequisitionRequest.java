package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Request to update an existing summary requisition")
public class UpdateSummaryRequisitionRequest {

    @Schema(description = "List of uploaded image files")
    private List<MultipartFile> files;

    @Schema(description = "JSON string containing list of image URLs to delete", example = "[\"/uploads/image1.jpg\", \"/uploads/image2.jpg\"]")
    private String imagesToDelete;

    @Schema(description = "English name of the product", example = "Product XYZ")
    private String englishName;

    @Schema(description = "Vietnamese name of the product", example = "Sản phẩm XYZ")
    private String vietnameseName;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSapCode;

    @Schema(description = "Hana SAP code", example = "NEW456")
    private String hanaSapCode;

    // ✅ NEW: Unit (editable)
    @Schema(description = "Unit of the requisition (editable)", example = "Box", nullable = true)
    private String unit;

    // ĐÃ SỬA: Tên field + example mới
    @Schema(description = "Department requisitions as JSON string",
            example = "[{\"id\": \"dept1\", \"name\": \"Finance Department\", \"qty\": 10, \"buy\": 8}, " +
                    "{\"id\": \"dept2\", \"name\": \"HR Department\", \"qty\": 20, \"buy\": 15}]")
    private String departmentRequisitions;

    @Schema(description = "Stock quantity", example = "100.0")
    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Stock quantity must be non-negative")
    private BigDecimal stock;

    @Schema(description = "Order quantity", example = "50.0")
    @NotNull(message = "Order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Order quantity must be non-negative")
    private BigDecimal orderQty;

    @Schema(description = "Reason for requisition", example = "Urgent restock")
    private String reason;

    @Schema(description = "Remark", example = "High priority")
    private String remark;

    @Schema(description = "Remark for comparison", example = "Comparison note")
    private String remarkComparison;

    @Schema(description = "Supplier ID", example = "689dbaddf1bf4d67a76ebae5")
    private String supplierId;

    @Schema(description = "Group ID", example = "689dbaddf1bf4d67a76ebae5")
    private String groupId;

    @Schema(description = "Product type 1 ID", example = "")
    private String productType1Id;

    @Schema(description = "Product type 2 ID", example = "")
    private String productType2Id;

    @Schema(description = "Full description", example = "Detailed description of requisition")
    private String fullDescription;

    // ==========================
    // Getters and Setters
    // ==========================
    public List<MultipartFile> getFiles() {
        return files;
    }

    public void setFiles(List<MultipartFile> files) {
        this.files = files;
    }

    public String getImagesToDelete() {
        return imagesToDelete;
    }

    public void setImagesToDelete(String imagesToDelete) {
        this.imagesToDelete = imagesToDelete;
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

    public String getHanaSapCode() {
        return hanaSapCode;
    }

    public void setHanaSapCode(String hanaSapCode) {
        this.hanaSapCode = hanaSapCode;
    }

    // ✅ NEW: unit getter/setter
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    // ĐÃ SỬA: Getter & Setter cho field mới
    public String getDepartmentRequisitions() {
        return departmentRequisitions;
    }

    public void setDepartmentRequisitions(String departmentRequisitions) {
        this.departmentRequisitions = departmentRequisitions;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    public BigDecimal getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
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

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }
}
