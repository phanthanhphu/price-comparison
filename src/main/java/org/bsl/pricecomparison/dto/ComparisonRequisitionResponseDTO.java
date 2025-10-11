package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Schema(description = "DTO for response containing paginated comparison requisitions and financial totals")
public class ComparisonRequisitionResponseDTO {
    @Schema(description = "Paginated list of comparison requisition details")
    private Page<ComparisonRequisitionDTO> page;

    @Schema(description = "Total amount in VND across all requisitions", example = "1000.0")
    @NotNull(message = "Total amount in VND is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount in VND must be non-negative")
    private BigDecimal totalAmt;

    @Schema(description = "Total amount difference across all requisitions", example = "-200.0")
    @NotNull(message = "Total amount difference is required")
    @DecimalMin(value = "-1000000.0", inclusive = true, message = "Total amount difference must be greater than or equal to -1000000")
    private BigDecimal totalAmtDifference;

    @Schema(description = "Total percentage difference across all requisitions", example = "-20.0")
    @NotNull(message = "Total percentage difference is required")
    @DecimalMin(value = "-100.0", inclusive = true, message = "Total percentage difference must be greater than or equal to -100")
    private BigDecimal totalDifferencePercentage;

    // Constructor
    public ComparisonRequisitionResponseDTO(Page<ComparisonRequisitionDTO> page,
                                            BigDecimal totalAmt,
                                            BigDecimal totalAmtDifference,
                                            BigDecimal totalDifferencePercentage) {
        this.page = page;
        this.totalAmt = totalAmt;
        this.totalAmtDifference = totalAmtDifference;
        this.totalDifferencePercentage = totalDifferencePercentage;
    }

    // Getters
    public Page<ComparisonRequisitionDTO> getPage() {
        return page;
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

    // Setters
    public void setPage(Page<ComparisonRequisitionDTO> page) {
        this.page = page;
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