package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Response DTO for monthly comparison requisitions")
public class MonthlyComparisonRequisitionResponseDTO {
    @Schema(description = "List of monthly comparison requisition DTOs")
    private List<MonthlyComparisonRequisitionDTO> requisitions;

    @Schema(description = "Total amount in VND", example = "1000000.0")
    @NotNull(message = "Total amount in VND is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount in VND must be non-negative")
    private BigDecimal totalAmt;

    @Schema(description = "Total amount difference", example = "-2000.0")
    private BigDecimal totalAmtDifference;

    @Schema(description = "Total difference percentage", example = "-20.0")
    private BigDecimal totalDifferencePercentage;

    // Constructor
    public MonthlyComparisonRequisitionResponseDTO(
            List<MonthlyComparisonRequisitionDTO> requisitions,
            BigDecimal totalAmt,
            BigDecimal totalAmtDifference,
            BigDecimal totalDifferencePercentage
    ) {
        this.requisitions = requisitions;
        this.totalAmt = totalAmt;
        this.totalAmtDifference = totalAmtDifference;
        this.totalDifferencePercentage = totalDifferencePercentage;
    }

    // Getters
    public List<MonthlyComparisonRequisitionDTO> getRequisitions() {
        return requisitions;
    }

    public BigDecimal getTotalAmt() {
        return totalAmt;
    }

    public BigDecimal getTotalAmtDifference() {
        return totalAmtDifference;
    }

    public BigDecimal getTotalDifferencePercentage() {
        return totalDifferencePercentage;
    }

    public void setRequisitions(List<MonthlyComparisonRequisitionDTO> requisitions) {
        this.requisitions = requisitions;
    }

    public void setTotalAmt(BigDecimal totalAmt) {
        this.totalAmt = totalAmt;
    }

    public void setTotalAmtDifference(BigDecimal totalAmtDifference) {
        this.totalAmtDifference = totalAmtDifference;
    }

    public void setTotalDifferencePercentage(BigDecimal totalDifferencePercentage) {
        this.totalDifferencePercentage = totalDifferencePercentage;
    }
}