package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "requisition_monthly")
public class RequisitionMonthly {
    @Id
    private String id;

    @Schema(description = "Group ID for the requisition", example = "GROUP123")
    @Indexed
    private String groupId;

    @Schema(description = "English item description", example = "Product XYZ")
    private String itemDescriptionEN;

    @Schema(description = "Vietnamese item description", example = "Sản phẩm XYZ")
    private String itemDescriptionVN;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSAPCode;

    @Schema(description = "New SAP code", example = "NEW456")
    private String sapCodeNewSAP;

    @Schema(description = "Unit of measurement", example = "Piece")
    private String unit;

    @ArraySchema(
            arraySchema = @Schema(description = "List of department requisitions", type = "array"),
            schema = @Schema(description = "Department requisition details", type = "object")
    )
    private List<DepartmentRequisitionMonthly> departmentRequisitions;

    @Schema(description = "Total not issued quantity", example = "50.0")
    private Double totalNotIssuedQty;

    @Schema(description = "Total requested quantity", example = "30.0")
    private Double totalRequestQty;

    @Schema(description = "In-hand quantity", example = "100.0")
    private Double inHand;

    @Schema(description = "Actual in-hand quantity", example = "95.0")
    private Double actualInHand;

    @Schema(description = "Order quantity", example = "50.0")
    private Double orderQty;

    @Schema(description = "Total amount", example = "3000.0")
    private Double amount;

    @Schema(description = "Price", example = "100.0")
    private Double price;

    @Schema(description = "Supplier ID", example = "1")
    @Indexed // Added index for supplierId
    private String supplierId;

    @Schema(description = "Supplier name", example = "Supplier ABC")
    private String supplierName;

    @Schema(description = "Product Type 1 ID", example = "1")
    @Indexed
    private String productType1Id;

    @Schema(description = "Product Type 2 ID", example = "2")
    @Indexed
    private String productType2Id;

    @Schema(description = "Product Type 1 Name", example = "Electronics")
    private String productType1Name;

    @Schema(description = "Product Type 2 Name", example = "Smartphones")
    private String productType2Name;

    @Schema(description = "Created date", example = "2025-09-18T15:07:00")
    private LocalDateTime createdDate;

    @Schema(description = "Updated date", example = "2025-09-18T15:07:00")
    private LocalDateTime updatedDate;

    @ArraySchema(
            arraySchema = @Schema(description = "List of image URLs", type = "array"),
            schema = @Schema(type = "string", example = "http://example.com/image.jpg")
    )
    private List<String> imageUrls;

    @Schema(description = "Full description", example = "Detailed description of requisition")
    private String fullDescription;

    @Schema(description = "Reason for the requisition", example = "Needed for project X")
    private String reason;

    @Schema(description = "Remarks about the requisition", example = "This is urgent")
    private String remark;

    @Schema(description = "Comparison remarks", example = "Prices compared with last month")
    private String remarkComparison;

    public RequisitionMonthly() {
    }

    public RequisitionMonthly(String id, String groupId, String itemDescriptionEN, String itemDescriptionVN, String oldSAPCode, String sapCodeNewSAP, String unit, List<DepartmentRequisitionMonthly> departmentRequisitions, Double totalNotIssuedQty, Double totalRequestQty, Double inHand, Double actualInHand, Double orderQty, Double amount, Double price, String supplierId, String supplierName, String productType1Id, String productType2Id, String productType1Name, String productType2Name, LocalDateTime createdDate, LocalDateTime updatedDate, List<String> imageUrls, String fullDescription, String reason, String remark, String remarkComparison) {
        this.id = id;
        this.groupId = groupId;
        this.itemDescriptionEN = itemDescriptionEN;
        this.itemDescriptionVN = itemDescriptionVN;
        this.oldSAPCode = oldSAPCode;
        this.sapCodeNewSAP = sapCodeNewSAP;
        this.unit = unit;
        this.departmentRequisitions = departmentRequisitions;
        this.totalNotIssuedQty = totalNotIssuedQty;
        this.totalRequestQty = totalRequestQty;
        this.inHand = inHand;
        this.actualInHand = actualInHand;
        this.orderQty = orderQty;
        this.amount = amount;
        this.price = price;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
        this.productType1Name = productType1Name;
        this.productType2Name = productType2Name;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.imageUrls = imageUrls;
        this.fullDescription = fullDescription;
        this.reason = reason;
        this.remark = remark;
        this.remarkComparison = remarkComparison;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getItemDescriptionEN() { return itemDescriptionEN; }
    public void setItemDescriptionEN(String itemDescriptionEN) { this.itemDescriptionEN = itemDescriptionEN; }
    public String getItemDescriptionVN() { return itemDescriptionVN; }
    public void setItemDescriptionVN(String itemDescriptionVN) { this.itemDescriptionVN = itemDescriptionVN; }
    public String getOldSAPCode() { return oldSAPCode; }
    public void setOldSAPCode(String oldSAPCode) { this.oldSAPCode = oldSAPCode; }
    public String getSapCodeNewSAP() { return sapCodeNewSAP; }
    public void setSapCodeNewSAP(String sapCodeNewSAP) { this.sapCodeNewSAP = sapCodeNewSAP; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public List<DepartmentRequisitionMonthly> getDepartmentRequisitions() { return departmentRequisitions; }
    public void setDepartmentRequisitions(List<DepartmentRequisitionMonthly> departmentRequisitions) { this.departmentRequisitions = departmentRequisitions; }
    public Double getTotalNotIssuedQty() { return totalNotIssuedQty; }
    public void setTotalNotIssuedQty(Double totalNotIssuedQty) { this.totalNotIssuedQty = totalNotIssuedQty; }
    public Double getTotalRequestQty() { return totalRequestQty; }
    public void setTotalRequestQty(Double totalRequestQty) { this.totalRequestQty = totalRequestQty; }
    public Double getInHand() { return inHand; }
    public void setInHand(Double inHand) { this.inHand = inHand; }
    public Double getActualInHand() { return actualInHand; }
    public void setActualInHand(Double actualInHand) { this.actualInHand = actualInHand; }
    public Double getOrderQty() { return orderQty; }
    public void setOrderQty(Double orderQty) { this.orderQty = orderQty; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getProductType1Id() { return productType1Id; }
    public void setProductType1Id(String productType1Id) { this.productType1Id = productType1Id; }
    public String getProductType2Id() { return productType2Id; }
    public void setProductType2Id(String productType2Id) { this.productType2Id = productType2Id; }
    public String getProductType1Name() { return productType1Name; }
    public void setProductType1Name(String productType1Name) { this.productType1Name = productType1Name; }
    public String getProductType2Name() { return productType2Name; }
    public void setProductType2Name(String productType2Name) { this.productType2Name = productType2Name; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getRemarkComparison() { return remarkComparison; }
    public void setRemarkComparison(String remarkComparison) { this.remarkComparison = remarkComparison; }
}