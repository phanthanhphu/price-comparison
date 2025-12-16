package org.bsl.pricecomparison.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.bsl.pricecomparison.enums.RequisitionType;
import org.bsl.pricecomparison.model.DepartmentRequisitionMonthly;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequisitionMonthlyDTO {

    private String id;
    private String groupId;

    // ===== Product Type =====
    private String productType1Id;
    private String productType2Id;
    private String productType1Name;
    private String productType2Name;

    // ===== Item =====
    private String itemDescriptionEN;
    private String itemDescriptionVN;
    private String oldSAPCode;
    private String hanaSAPCode;
    private String unit;

    private List<DepartmentRequisitionMonthly> departmentRequisitions;

    // ===== Qty / Amount =====
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

    // ===== Supplier =====
    private String supplierId;
    private String supplierName;

    // ===== Dates =====
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // ===== Audit (EMAIL ONLY) =====
    private String createdByEmail;
    private String updatedByEmail;
    private String completedByEmail;
    private String uncompletedByEmail;

    // ===== Completion tracking =====
    private LocalDateTime completedDate;
    private Boolean isCompleted;

    // ===== Extra =====
    private List<String> imageUrls;
    private String fullDescription;
    private String reason;
    private String remark;
    private String remarkComparison;

    @NotNull
    private RequisitionType type;

    private List<CompletedSupplierDTO> supplierComparisonList = new ArrayList<>();
    private String statusBestPrice;

    public RequisitionMonthlyDTO() {}

    // Getter dạng computed (FE khỏi tự if)
    public String getCompletedStatus() {
        return Boolean.TRUE.equals(isCompleted) ? "Yes" : "No";
    }

    // ===== GETTERS / SETTERS (viết đủ hoặc dùng Lombok) =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getProductType1Id() { return productType1Id; }
    public void setProductType1Id(String productType1Id) { this.productType1Id = productType1Id; }

    public String getProductType2Id() { return productType2Id; }
    public void setProductType2Id(String productType2Id) { this.productType2Id = productType2Id; }

    public String getProductType1Name() { return productType1Name; }
    public void setProductType1Name(String productType1Name) { this.productType1Name = productType1Name; }

    public String getProductType2Name() { return productType2Name; }
    public void setProductType2Name(String productType2Name) { this.productType2Name = productType2Name; }

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

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    public String getCreatedByEmail() { return createdByEmail; }
    public void setCreatedByEmail(String createdByEmail) { this.createdByEmail = createdByEmail; }

    public String getUpdatedByEmail() { return updatedByEmail; }
    public void setUpdatedByEmail(String updatedByEmail) { this.updatedByEmail = updatedByEmail; }

    public String getCompletedByEmail() { return completedByEmail; }
    public void setCompletedByEmail(String completedByEmail) { this.completedByEmail = completedByEmail; }

    public String getUncompletedByEmail() { return uncompletedByEmail; }
    public void setUncompletedByEmail(String uncompletedByEmail) { this.uncompletedByEmail = uncompletedByEmail; }

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
    public void setSupplierComparisonList(List<CompletedSupplierDTO> supplierComparisonList) {
        this.supplierComparisonList = supplierComparisonList != null ? supplierComparisonList : new ArrayList<>();
    }

    public String getStatusBestPrice() { return statusBestPrice; }
    public void setStatusBestPrice(String statusBestPrice) { this.statusBestPrice = statusBestPrice; }
}
