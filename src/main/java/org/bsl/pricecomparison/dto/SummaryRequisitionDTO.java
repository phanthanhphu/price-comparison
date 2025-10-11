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

    @Schema(description = "Product Type 1 Name", example = "Electronics")
    private String productType1Name;

    @Schema(description = "Product Type 2 Name", example = "Smartphones")
    private String productType2Name;

    @Schema(description = "Group ID", example = "GRP456")
    private String groupId;

    @Schema(description = "Total approved buy quantity", example = "23")
    private Integer sumBuy;

    @Schema(description = "Total price based on supplier price and sumBuy", example = "66700000.0")
    @NotNull(message = "Total price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total price must be non-negative")
    private BigDecimal totalPrice;

    @Schema(description = "Date when the requisition was created", example = "2025-09-30T10:05:00")
    private String createdDate;

    @Schema(description = "Date when the requisition was last updated", example = "2025-09-30T12:30:00")
    private String updatedDate;

    public SummaryRequisitionDTO(
            SummaryRequisition requisition,
            SupplierProduct supplierProduct,
            List<DepartmentRequestDTO> departmentRequests,
            String productType1Name,
            String productType2Name,
            Integer sumBuy,
            BigDecimal totalPrice,
            String createdDate,
            String updatedDate
    ) {
        this.requisition = requisition;
        this.supplierProduct = supplierProduct;
        this.departmentRequests = departmentRequests;
        this.productType1Name = productType1Name;
        this.productType2Name = productType2Name;
        this.groupId = requisition != null ? requisition.getGroupId() : null;
        this.sumBuy = sumBuy;
        this.totalPrice = totalPrice;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
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

    // Getters and setters
    public SummaryRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(SummaryRequisition requisition) {
        this.requisition = requisition;
        this.groupId = requisition != null ? requisition.getGroupId() : null;
        this.createdDate = requisition != null && requisition.getCreatedAt() != null ? requisition.getCreatedAt().toString() : null;
        this.updatedDate = requisition != null && requisition.getUpdatedAt() != null ? requisition.getUpdatedAt().toString() : null;
    }

    public SupplierProduct getSupplierProduct() {
        return supplierProduct;
    }

    public void setSupplierProduct(SupplierProduct supplierProduct) {
        this.supplierProduct = supplierProduct;
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

    public Integer getSumBuy() {
        return sumBuy;
    }

    public void setSumBuy(Integer sumBuy) {
        this.sumBuy = sumBuy;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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