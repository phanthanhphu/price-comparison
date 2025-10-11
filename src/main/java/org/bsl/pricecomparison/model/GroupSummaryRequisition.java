package org.bsl.pricecomparison.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "group_summary_requisition")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupSummaryRequisition {

    @Id
    private String id;
    private String name;
    private String status;
    private String type;
    private String currency; // New field for currency (e.g., VND, EURO, USD)
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime stockDate;

    @Transient
    private boolean isUsed; // Transient field to indicate if groupId is used

    public GroupSummaryRequisition() {
    }

    public GroupSummaryRequisition(String id, String name, String status, String type,
                                   String currency, LocalDateTime createdDate, String createdBy, LocalDateTime stockDate) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.type = type;
        this.currency = currency;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.stockDate = stockDate;
        this.isUsed = false; // Default to false
    }

    // Overloaded constructor including isUsed
    public GroupSummaryRequisition(String id, String name, String status, String type,
                                   String currency, LocalDateTime createdDate, String createdBy, LocalDateTime stockDate, boolean isUsed) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.type = type;
        this.currency = currency;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
        this.stockDate = stockDate;
        this.isUsed = isUsed;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getStockDate() {
        return stockDate;
    }

    public void setStockDate(LocalDateTime stockDate) {
        this.stockDate = stockDate;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }
}