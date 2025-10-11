package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

public class CreateRequisitionMonthlyRequest {

    @ArraySchema(
            arraySchema = @Schema(description = "Requisition image files", type = "array"),
            minItems = 0,
            maxItems = 10,
            uniqueItems = false,
            schema = @Schema(type = "string", format = "binary")
    )
    private List<MultipartFile> files;

    @Schema(description = "English item description", example = "Product XYZ")
    private String itemDescriptionEN;

    @Schema(description = "Vietnamese item description", example = "Sản phẩm XYZ")
    private String itemDescriptionVN;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSAPCode;

    @Schema(description = "HANA SAP code", example = "NEW456")
    private String hanaSAPCode;

    @Schema(description = "Department requisitions as JSON string", example = "[{\"id\": \"dept1\", \"name\": \"Finance Department\", \"qty\": 10, \"buy\": 8}, {\"id\": \"dept2\", \"name\": \"HR Department\", \"qty\": 20, \"buy\": 15}]")
    private String departmentRequisitions;

    @Schema(description = "Daily medical inventory", example = "50.0")
    @NotNull(message = "Daily medical inventory is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily medical inventory must be non-negative")
    private BigDecimal dailyMedInventory;

    @Schema(description = "Safe stock quantity", example = "100.0")
    @NotNull(message = "Safe stock is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Safe stock must be non-negative")
    private BigDecimal safeStock;

    @Schema(description = "Supplier ID", example = "1")
    private String supplierId;

    @Schema(description = "Product Type 1 ID", example = "1")
    private String productType1Id;

    @Schema(description = "Product Type 2 ID", example = "2")
    private String productType2Id;

    @Schema(description = "Full description", example = "Detailed description of requisition")
    private String fullDescription;

    @Schema(description = "Reason for the requisition", example = "Needed for project X")
    private String reason;

    @Schema(description = "Remarks about the requisition", example = "This is urgent")
    private String remark;

    @Schema(description = "Comparison remarks", example = "Prices compared with last month")
    private String remarkComparison;

    @Schema(description = "Group ID", example = "689dbaddf1bf4d67a76ebae5")
    private String groupId;

    public List<MultipartFile> getFiles() {
        return files;
    }

    public void setFiles(List<MultipartFile> files) {
        this.files = files;
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

    public String getDepartmentRequisitions() {
        return departmentRequisitions;
    }

    public void setDepartmentRequisitions(String departmentRequisitions) {
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

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}