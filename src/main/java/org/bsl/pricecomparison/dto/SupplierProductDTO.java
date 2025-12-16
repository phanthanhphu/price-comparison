package org.bsl.pricecomparison.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for SupplierProduct entity.
 * Represents a supplier product with validated fields for frontend consumption.
 */
public class SupplierProductDTO {

    private String id;
    private String supplierCode;
    private String supplierName;
    private String sapCode;

    // ĐÃ ĐỔI: itemNo → hanaSapCode
    private String hanaSapCode;

    // ĐÃ ĐỔI: itemDescription → itemDescriptionEN
    private String itemDescriptionEN;

    // ĐÃ ĐỔI: fullDescription → itemDescriptionVN
    private String itemDescriptionVN;

    private String materialGroupFullDescription;

    @Pattern(regexp = "^(VND|USD|EURO)$", message = "Currency must be VND, USD, or EURO")
    private String currency;

    @Pattern(regexp = "^(Common|Special)$", message = "Good Type must be Common or Special")
    private String goodType;

    private String size;

    /** Price as BigDecimal, serialized as string with currency-specific formatting */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;

    private String unit;
    private List<String> imageUrls;
    private String productType1Id;
    private String productType1Name;
    private String productType2Id;
    private String productType2Name;

    // Timestamp fields – formatted as ISO 8601 (e.g., "2025-04-05T10:30:45")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // ================== ✅ NEW: LAST PURCHASE INFO ==================

    @Schema(description = "Last purchase supplier name (from last completed requisition), exclude current item")
    private String lastPurchaseSupplierName;

    @Schema(description = "Last purchase date (from last completed requisition), exclude current item")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastPurchaseDate;

    @Schema(description = "Last purchase price (same currency matched) (from last completed requisition), exclude current item")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal lastPurchasePrice;

    @Schema(description = "Last purchase orderQty (from last completed requisition), exclude current item", example = "6")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal lastPurchaseOrderQty;

    // === GETTERS & SETTERS ===

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSapCode() { return sapCode; }
    public void setSapCode(String sapCode) { this.sapCode = sapCode; }

    public String getHanaSapCode() { return hanaSapCode; }
    public void setHanaSapCode(String hanaSapCode) { this.hanaSapCode = hanaSapCode; }

    public String getItemDescriptionEN() { return itemDescriptionEN; }
    public void setItemDescriptionEN(String itemDescriptionEN) { this.itemDescriptionEN = itemDescriptionEN; }

    public String getItemDescriptionVN() { return itemDescriptionVN; }
    public void setItemDescriptionVN(String itemDescriptionVN) { this.itemDescriptionVN = itemDescriptionVN; }

    public String getMaterialGroupFullDescription() { return materialGroupFullDescription; }
    public void setMaterialGroupFullDescription(String materialGroupFullDescription) { this.materialGroupFullDescription = materialGroupFullDescription; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getGoodType() { return goodType; }
    public void setGoodType(String goodType) { this.goodType = goodType; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getProductType1Id() { return productType1Id; }
    public void setProductType1Id(String productType1Id) { this.productType1Id = productType1Id; }

    public String getProductType1Name() { return productType1Name; }
    public void setProductType1Name(String productType1Name) { this.productType1Name = productType1Name; }

    public String getProductType2Id() { return productType2Id; }
    public void setProductType2Id(String productType2Id) { this.productType2Id = productType2Id; }

    public String getProductType2Name() { return productType2Name; }
    public void setProductType2Name(String productType2Name) { this.productType2Name = productType2Name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ✅ last purchase getters/setters
    public String getLastPurchaseSupplierName() { return lastPurchaseSupplierName; }
    public void setLastPurchaseSupplierName(String lastPurchaseSupplierName) { this.lastPurchaseSupplierName = lastPurchaseSupplierName; }

    public LocalDateTime getLastPurchaseDate() { return lastPurchaseDate; }
    public void setLastPurchaseDate(LocalDateTime lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }

    public BigDecimal getLastPurchasePrice() { return lastPurchasePrice; }
    public void setLastPurchasePrice(BigDecimal lastPurchasePrice) { this.lastPurchasePrice = lastPurchasePrice; }

    public BigDecimal getLastPurchaseOrderQty() { return lastPurchaseOrderQty; }
    public void setLastPurchaseOrderQty(BigDecimal lastPurchaseOrderQty) { this.lastPurchaseOrderQty = lastPurchaseOrderQty; }
}
