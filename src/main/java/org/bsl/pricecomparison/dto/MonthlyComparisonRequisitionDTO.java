package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "DTO for comparison requisition details")
public class MonthlyComparisonRequisitionDTO {

    @Schema(description = "English name", example = "Product XYZ")
    private String englishName;

    @Schema(description = "Vietnamese name", example = "Sản phẩm XYZ")
    private String vietnameseName;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSapCode;

    @Schema(description = "HANA SAP code", example = "NEW456")
    private String hanaSapCode;

    @Schema(description = "List of supplier details")
    private List<SupplierDTO> suppliers;

    @Schema(description = "Remark for comparison", example = "Comparison note")
    private String remarkComparison;

    @Schema(description = "List of department request details")
    private List<DepartmentRequestDTO> departmentRequests;

    @Schema(description = "Total amount (orderQty * price)", example = "1000.0")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be non-negative")
    private BigDecimal amount;

    @Schema(description = "Amount difference", example = "-200.0")
    private BigDecimal amtDifference;

    @Schema(description = "Percentage difference", example = "-20.0")
    private BigDecimal percentage;

    @Schema(description = "Highest price", example = "120.0")
    @NotNull(message = "Highest price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Highest price must be non-negative")
    private BigDecimal highestPrice;

    @Schema(description = "Indicates if the selected supplier has the best price", example = "true")
    private Boolean isBestPrice;

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

    @Schema(description = "Daily medical inventory", example = "10.0")
    @NotNull(message = "Daily medical inventory is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily medical inventory must be non-negative")
    private BigDecimal dailyMedInventory;

    @Schema(description = "Total requested quantity", example = "15.0")
    @NotNull(message = "Total requested quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total requested quantity must be non-negative")
    private BigDecimal totalRequestQty;

    @Schema(description = "Safe stock level", example = "5.0")
    @NotNull(message = "Safe stock is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Safe stock must be non-negative")
    private BigDecimal safeStock;

    @Schema(description = "Used stock quantity", example = "3.0")
    @NotNull(message = "Used stock quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Used stock quantity must be non-negative")
    private BigDecimal useStockQty;

    @Schema(description = "Order quantity", example = "12.0")
    @NotNull(message = "Order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Order quantity must be non-negative")
    private BigDecimal orderQty;

    @Schema(description = "Selected supplier price", example = "100.0")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    @Schema(description = "Currency of the selected supplier", example = "USD")
    private String currency;

    @Schema(description = "Good type of the selected supplier", example = "Electronics")
    private String goodtype;

    public MonthlyComparisonRequisitionDTO(
            String englishName,
            String vietnameseName,
            String oldSapCode,
            String hanaSapCode,
            List<SupplierDTO> suppliers,
            String remarkComparison,
            List<DepartmentRequestDTO> departmentRequests,
            BigDecimal amount,
            BigDecimal amtDifference,
            BigDecimal percentage,
            BigDecimal highestPrice,
            Boolean isBestPrice,
            String type1,
            String type2,
            String type1Name,
            String type2Name,
            String unit,
            BigDecimal dailyMedInventory,
            BigDecimal totalRequestQty,
            BigDecimal safeStock,
            BigDecimal useStockQty,
            BigDecimal orderQty,
            BigDecimal price,
            String currency,
            String goodtype
    ) {
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.hanaSapCode = hanaSapCode;
        this.suppliers = suppliers;
        this.remarkComparison = remarkComparison;
        this.departmentRequests = departmentRequests;
        this.amount = amount;
        this.amtDifference = amtDifference;
        this.percentage = percentage;
        this.highestPrice = highestPrice;
        this.isBestPrice = isBestPrice;
        this.type1 = type1;
        this.type2 = type2;
        this.type1Name = type1Name;
        this.type2Name = type2Name;
        this.unit = unit;
        this.dailyMedInventory = dailyMedInventory;
        this.totalRequestQty = totalRequestQty;
        this.safeStock = safeStock;
        this.useStockQty = useStockQty;
        this.orderQty = orderQty;
        this.price = price;
        this.currency = currency;
        this.goodtype = goodtype;
    }

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

    public String getHanaSapCode() {
        return hanaSapCode;
    }

    public void setHanaSapCode(String hanaSapCode) {
        this.hanaSapCode = hanaSapCode;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmtDifference() {
        return amtDifference;
    }

    public void setAmtDifference(BigDecimal amtDifference) {
        this.amtDifference = amtDifference;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public BigDecimal getHighestPrice() {
        return highestPrice;
    }

    public void setHighestPrice(BigDecimal highestPrice) {
        this.highestPrice = highestPrice;
    }

    public Boolean getIsBestPrice() {
        return isBestPrice;
    }

    public void setIsBestPrice(Boolean isBestPrice) {
        this.isBestPrice = isBestPrice;
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

    public BigDecimal getDailyMedInventory() {
        return dailyMedInventory;
    }

    public void setDailyMedInventory(BigDecimal dailyMedInventory) {
        this.dailyMedInventory = dailyMedInventory;
    }

    public BigDecimal getTotalRequestQty() {
        return totalRequestQty;
    }

    public void setTotalRequestQty(BigDecimal totalRequestQty) {
        this.totalRequestQty = totalRequestQty;
    }

    public BigDecimal getSafeStock() {
        return safeStock;
    }

    public void setSafeStock(BigDecimal safeStock) {
        this.safeStock = safeStock;
    }

    public BigDecimal getUseStockQty() {
        return useStockQty;
    }

    public void setUseStockQty(BigDecimal useStockQty) {
        this.useStockQty = useStockQty;
    }

    public BigDecimal getOrderQty() {
        return orderQty;
    }

    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGoodtype() {
        return goodtype;
    }

    public void setGoodtype(String goodtype) {
        this.goodtype = goodtype;
    }

    @Schema(description = "Supplier details")
    public static class SupplierDTO {
        @Schema(description = "Price", example = "100.0")
        @NotNull(message = "Supplier price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Supplier price must be non-negative")
        private BigDecimal price;

        @Schema(description = "Supplier name", example = "Supplier ABC")
        private String supplierName;

        @Schema(description = "Is selected (1 for selected, 0 for not selected)", example = "1")
        private Integer isSelected;

        @Schema(description = "Unit", example = "pcs")
        private String unit;

        @Schema(description = "Indicates if this supplier has the best price", example = "true")
        private Boolean isBestPrice;

        public SupplierDTO(BigDecimal price, String supplierName, Integer isSelected, String unit, Boolean isBestPrice) {
            this.price = price;
            this.supplierName = supplierName;
            this.isSelected = isSelected;
            this.unit = unit;
            this.isBestPrice = isBestPrice;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
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

        public Boolean getIsBestPrice() {
            return isBestPrice;
        }

        public void setIsBestPrice(Boolean isBestPrice) {
            this.isBestPrice = isBestPrice;
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