package org.bsl.pricecomparison.dto;

public class UpdateStatusBestPriceRequest {
    private String statusBestPrice;   // "EMPTY", "Yes", "No"
    private String remarkComparison;

    // Getters and Setters
    public String getStatusBestPrice() { return statusBestPrice; }
    public void setStatusBestPrice(String statusBestPrice) { this.statusBestPrice = statusBestPrice; }

    public String getRemarkComparison() { return remarkComparison; }
    public void setRemarkComparison(String remarkComparison) { this.remarkComparison = remarkComparison; }

}
