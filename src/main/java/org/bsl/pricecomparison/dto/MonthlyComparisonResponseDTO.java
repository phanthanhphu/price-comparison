package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "DTO for monthly comparison requisition details")
public class MonthlyComparisonResponseDTO {

    @Schema(description = "English name", example = "Product XYZ")
    private String englishName;

    @Schema(description = "Vietnamese name", example = "Sản phẩm XYZ")
    private String vietnameseName;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSapCode;

    @Schema(description = "New SAP code", example = "NEW456")
    private String newSapCode;

    @Schema(description = "List of supplier details")
    private List<SupplierDTO> suppliers;

    @Schema(description = "Remark for monthly comparison", example = "Monthly comparison note")
    private String remarkComparison;

    @Schema(description = "List of department request details")
    private List<DepartmentRequestDTO> departmentRequests;

    @Schema(description = "Selected price", example = "100.0")
    private Double price;

    @Schema(description = "Total amount in VND", example = "1000.0")
    private Double amtVnd;

    @Schema(description = "Amount difference", example = "-200.0")
    private Double amtDifference;

    @Schema(description = "Percentage difference", example = "-20.0")
    private Double percentage;

    @Schema(description = "Highest price", example = "120.0")
    private Double highestPrice;

    @Schema(description = "Product Type 1 ID", example = "1")
    private String type1;

    @Schema(description = "Product Type 2 ID", example = "2")
    private String type2;

    @Schema(description = "Product Type 1 Name", example = "Electronics")
    private String type1Name;

    @Schema(description = "Product Type 2 Name", example = "Smartphones")
    private String type2Name;

    @Schema(description = "Unit", example = "pcs")
    private String unit;

    public MonthlyComparisonResponseDTO(
            String englishName,
            String vietnameseName,
            String oldSapCode,
            String newSapCode,
            List<SupplierDTO> suppliers,
            String remarkComparison,
            List<DepartmentRequestDTO> departmentRequests,
            Double price,
            Double amtVnd,
            Double amtDifference,
            Double percentage,
            Double highestPrice,
            String type1,
            String type2,
            String type1Name,
            String type2Name,
            String unit
    ) {
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.newSapCode = newSapCode;
        this.suppliers = suppliers;
        this.remarkComparison = remarkComparison;
        this.departmentRequests = departmentRequests;
        this.price = price;
        this.amtVnd = amtVnd;
        this.amtDifference = amtDifference;
        this.percentage = percentage;
        this.highestPrice = highestPrice;
        this.type1 = type1;
        this.type2 = type2;
        this.type1Name = type1Name;
        this.type2Name = type2Name;
        this.unit = unit;
    }

    // Getters and setters
    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public void setVietnameseName(String vietnameseName) {
        this.vietnameseName = vietnameseName;
    }

    public String getOldSapCode() {
        return oldSapCode;
    }

    public void setOldSapCode(String oldSapCode) {
        this.oldSapCode = oldSapCode;
    }

    public String getNewSapCode() {
        return newSapCode;
    }

    public void setNewSapCode(String newSapCode) {
        this.newSapCode = newSapCode;
    }

    public List<SupplierDTO> getSuppliers() {
        return suppliers;
    }

    public void setSuppliers(List<SupplierDTO> suppliers) {
        this.suppliers = suppliers;
    }

    public String getRemarkComparison() {
        return remarkComparison;
    }

    public void setRemarkComparison(String remarkComparison) {
        this.remarkComparison = remarkComparison;
    }

    public List<DepartmentRequestDTO> getDepartmentRequests() {
        return departmentRequests;
    }

    public void setDepartmentRequests(List<DepartmentRequestDTO> departmentRequests) {
        this.departmentRequests = departmentRequests;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getAmtVnd() {
        return amtVnd;
    }

    public void setAmtVnd(Double amtVnd) {
        this.amtVnd = amtVnd;
    }

    public Double getAmtDifference() {
        return amtDifference;
    }

    public void setAmtDifference(Double amtDifference) {
        this.amtDifference = amtDifference;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Double getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(Double highestPrice) {
        this.highestPrice = highestPrice;
    }

    public String getType1() {
        return type1;
    }

    public void setType1(String type1) {
        this.type1 = type1;
    }

    public String getType2() {
        return type2;
    }

    public void setType2(String type2) {
        this.type2 = type2;
    }

    public String getType1Name() {
        return type1Name;
    }

    public void setType1Name(String type1Name) {
        this.type1Name = type1Name;
    }

    public String getType2Name() {
        return type2Name;
    }

    public void setType2Name(String type2Name) {
        this.type2Name = type2Name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Schema(description = "Supplier details")
    public static class SupplierDTO {
        @Schema(description = "Price", example = "100.0")
        private Double price;

        @Schema(description = "Supplier name", example = "Supplier ABC")
        private String supplierName;

        @Schema(description = "Is selected (1 for selected, 0 for not selected)", example = "1")
        private Integer isSelected;

        @Schema(description = "Unit", example = "pcs")
        private String unit;

        public SupplierDTO(Double price, String supplierName, Integer isSelected, String unit) {
            this.price = price;
            this.supplierName = supplierName;
            this.isSelected = isSelected;
            this.unit = unit;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }

        public Integer getIsSelected() {
            return isSelected;
        }

        public void setIsSelected(Integer isSelected) {
            this.isSelected = isSelected;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

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
