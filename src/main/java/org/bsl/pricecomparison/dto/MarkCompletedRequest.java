// src/main/java/org/bsl/pricecomparison/dto/MarkCompletedRequest.java
package org.bsl.pricecomparison.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MarkCompletedRequest {
    @NotEmpty(message = "requisitionIds cannot be empty")
    private List<String> requisitionIds;

    // getters and setters
    public List<String> getRequisitionIds() { return requisitionIds; }
    public void setRequisitionIds(List<String> requisitionIds) { this.requisitionIds = requisitionIds; }
}