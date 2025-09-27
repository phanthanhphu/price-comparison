package org.bsl.pricecomparison.response;

import org.bsl.pricecomparison.dto.RequisitionMonthlyDTO;

import java.util.List;

public class RequisitionMonthlyResponse {
    private List<RequisitionMonthlyDTO> requisitions;
    private Double totalSumNotIssuedQty;
    private Double totalSumInHand;
    private Double totalSumRequestQty;
    private Double totalSumActualInHand;
    private Double totalSumOrderQty;
    private Double totalSumAmount;
    private Double totalSumPrice;

    public RequisitionMonthlyResponse(List<RequisitionMonthlyDTO> requisitions,
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
    public List<RequisitionMonthlyDTO> getRequisitions() {
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
