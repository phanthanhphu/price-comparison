package org.bsl.pricecomparison.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public class DepartmentRequisitionMonthly {

    @Schema(description = "Department ID", example = "dept1")
    private String id;

    @Schema(description = "Department name", example = "IT Department")
    private String name;

    @Schema(description = "Requested quantity", example = "10")
    private BigDecimal qty;

    @Schema(description = "Approved buy quantity", example = "8")
    private BigDecimal buy;

    public DepartmentRequisitionMonthly() {
    }

    public DepartmentRequisitionMonthly(String id, String name, BigDecimal qty, BigDecimal buy) {
        this.id = id;
        this.name = name;
        this.qty = qty;
        this.buy = buy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    // Inner class to hold department request details
    @Schema(description = "Department request details")
    public static class DepartmentRequestDTO {
        @Schema(description = "Department ID", example = "dept1")
        private String id;

        @Schema(description = "Department name", example = "IT Department")
        private String name;

        @Schema(description = "Requested quantity", example = "10")
        private BigDecimal qty;

        @Schema(description = "Approved buy quantity", example = "8")
        private BigDecimal buy;

        public DepartmentRequestDTO() {
        }

        public DepartmentRequestDTO(String id, String name, BigDecimal qty, BigDecimal buy) {
            this.id = id;
            this.name = name;
            this.qty = qty;
            this.buy = buy;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

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
}
