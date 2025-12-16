package org.bsl.pricecomparison.dto;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "purchase_history_items")
@CompoundIndex(
        name = "uk_group_requisition",
        def = "{'groupId': 1, 'requisitionMonthlyId': 1}",
        unique = true
)
public class PurchaseHistoryItem {

    @Id
    private String id;

    private String groupId;
    private String requisitionMonthlyId;

    private String oldSapCode;
    private String hanaSapCode;

    private String itemDescriptionEN;
    private String itemDescriptionVN;

    private String supplierId;
    private String supplierCodeSnapshot;
    private String supplierNameSnapshot;

    private String currencySnapshot;
    private String goodTypeSnapshot;

    private String unitSnapshot;                // ✅ đơn vị lúc mua
    private BigDecimal unitPriceSnapshot;       // ✅ giá/unit lúc mua
    private BigDecimal orderQtySnapshot;        // ✅ số lượng mua (orderQty)
    private BigDecimal amountSnapshot;          // ✅ orderQty * unitPrice

    private LocalDateTime purchasedAt;          // ✅ ngày giờ mua
    private String purchasedBy;                 // optional

    public PurchaseHistoryItem() {}

    // ===== getters/setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getRequisitionMonthlyId() { return requisitionMonthlyId; }
    public void setRequisitionMonthlyId(String requisitionMonthlyId) { this.requisitionMonthlyId = requisitionMonthlyId; }

    public String getOldSapCode() { return oldSapCode; }
    public void setOldSapCode(String oldSapCode) { this.oldSapCode = oldSapCode; }

    public String getHanaSapCode() { return hanaSapCode; }
    public void setHanaSapCode(String hanaSapCode) { this.hanaSapCode = hanaSapCode; }

    public String getItemDescriptionEN() { return itemDescriptionEN; }
    public void setItemDescriptionEN(String itemDescriptionEN) { this.itemDescriptionEN = itemDescriptionEN; }

    public String getItemDescriptionVN() { return itemDescriptionVN; }
    public void setItemDescriptionVN(String itemDescriptionVN) { this.itemDescriptionVN = itemDescriptionVN; }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }

    public String getSupplierCodeSnapshot() { return supplierCodeSnapshot; }
    public void setSupplierCodeSnapshot(String supplierCodeSnapshot) { this.supplierCodeSnapshot = supplierCodeSnapshot; }

    public String getSupplierNameSnapshot() { return supplierNameSnapshot; }
    public void setSupplierNameSnapshot(String supplierNameSnapshot) { this.supplierNameSnapshot = supplierNameSnapshot; }

    public String getCurrencySnapshot() { return currencySnapshot; }
    public void setCurrencySnapshot(String currencySnapshot) { this.currencySnapshot = currencySnapshot; }

    public String getGoodTypeSnapshot() { return goodTypeSnapshot; }
    public void setGoodTypeSnapshot(String goodTypeSnapshot) { this.goodTypeSnapshot = goodTypeSnapshot; }

    public String getUnitSnapshot() { return unitSnapshot; }
    public void setUnitSnapshot(String unitSnapshot) { this.unitSnapshot = unitSnapshot; }

    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }

    public BigDecimal getOrderQtySnapshot() { return orderQtySnapshot; }
    public void setOrderQtySnapshot(BigDecimal orderQtySnapshot) { this.orderQtySnapshot = orderQtySnapshot; }

    public BigDecimal getAmountSnapshot() { return amountSnapshot; }
    public void setAmountSnapshot(BigDecimal amountSnapshot) { this.amountSnapshot = amountSnapshot; }

    public LocalDateTime getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(LocalDateTime purchasedAt) { this.purchasedAt = purchasedAt; }

    public String getPurchasedBy() { return purchasedBy; }
    public void setPurchasedBy(String purchasedBy) { this.purchasedBy = purchasedBy; }
}
