package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Quantity and approved buy amount for a department")
public class DepartmentQty {

    @Schema(description = "Requested quantity", example = "10")
    private BigDecimal qty;

    @Schema(description = "Approved buy quantity", example = "8")
    private BigDecimal buy;

    // === GETTERS & SETTERS ===
    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getBuy() {
        return buy;
    }

    public void setBuy(BigDecimal buy) {
        this.buy = buy;
    }
}
