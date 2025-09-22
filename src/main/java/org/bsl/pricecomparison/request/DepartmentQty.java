package org.bsl.pricecomparison.request;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Quantity and approved buy amount for a department")
public class DepartmentQty {
    @Schema(description = "Requested quantity", example = "10.0")
    private Double qty;

    @Schema(description = "Approved buy quantity", example = "8.0")
    private Double buy;

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getBuy() {
        return buy;
    }

    public void setBuy(Double buy) {
        this.buy = buy;
    }
}