package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequisitionMonthlyService {

    private List<RequisitionMonthly> requisitions;

    private boolean isDuplicate(RequisitionMonthly requisition) {
        return requisitions.stream().anyMatch(r ->
                r.getOldSAPCode().equals(requisition.getOldSAPCode()) &&
                        r.getItemDescriptionEN().equals(requisition.getItemDescriptionEN()) &&
                        r.getItemDescriptionVN().equals(requisition.getItemDescriptionVN()) &&
                        r.getProductType1Id() == requisition.getProductType1Id() &&
                        r.getProductType2Id() == requisition.getProductType2Id());
    }

    public RequisitionMonthly createRequisition(RequisitionMonthly requisition) {
        if (isDuplicate(requisition)) {
            throw new RuntimeException("Requisition with the same SAP code, descriptions, and product types already exists.");
        }

        requisition.setCreatedDate(LocalDateTime.now());
        requisition.setUpdatedDate(LocalDateTime.now());
        requisitions.add(requisition);
        return requisition;
    }

    public RequisitionMonthly updateRequisition(int id, RequisitionMonthly updatedRequisition) {
        if (isDuplicate(updatedRequisition)) {
            throw new RuntimeException("Requisition with the same SAP code, descriptions, and product types already exists.");
        }

        RequisitionMonthly existingRequisition = requisitions.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Requisition not found"));

        existingRequisition.setUpdatedDate(LocalDateTime.now());
        existingRequisition.setItemDescriptionEN(updatedRequisition.getItemDescriptionEN());
        existingRequisition.setItemDescriptionVN(updatedRequisition.getItemDescriptionVN());
        existingRequisition.setOldSAPCode(updatedRequisition.getOldSAPCode());
        existingRequisition.setSapCodeNewSAP(updatedRequisition.getSapCodeNewSAP());
        existingRequisition.setUnit(updatedRequisition.getUnit());
        existingRequisition.setDepartmentRequisitions(updatedRequisition.getDepartmentRequisitions());
        existingRequisition.setTotalNotIssuedQty(updatedRequisition.getTotalNotIssuedQty());
        existingRequisition.setInHand(updatedRequisition.getInHand());
        existingRequisition.setActualInHand(updatedRequisition.getActualInHand());
        existingRequisition.setPurchasing(updatedRequisition.getPurchasing());
        existingRequisition.setAmount(updatedRequisition.getAmount());
        existingRequisition.setSupplierId(updatedRequisition.getSupplierId());
        existingRequisition.setProductType1Id(updatedRequisition.getProductType1Id());
        existingRequisition.setProductType2Id(updatedRequisition.getProductType2Id());

        return existingRequisition;
    }

    public void deleteRequisition(int id) {
        requisitions.removeIf(r -> r.getId() == id);
    }

    public List<RequisitionMonthly> getLatestRequisitions() {
        return requisitions.stream()
                .sorted((r1, r2) -> r2.getCreatedDate().compareTo(r1.getCreatedDate()))  // Sắp xếp theo ngày tạo mới nhất
                .collect(Collectors.toList());
    }
}
