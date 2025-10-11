package org.bsl.pricecomparison.response;

import org.bsl.pricecomparison.dto.RequisitionMonthlyDTO;
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public class RequisitionMonthlyPagedResponse {
    private Page<RequisitionMonthlyDTO> requisitions;

    @NotNull(message = "Total sum daily medical inventory is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum daily medical inventory must be non-negative")
    private BigDecimal totalSumDailyMedInventory;

    @NotNull(message = "Total sum safe stock is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum safe stock must be non-negative")
    private BigDecimal totalSumSafeStock;

    @NotNull(message = "Total sum request quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum request quantity must be non-negative")
    private BigDecimal totalSumRequestQty;

    @NotNull(message = "Total sum use stock quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum use stock quantity must be non-negative")
    private BigDecimal totalSumUseStockQty;

    @NotNull(message = "Total sum order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum order quantity must be non-negative")
    private BigDecimal totalSumOrderQty;

    @NotNull(message = "Total sum amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum amount must be non-negative")
    private BigDecimal totalSumAmount;

    @NotNull(message = "Total sum price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total sum price must be non-negative")
    private BigDecimal totalSumPrice;

    public RequisitionMonthlyPagedResponse(Page<RequisitionMonthlyDTO> requisitions,
                                           BigDecimal totalSumDailyMedInventory, BigDecimal totalSumSafeStock,
                                           BigDecimal totalSumRequestQty, BigDecimal totalSumUseStockQty,
                                           BigDecimal totalSumOrderQty, BigDecimal totalSumAmount,
                                           BigDecimal totalSumPrice) {
        this.requisitions = requisitions;
        this.totalSumDailyMedInventory = totalSumDailyMedInventory;
        this.totalSumSafeStock = totalSumSafeStock;
        this.totalSumRequestQty = totalSumRequestQty;
        this.totalSumUseStockQty = totalSumUseStockQty;
        this.totalSumOrderQty = totalSumOrderQty;
        this.totalSumAmount = totalSumAmount;
        this.totalSumPrice = totalSumPrice;
    }

    // Getters
    public Page<RequisitionMonthlyDTO> getRequisitions() {
        return requisitions;
    }

    public BigDecimal getTotalSumDailyMedInventory() {
        return totalSumDailyMedInventory;
    }

    public BigDecimal getTotalSumSafeStock() {
        return totalSumSafeStock;
    }

    public BigDecimal getTotalSumRequestQty() {
        return totalSumRequestQty;
    }

    public BigDecimal getTotalSumUseStockQty() {
        return totalSumUseStockQty;
    }

    public BigDecimal getTotalSumOrderQty() {
        return totalSumOrderQty;
    }

    public BigDecimal getTotalSumAmount() {
        return totalSumAmount;
    }

    public BigDecimal getTotalSumPrice() {
        return totalSumPrice;
    }
}