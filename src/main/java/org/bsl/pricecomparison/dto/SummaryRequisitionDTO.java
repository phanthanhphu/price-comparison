package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.model.SummaryRequisition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "DTO for summary requisition details")
public class SummaryRequisitionDTO {

    @Schema(description = "Summary requisition object")
    private SummaryRequisition requisition;

    @Schema(description = "Supplier product details")
    private SupplierProduct supplierProduct;

    @Schema(description = "List of department request details")
    private List<DepartmentRequestDTO> departmentRequests;

    @Schema(description = "Product Type 1 Name", example = "Device & tools")
    private String productType1Name;

    @Schema(description = "Product Type 2 Name", example = "Mechanical device & tools")
    private String productType2Name;

    @Schema(description = "Group ID", example = "68ec85a5f445635ed9a10bae")
    private String groupId;

    @Schema(description = "Total requested quantity (sum of qty)", example = "2")
    private Integer totalRequestQty;

    @Schema(description = "Total approved buy quantity (sum of buy)", example = "2")
    private Integer sumBuy;

    @Schema(description = "Stock quantity", example = "1")
    private BigDecimal stock;

    @Schema(description = "Order quantity", example = "2")
    private BigDecimal orderQty; // Changed from Integer to BigDecimal

    @Schema(description = "Total price based on supplier price and orderQty", example = "200000")
    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total price must be non-negative")
    private BigDecimal totalPrice;

    @Schema(description = "Unit price from supplier", example = "100000")
    private BigDecimal price;

    @Schema(description = "Currency code", example = "VND")
    private String currency;

    @Schema(description = "Type of goods", example = "Common")
    private String goodType;

    @Schema(description = "Supplier ID", example = "68f9f6791911f0486715498b")
    private String supplierId;

    @Schema(description = "Supplier name", example = "CÔNG TY TNHH THƯƠNG MẠI KỸ THUẬT DỊCH VỤ HOÀNG VŨ NAM")
    private String supplierName;

    @Schema(description = "Product Type 1 ID", example = "68a4272e922d8c55415d94d0")
    private String productType1Id;

    @Schema(description = "Product Type 2 ID", example = "68a4273f922d8c55415d94d1")
    private String productType2Id;

    @Schema(description = "List of image URLs", example = "[\"/uploads/1761574908665_maptest.png\"]")
    private List<String> imageUrls;

    @Schema(description = "Full description of the item", example = "ưerwewew")
    private String fullDescription;

    @Schema(description = "Reason for the requisition", example = "4")
    private String reason;

    @Schema(description = "Remark comparison", example = "Old price")
    private String remarkComparison;

    @Schema(description = "Date when the requisition was created", example = "2025-10-27T21:21:48.672")
    private String createdDate;

    @Schema(description = "Date when the requisition was last updated", example = "2025-10-27T21:21:48.672")
    private String updatedDate;

    public SummaryRequisitionDTO(
            SummaryRequisition requisition,
            SupplierProduct supplierProduct,
            List<DepartmentRequestDTO> departmentRequests,
            String productType1Name,
            String productType2Name,
            Integer totalRequestQty,
            Integer sumBuy,
            BigDecimal stock,
            BigDecimal orderQty, // Changed to BigDecimal
            BigDecimal totalPrice,
            BigDecimal price,
            String currency,
            String goodType,
            String supplierId,
            String supplierName,
            String productType1Id,
            String productType2Id,
            List<String> imageUrls,
            String fullDescription,
            String reason,
            String remarkComparison,
            String createdDate,
            String updatedDate
    ) {
        this.requisition = requisition;
        this.supplierProduct = supplierProduct;
        this.departmentRequests = departmentRequests;
        this.productType1Name = productType1Name;
        this.productType2Name = productType2Name;
        this.groupId = requisition != null ? requisition.getGroupId() : null;
        this.totalRequestQty = totalRequestQty;
        this.sumBuy = sumBuy;
        this.stock = stock;
        this.orderQty = orderQty;
        this.totalPrice = totalPrice;
        this.price = price;
        this.currency = currency;
        this.goodType = goodType;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.productType1Id = productType1Id;
        this.productType2Id = productType2Id;
        this.imageUrls = imageUrls;
        this.fullDescription = fullDescription;
        this.reason = reason;
        this.remarkComparison = remarkComparison;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    // Inner class to hold department request details
    @Schema(description = "Department request details")
    public static class DepartmentRequestDTO {
        @Schema(description = "Department ID", example = "temp_1758166905526")
        private String departmentId;

        @Schema(description = "Department name", example = "Sample F5")
        private String departmentName;

        @Schema(description = "Requested quantity", example = "2")
        private Integer qty;

        @Schema(description = "Approved buy quantity", example = "2")
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

    // Getters and setters
    public SummaryRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(SummaryRequisition requisition) {
        this.requisition = requisition;
        this.groupId = requisition != null ? requisition.getGroupId() : null;
        this.supplierId = requisition != null ? requisition.getSupplierId() : null;
        this.productType1Id = requisition != null ? requisition.getProductType1Id() : null;
        this.productType2Id = requisition != null ? requisition.getProductType2Id() : null;
        this.imageUrls = requisition != null ? requisition.getImageUrls() : null;
        this.fullDescription = requisition != null ? requisition.getFullDescription() : null;
        this.reason = requisition != null ? requisition.getReason() : null;
        this.remarkComparison = requisition != null ? requisition.getRemarkComparison() : null;
        this.createdDate = requisition != null && requisition.getCreatedAt() != null ? requisition.getCreatedAt().toString() : null;
        this.updatedDate = requisition != null && requisition.getUpdatedAt() != null ? requisition.getUpdatedAt().toString() : null;
    }

    public SupplierProduct getSupplierProduct() {
        return supplierProduct;
    }

    public void setSupplierProduct(SupplierProduct supplierProduct) {
        this.supplierProduct = supplierProduct;
        this.supplierName = supplierProduct != null ? supplierProduct.getSupplierName() : null;
        this.price = supplierProduct != null ? supplierProduct.getPrice() : null;
        this.currency = supplierProduct != null ? supplierProduct.getCurrency() : null;
        this.goodType = supplierProduct != null ? supplierProduct.getGoodType() : null;
    }

    public List<DepartmentRequestDTO> getDepartmentRequests() {
        return departmentRequests;
    }

    public void setDepartmentRequests(List<DepartmentRequestDTO> departmentRequests) {
        this.departmentRequests = departmentRequests;
    }

    public String getProductType1Name() {
        return productType1Name;
    }

    public void setProductType1Name(String productType1Name) {
        this.productType1Name = productType1Name;
    }

    public String getProductType2Name() {
        return productType2Name;
    }

    public void setProductType2Name(String productType2Name) {
        this.productType2Name = productType2Name;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Integer getTotalRequestQty() {
        return totalRequestQty;
    }

    public void setTotalRequestQty(Integer totalRequestQty) {
        this.totalRequestQty = totalRequestQty;
    }

    public Integer getSumBuy() {
        return sumBuy;
    }

    public void setSumBuy(Integer sumBuy) {
        this.sumBuy = sumBuy;
    }

    public BigDecimal getStock() {
        return stock;
    }

    public void setStock(BigDecimal stock) {
        this.stock = stock;
    }

    public BigDecimal getOrderQty() { // Changed to BigDecimal
        return orderQty;
    }

    public void setOrderQty(BigDecimal orderQty) { // Changed to BigDecimal
        this.orderQty = orderQty;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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

    public String getGoodType() {
        return goodType;
    }

    public void setGoodType(String goodType) {
        this.goodType = goodType;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getProductType1Id() {
        return productType1Id;
    }

    public void setProductType1Id(String productType1Id) {
        this.productType1Id = productType1Id;
    }

    public String getProductType2Id() {
        return productType2Id;
    }

    public void setProductType2Id(String productType2Id) {
        this.productType2Id = productType2Id;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRemarkComparison() {
        return remarkComparison;
    }

    public void setRemarkComparison(String remarkComparison) {
        this.remarkComparison = remarkComparison;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }
}