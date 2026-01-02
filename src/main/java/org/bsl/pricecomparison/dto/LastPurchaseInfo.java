package org.bsl.pricecomparison.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LastPurchaseInfo {
    private final BigDecimal orderQty;          // SUM qty previous month
    private final BigDecimal price;             // latest all-time
    private final LocalDateTime date;           // latest all-time
    private final String supplierName;          // latest all-time

    public LastPurchaseInfo(BigDecimal orderQty, BigDecimal price, LocalDateTime date, String supplierName) {
        this.orderQty = orderQty;
        this.price = price;
        this.date = date;
        this.supplierName = supplierName;
    }

    public BigDecimal getOrderQty() { return orderQty; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getDate() { return date; }
    public String getSupplierName() { return supplierName; }
}
