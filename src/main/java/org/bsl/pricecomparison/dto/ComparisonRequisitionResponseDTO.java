package org.bsl.pricecomparison.dto;

import java.util.List;

public class ComparisonRequisitionResponseDTO {
    private List<ComparisonRequisitionDTO> requisitions;
    private Double totalAmtVnd;
    private Double totalAmtDifference;
    private Double totalDifferencePercentage;

    // Constructor
    public ComparisonRequisitionResponseDTO(List<ComparisonRequisitionDTO> requisitions, Double totalAmtVnd, Double totalAmtDifference, Double totalDifferencePercentage) {
        this.requisitions = requisitions;
        this.totalAmtVnd = totalAmtVnd;
        this.totalAmtDifference = totalAmtDifference;
        this.totalDifferencePercentage = totalDifferencePercentage;
    }

    // Getters
    public List<ComparisonRequisitionDTO> getRequisitions() {
        return requisitions;
    }

    public Double getTotalAmtVnd() {
        return totalAmtVnd;
    }

    public Double getTotalAmtDifference() {
        return totalAmtDifference;
    }

    public Double getTotalDifferencePercentage() {
        return totalDifferencePercentage;
    }
}