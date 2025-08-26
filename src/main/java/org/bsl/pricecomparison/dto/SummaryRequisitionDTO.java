package org.bsl.pricecomparison.dto;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.model.SummaryRequisition;

import java.util.List;

public class SummaryRequisitionDTO {

    private SummaryRequisition requisition;
    private SupplierProduct supplierProduct;
    private List<DepartmentRequestDTO> departmentRequests;
    private String productType1Name;
    private String productType2Name;
    private String groupId;

    public SummaryRequisitionDTO(
            SummaryRequisition requisition,
            SupplierProduct supplierProduct,
            List<DepartmentRequestDTO> departmentRequests,
            String productType1Name,
            String productType2Name
    ) {
        this.requisition = requisition;
        this.supplierProduct = supplierProduct;
        this.departmentRequests = departmentRequests;
        this.productType1Name = productType1Name;
        this.productType2Name = productType2Name;
        this.groupId = requisition != null ? requisition.getGroupId() : null;
    }

    // Inner class to hold department request details
    public static class DepartmentRequestDTO {
        private String departmentId;
        private String departmentName;
        private Integer quantity;

        public DepartmentRequestDTO(String departmentId, String departmentName, Integer quantity) {
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

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    // Getters and setters
    public SummaryRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(SummaryRequisition requisition) {
        this.requisition = requisition;
        this.groupId = requisition != null ? requisition.getGroupId() : null;
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
}