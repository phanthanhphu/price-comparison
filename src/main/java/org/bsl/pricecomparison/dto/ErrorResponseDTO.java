package org.bsl.pricecomparison.dto;

import java.util.List;

public class ErrorResponseDTO {
    private String message;
    private List<String> conflictingItems;

    public ErrorResponseDTO(String message, List<String> conflictingItems) {
        this.message = message;
        this.conflictingItems = conflictingItems;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getConflictingItems() {
        return conflictingItems;
    }

    public void setConflictingItems(List<String> conflictingItems) {
        this.conflictingItems = conflictingItems;
    }
}
