package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "departments")
public class Department {

    @Id
    private String id;

    private String division; // Tương ứng với cột "DIVISION"
    private String departmentName; // Tương ứng với cột "DEPARTMENT (PER COST CENTER)"

    @Indexed
    private LocalDateTime createdAt;

    public Department() {
        this.createdAt = LocalDateTime.now();
    }

    public Department(String id, String division, String departmentName) {
        this.id = id;
        this.division = division;
        this.departmentName = departmentName;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
