package org.bsl.pricecomparison.dto;

class DepartmentRequestDTO {
    private String departmentId;
    private String departmentName;
    private Double quantity;

    public DepartmentRequestDTO(String departmentId, String departmentName, Double quantity) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.quantity = quantity;
    }

    // Getters and setters
    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }
}