// src/main/java/org/bsl/pricecomparison/model/RequisitionMonthly.java
package org.bsl.pricecomparison.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.bsl.pricecomparison.dto.CompletedSupplierDTO;
import org.bsl.pricecomparison.enums.RequisitionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "requisition_monthly")
public class RequisitionMonthly {

    @Id
    private String id;

    @Indexed
    private String groupId;

    private String itemDescriptionEN;
    private String itemDescriptionVN;
    private String oldSAPCode;
    private String hanaSAPCode;
    private String unit;

    private List<DepartmentRequisitionMonthly> departmentRequisitions;

    @NotNull @DecimalMin("0.0")
    private BigDecimal dailyMedInventory;

    @NotNull @DecimalMin("0.0")
    private BigDecimal stock;

    @NotNull @DecimalMin("0.0")
    private BigDecimal totalRequestQty;

    @NotNull @DecimalMin("0.0")
    private BigDecimal safeStock;

    @NotNull @DecimalMin("0.0")
    private BigDecimal useStockQty;

    @NotNull @DecimalMin("0.0")
    private BigDecimal orderQty;

    @NotNull @DecimalMin("0.0")
    private BigDecimal amount;

    @NotNull @DecimalMin("0.0")
    private BigDecimal price;

    private String currency;
    private String goodType;

    @Indexed
    private String supplierId;
    private String supplierName;

    @Indexed
    private String productType1Id;
    @Indexed
    private String productType2Id;
    private String productType1Name;
    private String productType2Name;

    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // ==================== AUDIT USER (EMAIL ONLY) ====================
    @Field("createdByEmail")
    @Schema(description = "Email of the user who created this requisition", example = "buyer01@company.com")
    private String createdByEmail;

    @Field("updatedByEmail")
    @Schema(description = "Email of the user who updated this requisition most recently", example = "buyer01@company.com")
    private String updatedByEmail;

    @Field("completedByEmail")
    @Schema(description = "Email of the user who marked this requisition as completed", example = "buyer01@company.com")
    private String completedByEmail;

    @Field("uncompletedByEmail")
    @Schema(description = "Email of the user who unmarked completed status", example = "buyer02@company.com")
    private String uncompletedByEmail;

    // ==================== Completion tracking ====================
    @Field("completedDate")
    @Schema(description = "Date and time when this requisition was marked as completed", example = "2025-08-10T14:30:00")
    private LocalDateTime completedDate;

    @Field("isCompleted")
    @Schema(description = "Completion status: true = Completed, false = Not completed", example = "true")
    private Boolean isCompleted = false;   // default = not completed

    private List<String> imageUrls;

    private String fullDescription;
    private String reason;
    private String remark;
    private String remarkComparison;

    @Field("type")
    @Indexed
    @Schema(description = "Type of requisition", example = "MONTHLY")
    private RequisitionType type;

    @Field("supplierComparisonList")
    @Schema(description = "List of alternative suppliers for price comparison (sorted by price ascending)")
    private List<CompletedSupplierDTO> supplierComparisonList = new ArrayList<>();

    @Field("statusBestPrice")
    @Schema(description = "Indicates whether the current supplier has the best price: 'Yes' or 'No'")
    private String statusBestPrice;

    // ==================== CONSTRUCTORS ====================
    public RequisitionMonthly() {
        this.type = RequisitionType.MONTHLY;
        this.isCompleted = false;
    }

    // Full constructor
    public RequisitionMonthly(
            String id, String groupId,
            String itemDescriptionEN, String itemDescriptionVN,
            String oldSAPCode, String hanaSAPCode, String unit,
            List<DepartmentRequisitionMonthly> departmentRequisitions,
            BigDecimal dailyMedInventory, BigDecimal stock, BigDecimal totalRequestQty,
            BigDecimal safeStock, BigDecimal useStockQty, BigDecimal orderQty,
            BigDecimal amount, BigDecimal price, String currency, String goodType,
            String supplierId, String supplierName,
            String productType1Id, String productType2Id,
            String productType1Name, String productType2Name,
            LocalDateTime createdDate, LocalDateTime updatedDate,

            // audit (EMAIL ONLY)
            String createdByEmail, String updatedByEmail,
            String completedByEmail, String uncompletedByEmail,

            LocalDateTime completedDate, Boolean isCompleted,
            List<String> imageUrls,
            String fullDescription, String reason, String remark, String remarkComparison,
            RequisitionType type,
            List<CompletedSupplierDTO> supplierComparisonList,
            String statusBestPrice
    ) {
        this.id = id;
        this.groupId = groupId;
        this.itemDescriptionEN = itemDescriptionEN;
        this.itemDescriptionVN = itemDescriptionVN;
        this.oldSAPCode = oldSAPCode;
        this.hanaSAPCode = hanaSAPCode;
        this.unit = unit;
        this.departmentRequisitions = departmentRequisitions;
        this.dailyMedInventory = dailyMedInventory;
        this.stock = stock;
        this.totalRequestQty = totalRequestQty;
        this.safeStock = safeStock;
        this.useStockQty = useStockQty;
        this.orderQty = orderQty;
        this.amount = amount;
        this.price = price;
        this.currency = currency;
        this.goodType = goodType;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
        this.productType1Name = productType1Name;
        this.productType2Name = productType2Name;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;

        this.createdByEmail = createdByEmail;
        this.updatedByEmail = updatedByEmail;
        this.completedByEmail = completedByEmail;
        this.uncompletedByEmail = uncompletedByEmail;

        this.completedDate = completedDate;
        this.isCompleted = isCompleted != null ? isCompleted : false;

        this.imageUrls = imageUrls;
        this.fullDescription = fullDescription;
        this.reason = reason;
        this.remark = remark;
        this.remarkComparison = remarkComparison;
        this.type = type;
        this.supplierComparisonList = supplierComparisonList;
        this.statusBestPrice = statusBestPrice;
    }

    // ==================== HELPERS ====================
    @Schema(hidden = true)
    public String getCompletedStatus() {
        return Boolean.TRUE.equals(isCompleted) ? "Yes" : "No";
    }

    // ==================== GETTERS & SETTERS ====================
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

    public String getHanaSAPCode() { return hanaSAPCode; }
    public void setHanaSAPCode(String hanaSAPCode) { this.hanaSAPCode = hanaSAPCode; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public List<DepartmentRequisitionMonthly> getDepartmentRequisitions() { return departmentRequisitions; }
    public void setDepartmentRequisitions(List<DepartmentRequisitionMonthly> departmentRequisitions) { this.departmentRequisitions = departmentRequisitions; }

    public BigDecimal getDailyMedInventory() { return dailyMedInventory; }
    public void setDailyMedInventory(BigDecimal dailyMedInventory) { this.dailyMedInventory = dailyMedInventory; }

    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) { this.stock = stock; }

    public BigDecimal getTotalRequestQty() { return totalRequestQty; }
    public void setTotalRequestQty(BigDecimal totalRequestQty) { this.totalRequestQty = totalRequestQty; }

    public BigDecimal getSafeStock() { return safeStock; }
    public void setSafeStock(BigDecimal safeStock) { this.safeStock = safeStock; }

    public BigDecimal getUseStockQty() { return useStockQty; }
    public void setUseStockQty(BigDecimal useStockQty) { this.useStockQty = useStockQty; }

    public BigDecimal getOrderQty() { return orderQty; }
    public void setOrderQty(BigDecimal orderQty) { this.orderQty = orderQty; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getGoodType() { return goodType; }
    public void setGoodType(String goodType) { this.goodType = goodType; }

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

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted != null ? isCompleted : false; }

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

    public RequisitionType getType() { return type; }
    public void setType(RequisitionType type) { this.type = type; }

    public List<CompletedSupplierDTO> getSupplierComparisonList() { return supplierComparisonList; }
    public void setSupplierComparisonList(List<CompletedSupplierDTO> supplierComparisonList) { this.supplierComparisonList = supplierComparisonList; }

    public String getStatusBestPrice() { return statusBestPrice; }
    public void setStatusBestPrice(String statusBestPrice) { this.statusBestPrice = statusBestPrice; }

    // ===== AUDIT (EMAIL ONLY) getters/setters =====
    public String getCreatedByEmail() { return createdByEmail; }
    public void setCreatedByEmail(String createdByEmail) { this.createdByEmail = createdByEmail; }

    public String getUpdatedByEmail() { return updatedByEmail; }
    public void setUpdatedByEmail(String updatedByEmail) { this.updatedByEmail = updatedByEmail; }

    public String getCompletedByEmail() { return completedByEmail; }
    public void setCompletedByEmail(String completedByEmail) { this.completedByEmail = completedByEmail; }

    public String getUncompletedByEmail() { return uncompletedByEmail; }
    public void setUncompletedByEmail(String uncompletedByEmail) { this.uncompletedByEmail = uncompletedByEmail; }
}
