package org.bsl.pricecomparison.dto;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.model.SummaryRequisition;

public class SummaryRequisitionWithSupplierDTO {

    private SummaryRequisition requisition;
    private SupplierProduct supplierProduct;

    public SummaryRequisitionWithSupplierDTO(SummaryRequisition requisition, SupplierProduct supplierProduct) {
        this.requisition = requisition;
        this.supplierProduct = supplierProduct;
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

    public String getGroupId() {
        return requisition != null ? requisition.getGroupId() : null;
    }
}
