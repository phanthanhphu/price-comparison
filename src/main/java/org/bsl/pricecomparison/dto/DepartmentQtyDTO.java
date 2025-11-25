package org.bsl.pricecomparison.dto;

import java.math.BigDecimal;

public class DepartmentQtyDTO {
    private String departmentId;
    private String departmentName;
    private BigDecimal qty;
    private BigDecimal buy;

    public DepartmentQtyDTO(String departmentId, String departmentName, BigDecimal qty, BigDecimal buy) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.qty = qty != null ? qty : BigDecimal.ZERO;
        this.buy = buy != null ? buy : BigDecimal.ZERO;
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

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty != null ? qty : BigDecimal.ZERO;
    }

    public BigDecimal getBuy() {
        return buy;
    }

    public void setBuy(BigDecimal buy) {
        this.buy = buy != null ? buy : BigDecimal.ZERO;
    }
}
