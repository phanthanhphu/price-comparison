package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.service.RequisitionMonthlyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/requisitions")
public class RequisitionMonthlyController {

    @Autowired
    private RequisitionMonthlyService requisitionMonthlyService;

    // Thêm mới requisition
    @PostMapping
    public ResponseEntity<RequisitionMonthly> createRequisition(@RequestBody RequisitionMonthly requisition) {
        RequisitionMonthly createdRequisition = requisitionMonthlyService.createRequisition(requisition);
        return ResponseEntity.ok(createdRequisition);
    }

    // Cập nhật requisition
    @PutMapping("/{id}")
    public ResponseEntity<RequisitionMonthly> updateRequisition(@PathVariable int id, @RequestBody RequisitionMonthly requisition) {
        RequisitionMonthly updatedRequisition = requisitionMonthlyService.updateRequisition(id, requisition);
        return ResponseEntity.ok(updatedRequisition);
    }

    // Xóa requisition
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequisition(@PathVariable int id) {
        requisitionMonthlyService.deleteRequisition(id);
        return ResponseEntity.noContent().build();
    }

    // Lấy requisition mới nhất theo ngày tạo
    @GetMapping("/latest")
    public ResponseEntity<List<RequisitionMonthly>> getLatestRequisitions() {
        List<RequisitionMonthly> latestRequisitions = requisitionMonthlyService.getLatestRequisitions();
        return ResponseEntity.ok(latestRequisitions);
    }
}
