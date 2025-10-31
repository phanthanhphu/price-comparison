package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quantity and approved buy amount for a department")
public class DepartmentQty {

    @Schema(description = "Requested quantity", example = "10")
    private Integer qty;

    @Schema(description = "Approved buy quantity", example = "8")
    private Integer buy;

    // === GETTERS & SETTERS ===
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Integer getBuy() {
        return buy;
    }

    public void setBuy(Integer buy) {
        this.buy = buy;
    }
}