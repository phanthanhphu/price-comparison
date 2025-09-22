package org.bsl.pricecomparison.dto;

public class DepartmentQtyDTO {
    private String departmentId;
    private String departmentName;
    private double qty;
    private double buy;

    public DepartmentQtyDTO(String departmentId, String departmentName, double qty, double buy) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.qty = qty;
        this.buy = buy;
    }

    // Getters and Setters
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

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getBuy() {
        return buy;
    }

    public void setBuy(double buy) {
        this.buy = buy;
    }
}