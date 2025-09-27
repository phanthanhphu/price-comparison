package org.bsl.pricecomparison.response;

import org.bsl.pricecomparison.dto.RequisitionMonthlyDTO;
import org.springframework.data.domain.Page;

public class RequisitionMonthlyPagedResponse {
    private Page<RequisitionMonthlyDTO> requisitions;
    private Double totalSumNotIssuedQty;
    private Double totalSumInHand;
    private Double totalSumRequestQty;
    private Double totalSumActualInHand;
    private Double totalSumOrderQty;
    private Double totalSumAmount;
    private Double totalSumPrice;

    public RequisitionMonthlyPagedResponse(Page<RequisitionMonthlyDTO> requisitions,
                                           Double totalSumNotIssuedQty, Double totalSumInHand, Double totalSumRequestQty,
                                           Double totalSumActualInHand, Double totalSumOrderQty, Double totalSumAmount,
                                           Double totalSumPrice) {
        this.requisitions = requisitions;
        this.totalSumNotIssuedQty = totalSumNotIssuedQty;
        this.totalSumInHand = totalSumInHand;
        this.totalSumRequestQty = totalSumRequestQty;
        this.totalSumActualInHand = totalSumActualInHand;
        this.totalSumOrderQty = totalSumOrderQty;
        this.totalSumAmount = totalSumAmount;
        this.totalSumPrice = totalSumPrice;
    }

    // Getters
    public Page<RequisitionMonthlyDTO> getRequisitions() {
        return requisitions;
    }

    public Double getTotalSumNotIssuedQty() {
        return totalSumNotIssuedQty;
    }

    public Double getTotalSumInHand() {
        return totalSumInHand;
    }

    public Double getTotalSumRequestQty() {
        return totalSumRequestQty;
    }

    public Double getTotalSumActualInHand() {
        return totalSumActualInHand;
    }

    public Double getTotalSumOrderQty() {
        return totalSumOrderQty;
    }

    public Double getTotalSumAmount() {
        return totalSumAmount;
    }

    public Double getTotalSumPrice() {
        return totalSumPrice;
    }
}