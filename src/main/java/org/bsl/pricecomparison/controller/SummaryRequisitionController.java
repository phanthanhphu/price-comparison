package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.dto.SummaryRequisitionWithSupplierDTO;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.model.SummaryRequisition;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/summary-requisitions")
public class SummaryRequisitionController {

    private final SummaryRequisitionRepository requisitionRepository;
    private final SupplierProductRepository supplierProductRepository;

    @Autowired
    public SummaryRequisitionController(SummaryRequisitionRepository requisitionRepository,
                                        SupplierProductRepository supplierProductRepository) {
        this.requisitionRepository = requisitionRepository;
        this.supplierProductRepository = supplierProductRepository;
    }

    @GetMapping
    public List<SummaryRequisition> getAll() {
        return requisitionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SummaryRequisitionWithSupplierDTO> getById(@PathVariable String id) {
        return requisitionRepository.findById(id)
                .map(req -> {
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository.findById(req.getSupplierId()).orElse(null);
                    }
                    SummaryRequisitionWithSupplierDTO dto = new SummaryRequisitionWithSupplierDTO(req, supplierProduct);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public SummaryRequisition create(@RequestBody SummaryRequisition summary) {
        return requisitionRepository.save(summary);
    }

    @PutMapping("/{id}")
    public SummaryRequisition update(@PathVariable String id, @RequestBody SummaryRequisition summary) {
        summary.setId(id);
        return requisitionRepository.save(summary);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        requisitionRepository.deleteById(id);
    }

    @GetMapping("/with-suppliers")
    public List<SummaryRequisitionWithSupplierDTO> getAllWithSupplierInfo() {
        List<SummaryRequisition> requisitions = requisitionRepository.findAll();

        return requisitions.stream()
                .map(req -> {
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository
                                .findById(req.getSupplierId())
                                .orElse(null);
                    }
                    return new SummaryRequisitionWithSupplierDTO(req, supplierProduct);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<SummaryRequisitionWithSupplierDTO>> getAllByGroupId(@PathVariable String groupId) {
        List<SummaryRequisition> requisitions = requisitionRepository.findByGroupId(groupId);

        List<SummaryRequisitionWithSupplierDTO> dtoList = requisitions.stream()
                .map(req -> {
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository.findById(req.getSupplierId()).orElse(null);
                    }
                    return new SummaryRequisitionWithSupplierDTO(req, supplierProduct);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }


}