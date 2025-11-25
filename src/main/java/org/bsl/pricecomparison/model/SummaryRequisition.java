// src/main/java/org/bsl/pricecomparison/model/SummaryRequisition.java
package org.bsl.pricecomparison.model;

import org.bsl.pricecomparison.enums.RequisitionType;
import org.bsl.pricecomparison.request.DepartmentQty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "summary_requisition")
@Schema(description = "Model for summary requisition data")
public class SummaryRequisition {

    @Id
    @Schema(description = "Unique identifier of the requisition", example = "req123")
    private String id;

    @Schema(description = "Requisition number", example = "1")
    private int no;

    @Schema(description = "English name of the item", example = "Laptop")
    private String englishName;

    @Schema(description = "Vietnamese name of the item", example = "Máy tính xách tay")
    private String vietnameseName;

    @Schema(description = "Old SAP code of the item", example = "SAP123")
    private String oldSapCode;

    @Schema(description = "Hana SAP code of the item", example = "HANA456")
    private String hanaSapCode;

    @Schema(description = "Unit of measure for the item", example = "PC")
    private String unit;  // THÊM TRƯỜNG UNIT

    @Indexed
    @Schema(description = "Map of department request quantities")
    private Map<String, DepartmentQty> departmentRequestQty;

    @Schema(description = "Stock quantity", example = "100.0")
    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Stock quantity must be non-negative")
    private BigDecimal stock;

    @Schema(description = "Date of stock update", example = "2025-09-30T10:05:00")
    private LocalDateTime dateStock;

    @Schema(description = "Order quantity", example = "50.0")
    @NotNull(message = "Order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Order quantity must be non-negative")
    private BigDecimal orderQty;

    @Schema(description = "Reason for requisition", example = "Restock")
    private String reason;

    @Schema(description = "General remarks", example = "Urgent request")
    private String remark;

    @Schema(description = "Comparison remarks", example = "Compared with supplier A")
    private String remarkComparison;

    @Indexed
    @Schema(description = "Supplier ID", example = "supp789")
    private String supplierId;

    @Indexed
    @Schema(description = "Group ID", example = "GRP456")
    private String groupId;

    @Indexed
    @Schema(description = "Product Type 1 ID", example = "type1_123")
    private String productType1Id;

    @Indexed
    @Schema(description = "Product Type 2 ID", example = "type2_456")
    private String productType2Id;

    @Schema(description = "Creation date", example = "2025-09-30T10:05:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated date", example = "2025-09-30T12:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Full description of the item", example = "High-performance laptop")
    private String fullDescription;

    @Schema(description = "List of image URLs", example = "[\"http://example.com/image1.jpg\"]")
    private List<String> imageUrls;

    // === THÊM TRƯỜNG TYPE ===
    @Field("type")
    @Indexed
    @Schema(description = "Type of requisition", example = "SUMMARY", allowableValues = {"MONTHLY", "SUMMARY"})
    private RequisitionType type;

    // === CONSTRUCTORS ===
    public SummaryRequisition() {
        this.type = RequisitionType.WEEKLY;
    }

    public SummaryRequisition(
            String id,
            int no,
            String englishName,
            String vietnameseName,
            String oldSapCode,
            String hanaSapCode,
            String unit,  // THÊM UNIT VÀO CONSTRUCTOR
            Map<String, DepartmentQty> departmentRequestQty,
            BigDecimal stock,
            LocalDateTime dateStock,
            BigDecimal orderQty,
            String reason,
            String remark,
            String remarkComparison,
            String supplierId,
            String groupId,
            String productType1Id,
            String productType2Id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String fullDescription,
            List<String> imageUrls,
            RequisitionType type
    ) {
        this.id = id;
        this.no = no;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.hanaSapCode = hanaSapCode;
        this.unit = unit;  // GÁN GIÁ TRỊ CHO UNIT
        this.departmentRequestQty = departmentRequestQty;
        this.stock = stock;
        this.dateStock = dateStock;
        this.orderQty = orderQty;
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
        this.type = type != null ? type : RequisitionType.WEEKLY;
    }

    // === GETTERS & SETTERS ===
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getNo() { return no; }
    public void setNo(int no) { this.no = no; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public String getVietnameseName() { return vietnameseName; }
    public void setVietnameseName(String vietnameseName) { this.vietnameseName = vietnameseName; }

    public String getOldSapCode() { return oldSapCode; }
    public void setOldSapCode(String oldSapCode) { this.oldSapCode = oldSapCode; }

    public String getHanaSapCode() { return hanaSapCode; }
    public void setHanaSapCode(String hanaSapCode) { this.hanaSapCode = hanaSapCode; }

    public String getUnit() {  // GETTER UNIT
        return unit;
    }

    public void setUnit(String unit) {  // SETTER UNIT
        this.unit = unit;
    }

    public Map<String, DepartmentQty> getDepartmentRequestQty() { return departmentRequestQty; }
    public void setDepartmentRequestQty(Map<String, DepartmentQty> departmentRequestQty) {
        this.departmentRequestQty = departmentRequestQty;
    }

    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }

    public LocalDateTime getDateStock() { return dateStock; }
    public void setDateStock(LocalDateTime dateStock) { this.dateStock = dateStock; }

    public BigDecimal getOrderQty() { return orderQty; }
    public void setOrderQty(BigDecimal orderQty) { this.orderQty = orderQty; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getRemarkComparison() { return remarkComparison; }
    public void setRemarkComparison(String remarkComparison) { this.remarkComparison = remarkComparison; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getProductType1Id() { return productType1Id; }
    public void setProductType1Id(String productType1Id) { this.productType1Id = productType1Id; }

    public String getProductType2Id() { return productType2Id; }
    public void setProductType2Id(String productType2Id) { this.productType2Id = productType2Id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    // === GETTER & SETTER CHO TYPE ===
    public RequisitionType getType() {
        return type;
    }

    public void setType(RequisitionType type) {
        this.type = type != null ? type : RequisitionType.WEEKLY;
    }
}