package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "group_summary_requisition")
public class GroupSummaryRequisition {

    @Id
    private String id;
    private String name;
    private String status;
    private String type;
    private LocalDateTime createdDate;
    private String createdBy;

    public GroupSummaryRequisition() {
    }

    public GroupSummaryRequisition(String id, String name, String status, String type,
                                   LocalDateTime createdDate, String createdBy) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.type = type;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
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

    public String getType() {   // getter
        return type;
    }

    public void setType(String type) {   // setter
        this.type = type;
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
}
