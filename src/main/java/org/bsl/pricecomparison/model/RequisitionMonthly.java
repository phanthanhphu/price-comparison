// src/main/java/org/bsl/pricecomparison/model/RequisitionMonthly.java
package org.bsl.pricecomparison.model;

import org.bsl.pricecomparison.enums.RequisitionType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private List<String> imageUrls;

    private String fullDescription;
    private String reason;
    private String remark;
    private String remarkComparison;

    // === TYPE DÙNG ENUM ===
    @Field("type")
    @Indexed
    @Schema(description = "Type of requisition", example = "MONTHLY")
    private RequisitionType type;

    // === CONSTRUCTORS ===
    public RequisitionMonthly() {
        this.type = RequisitionType.MONTHLY;
    }

    public RequisitionMonthly(String id, String groupId, String itemDescriptionEN, String itemDescriptionVN,
                              String oldSAPCode, String hanaSAPCode, String unit,
                              List<DepartmentRequisitionMonthly> departmentRequisitions,
                              BigDecimal dailyMedInventory, BigDecimal stock,
                              BigDecimal totalRequestQty, BigDecimal safeStock, BigDecimal useStockQty,
                              BigDecimal orderQty, BigDecimal amount, BigDecimal price,
                              String currency, String goodType, String supplierId, String supplierName,
                              String productType1Id, String productType2Id,
                              String productType1Name, String productType2Name,
                              LocalDateTime createdDate, LocalDateTime updatedDate,
                              List<String> imageUrls, String fullDescription,
                              String reason, String remark, String remarkComparison,
                              RequisitionType type) {
        this.id = id;
        this.groupId = groupId;
        this.itemDescriptionEN = itemDescriptionEN;
        this.itemDescriptionVN = itemDescriptionVN;
        this.oldSAPCode = oldSAPCode;
        this.hanaSAPCode = hanaSAPCode;
        this.unit = unit;
        this.departmentRequisitions = departmentRequisitions;
        this.dailyMedInventory = dailyMedInventory;
        this.stock = stock != null ? stock : dailyMedInventory;
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
        this.imageUrls = imageUrls;
        this.fullDescription = fullDescription;
        this.reason = reason;
        this.remark = remark;
        this.remarkComparison = remarkComparison;
        this.type = type != null ? type : RequisitionType.MONTHLY;
    }

    // === GETTERS & SETTERS ===
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
    public void setDepartmentRequisitions(List<DepartmentRequisitionMonthly> departmentRequisitions) {
        this.departmentRequisitions = departmentRequisitions;
    }

    public BigDecimal getDailyMedInventory() { return dailyMedInventory; }
    public void setDailyMedInventory(BigDecimal dailyMedInventory) {
        this.dailyMedInventory = dailyMedInventory;
        this.stock = dailyMedInventory;
    }

    public BigDecimal getStock() { return stock; }
    public void setStock(BigDecimal stock) {
        this.stock = stock;
        this.dailyMedInventory = stock;
    }

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
    public void setType(RequisitionType type) {
        this.type = type != null ? type : RequisitionType.MONTHLY;
    }
}