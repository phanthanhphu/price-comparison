package org.bsl.pricecomparison.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class DepartmentRequisitionMonthly {

    @Schema(description = "Department ID", example = "dept1")
    private String id;        // ID của phòng ban

    @Schema(description = "Department name", example = "IT Department")
    private String name;      // Tên phòng ban

    @Schema(description = "Requested quantity", example = "10")
    private Integer qty;      // Số lượng yêu cầu

    @Schema(description = "Approved buy quantity", example = "8")
    private Integer buy;      // Số lượng được phê duyệt

    public DepartmentRequisitionMonthly() {
    }

    public DepartmentRequisitionMonthly(String id, String name, Integer qty, Integer buy) {
        this.id = id;
        this.name = name;
        this.qty = qty;
        this.buy = buy;
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

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }

    public Integer getBuy() {
        return buy;
    }

    public void setBuy(Integer buy) {
        this.buy = buy;
    }

    // Inner class to hold department request details
    @Schema(description = "Department request details")
    public static class DepartmentRequestDTO {
        @Schema(description = "Department ID", example = "dept1")
        private String departmentId;

        @Schema(description = "Department name", example = "IT Department")
        private String departmentName;

        @Schema(description = "Requested quantity", example = "10")
        private Integer qty;

        @Schema(description = "Approved buy quantity", example = "8")
        private Integer buy;

        public DepartmentRequestDTO(String departmentId, String departmentName, Integer qty, Integer buy) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.qty = qty;
            this.buy = buy;
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

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }

        public Integer getBuy() {
            return buy;
        }

        public void setBuy(Integer buy) {
            this.buy = buy;
        }
    }
}