package org.bsl.pricecomparison.dto;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.model.SummaryRequisition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SummaryRequisitionWithSupplierDTO {

    private SummaryRequisition requisition;
    private SupplierProduct supplierProduct;
    private List<DepartmentQtyDTO> departmentRequestQuantities;

    public SummaryRequisitionWithSupplierDTO(SummaryRequisition requisition, SupplierProduct supplierProduct, List<DepartmentQtyDTO> departmentRequestQuantities) {
        this.requisition = requisition;
        this.supplierProduct = supplierProduct;
        this.departmentRequestQuantities = departmentRequestQuantities;
    }

    public SummaryRequisition getRequisition() {
        return requisition;
    }

    public void setRequisition(SummaryRequisition requisition) {
        this.requisition = requisition;
    }

    public SupplierProduct getSupplierProduct() {
        return supplierProduct;
    }

    public void setSupplierProduct(SupplierProduct supplierProduct) {
        this.supplierProduct = supplierProduct;
    }

    public List<DepartmentQtyDTO> getDepartmentRequestQuantities() {
        return departmentRequestQuantities;
    }

    public void setDepartmentRequestQuantities(List<DepartmentQtyDTO> departmentRequestQuantities) {
        this.departmentRequestQuantities = departmentRequestQuantities;
    }

    public String getGroupId() {
        return requisition != null ? requisition.getGroupId() : null;
    }
}