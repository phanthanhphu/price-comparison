package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "DTO for comparison requisition details")
public class ComparisonRequisitionDTO {

    @Schema(description = "ID của RequisitionMonthly (UUID)", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
    private String id;

    @Schema(description = "English name", example = "Product XYZ")
    private String englishName;

    @Schema(description = "Vietnamese name", example = "Sản phẩm XYZ")
    private String vietnameseName;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSapCode;

    @Schema(description = "Hana SAP code", example = "NEW456")
    private String hanaSapCode;

    @Schema(description = "List of supplier details")
    private List<SupplierDTO> suppliers;

    @Schema(description = "Remark for comparison", example = "Comparison note")
    private String remarkComparison;

    @Schema(description = "List of department request details")
    private List<DepartmentRequestDTO> departmentRequests;

    @Schema(description = "Selected price", example = "100.0")
    @NotNull(message = "Selected price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selected price must be non-negative")
    private BigDecimal selectedPrice;

    @Schema(description = "Total amount in group currency", example = "1000.0")
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be non-negative")
    private BigDecimal amtVnd;

    @Schema(description = "Amount difference", example = "-200.0")
    @NotNull(message = "Amount difference is required")
    private BigDecimal amtDifference;

    @Schema(description = "Percentage difference", example = "-20.0")
    @NotNull(message = "Percentage difference is required")
    private BigDecimal percentage;

    @Schema(description = "Highest price", example = "120.0")
    @NotNull(message = "Highest price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Highest price must be non-negative")
    private BigDecimal highestPrice;

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

    @Schema(description = "Total buy quantity across departments (SUM BUY)", example = "20")
    private int requestQty;

    @Schema(description = "Order quantity", example = "5.0")
    @NotNull(message = "Order quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Order quantity must be non-negative")
    private BigDecimal orderQty;

    @Schema(description = "Currency of group", example = "VND")
    private String currency;

    @Schema(description = "Good type of the selected supplier", example = "Material")
    private String goodType;

    @Schema(description = "Indicates if the selected price is the best price", example = "true")
    private boolean isBestPrice;

    // ===================== ✅ LAST PURCHASE INFO (NEW) =====================
    @Schema(description = "Last purchase supplier name (from last completed requisition), exclude current item")
    private String lastPurchaseSupplierName;

    @Schema(description = "Last purchase date (from last completed requisition), exclude current item")
    private LocalDateTime lastPurchaseDate;

    @Schema(description = "Last purchase price (group currency) (from last completed requisition), exclude current item")
    private BigDecimal lastPurchasePrice;

    @Schema(description = "Last purchase orderQty (from last completed requisition), exclude current item", example = "6")
    private BigDecimal lastPurchaseOrderQty;
    // ======================================================================

    public ComparisonRequisitionDTO(
            String id,
            String englishName,
            String vietnameseName,
            String oldSapCode,
            String hanaSapCode,
            List<SupplierDTO> suppliers,
            String remarkComparison,
            List<DepartmentRequestDTO> departmentRequests,
            BigDecimal selectedPrice,
            BigDecimal amtVnd,
            BigDecimal amtDifference,
            BigDecimal percentage,
            BigDecimal highestPrice,
            String type1,
            String type2,
            String type1Name,
            String type2Name,
            String unit,
            int requestQty,
            BigDecimal orderQty,
            String currency,
            String goodType,
            boolean isBestPrice
    ) {
        this.id = id;
        this.englishName = englishName;
        this.vietnameseName = vietnameseName;
        this.oldSapCode = oldSapCode;
        this.hanaSapCode = hanaSapCode;
        this.suppliers = suppliers;
        this.remarkComparison = remarkComparison;
        this.departmentRequests = departmentRequests;
        this.selectedPrice = selectedPrice;
        this.amtVnd = amtVnd;
        this.amtDifference = amtDifference;
        this.percentage = percentage;
        this.highestPrice = highestPrice;
        this.type1 = type1;
        this.type2 = type2;
        this.type1Name = type1Name;
        this.type2Name = type2Name;
        this.unit = unit;
        this.requestQty = requestQty;
        this.orderQty = orderQty;
        this.currency = currency;
        this.goodType = goodType;
        this.isBestPrice = isBestPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEnglishName() { return englishName; }
    public void setEnglishName(String englishName) { this.englishName = englishName; }

    public String getVietnameseName() { return vietnameseName; }
    public void setVietnameseName(String vietnameseName) { this.vietnameseName = vietnameseName; }

    public String getOldSapCode() { return oldSapCode; }
    public void setOldSapCode(String oldSapCode) { this.oldSapCode = oldSapCode; }

    public String getHanaSapCode() { return hanaSapCode; }
    public void setHanaSapCode(String hanaSapCode) { this.hanaSapCode = hanaSapCode; }

    public List<SupplierDTO> getSuppliers() { return suppliers; }
    public void setSuppliers(List<SupplierDTO> suppliers) { this.suppliers = suppliers; }

    public String getRemarkComparison() { return remarkComparison; }
    public void setRemarkComparison(String remarkComparison) { this.remarkComparison = remarkComparison; }

    public List<DepartmentRequestDTO> getDepartmentRequests() { return departmentRequests; }
    public void setDepartmentRequests(List<DepartmentRequestDTO> departmentRequests) { this.departmentRequests = departmentRequests; }

    public BigDecimal getSelectedPrice() { return selectedPrice; }
    public void setSelectedPrice(BigDecimal selectedPrice) { this.selectedPrice = selectedPrice; }

    public BigDecimal getAmtVnd() { return amtVnd; }
    public void setAmtVnd(BigDecimal amtVnd) { this.amtVnd = amtVnd; }

    public BigDecimal getAmtDifference() { return amtDifference; }
    public void setAmtDifference(BigDecimal amtDifference) { this.amtDifference = amtDifference; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public BigDecimal getHighestPrice() { return highestPrice; }
    public void setHighestPrice(BigDecimal highestPrice) { this.highestPrice = highestPrice; }

    public String getType1() { return type1; }
    public void setType1(String type1) { this.type1 = type1; }

    public String getType2() { return type2; }
    public void setType2(String type2) { this.type2 = type2; }

    public String getType1Name() { return type1Name; }
    public void setType1Name(String type1Name) { this.type1Name = type1Name; }

    public String getType2Name() { return type2Name; }
    public void setType2Name(String type2Name) { this.type2Name = type2Name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getRequestQty() { return requestQty; }
    public void setRequestQty(int requestQty) { this.requestQty = requestQty; }

    public BigDecimal getOrderQty() { return orderQty; }
    public void setOrderQty(BigDecimal orderQty) { this.orderQty = orderQty; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getGoodType() { return goodType; }
    public void setGoodType(String goodType) { this.goodType = goodType; }

    public boolean isBestPrice() { return isBestPrice; }
    public void setBestPrice(boolean bestPrice) { isBestPrice = bestPrice; }

    // ===== ✅ last purchase getters/setters =====
    public String getLastPurchaseSupplierName() { return lastPurchaseSupplierName; }
    public void setLastPurchaseSupplierName(String lastPurchaseSupplierName) { this.lastPurchaseSupplierName = lastPurchaseSupplierName; }

    public LocalDateTime getLastPurchaseDate() { return lastPurchaseDate; }
    public void setLastPurchaseDate(LocalDateTime lastPurchaseDate) { this.lastPurchaseDate = lastPurchaseDate; }

    public BigDecimal getLastPurchasePrice() { return lastPurchasePrice; }
    public void setLastPurchasePrice(BigDecimal lastPurchasePrice) { this.lastPurchasePrice = lastPurchasePrice; }

    public BigDecimal getLastPurchaseOrderQty() { return lastPurchaseOrderQty; }
    public void setLastPurchaseOrderQty(BigDecimal lastPurchaseOrderQty) { this.lastPurchaseOrderQty = lastPurchaseOrderQty; }
    // ==========================================

    @Schema(description = "Supplier details")
    public static class SupplierDTO {
        @Schema(description = "Price", example = "100.0")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        private BigDecimal price;

        @Schema(description = "Supplier name", example = "Supplier ABC")
        private String supplierName;

        @Schema(description = "Is selected (1 for selected, 0 for not selected)", example = "1")
        private Integer isSelected;

        @Schema(description = "Unit", example = "pcs")
        private String unit;

        public SupplierDTO(BigDecimal price, String supplierName, Integer isSelected, String unit) {
            this.price = price;
            this.supplierName = supplierName;
            this.isSelected = isSelected;
            this.unit = unit;
        }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public Integer getIsSelected() { return isSelected; }
        public void setIsSelected(Integer isSelected) { this.isSelected = isSelected; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
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

        public String getDepartmentId() { return departmentId; }
        public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
        public Integer getBuy() { return buy; }
        public void setBuy(Integer buy) { this.buy = buy; }
    }
}
