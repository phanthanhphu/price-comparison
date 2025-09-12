package org.bsl.pricecomparison.dto;

import java.util.List;

public class ComparisonRequisitionDTO {

    private String englishName;
    private String vietnameseName;
    private String oldSapCode;
    private String newSapCode;
    private List<SupplierDTO> suppliers;
    private String remark;
    private List<DepartmentRequestDTO> departmentRequests;
    private Double price; // Giá của nhà cung cấp được chọn
    private Double amtVnd; // Giá được chọn * tổng quantity
    private Double amtDifference; // amtVnd - (giá cao nhất * tổng quantity)
    private Double percentage; // (amtDifference / amtVnd) * 100
    private Double highestPrice; // Giá cao nhất trong danh sách suppliers
    private String type1; // Product Type 1 ID
    private String type2; // Product Type 2 ID
    private String type1Name; // Product Type 1 Name
    private String type2Name; // Product Type 2 Name
    private String unit; // Unit of the selected supplier

    public ComparisonRequisitionDTO(String englishName, String vietnameseName, String oldSapCode, String newSapCode,
                                    List<SupplierDTO> suppliers, String remark, List<DepartmentRequestDTO> departmentRequests,
                                    Double price, Double amtVnd, Double amtDifference, Double percentage, Double highestPrice,
                                    String type1, String type2, String type1Name, String type2Name, String unit) {
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.newSapCode = newSapCode;
        this.suppliers = suppliers;
        this.remark = remark;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public static class SupplierDTO {
        private Double price;
        private String supplierName;
        private int isSelected; // 1 nếu nhà cung cấp được chọn, 0 nếu không
        private String unit; // Unit of the supplier

        public SupplierDTO(Double price, String supplierName, int isSelected, String unit) {
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

        public int getIsSelected() {
            return isSelected;
        }

        public void setIsSelected(int isSelected) {
            this.isSelected = isSelected;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public static class DepartmentRequestDTO {
        private String departmentId;
        private String departmentName;
        private int quantity;

        public DepartmentRequestDTO(String departmentId, String departmentName, int quantity) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.quantity = quantity;
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

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}