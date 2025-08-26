package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.dto.SummaryRequisitionDTO;
import org.bsl.pricecomparison.dto.SummaryRequisitionWithSupplierDTO;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.DepartmentRepository;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private ProductType2Service productType2Service;

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
    public ResponseEntity<?> create(@RequestBody SummaryRequisition summary) {
        Optional<SummaryRequisition> existing = requisitionRepository.findByProductType1IdAndProductType2IdAndNewSapCode(
                summary.getProductType1Id(),
                summary.getProductType2Id(),
                summary.getNewSapCode()
        );

        if (existing.isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("Duplicate entry: productType1Id, productType2Id, and newSapCode must be unique together.");
        }

        summary.setCreatedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());

        SummaryRequisition saved = requisitionRepository.save(summary);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody SummaryRequisition summary) {
        try {
            Optional<SummaryRequisition> existingRequisition = requisitionRepository.findById(id);
            if (!existingRequisition.isPresent()) {
                return ResponseEntity
                        .status(404)
                        .body("Requisition with ID " + id + " not found.");
            }

            Optional<SummaryRequisition> duplicate = requisitionRepository.findByProductType1IdAndProductType2IdAndNewSapCode(
                    summary.getProductType1Id(),
                    summary.getProductType2Id(),
                    summary.getNewSapCode()
            );
            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                return ResponseEntity
                        .badRequest()
                        .body("Duplicate entry: productType1Id, productType2Id, and newSapCode must be unique together.");
            }

            SummaryRequisition current = existingRequisition.get();

            current.setEnglishName(summary.getEnglishName() != null ? summary.getEnglishName() : current.getEnglishName());
            current.setVietnameseName(summary.getVietnameseName() != null ? summary.getVietnameseName() : current.getVietnameseName());
            current.setFullDescription(summary.getFullDescription() != null ? summary.getFullDescription() : current.getFullDescription());
            current.setOldSapCode(summary.getOldSapCode() != null ? summary.getOldSapCode() : current.getOldSapCode());
            current.setNewSapCode(summary.getNewSapCode() != null ? summary.getNewSapCode() : current.getNewSapCode());
            current.setDepartmentRequestQty(summary.getDepartmentRequestQty() != null ? summary.getDepartmentRequestQty() : current.getDepartmentRequestQty());
            current.setStock(summary.getStock());
            current.setPurchasingSuggest(summary.getPurchasingSuggest());
            current.setReason(summary.getReason() != null ? summary.getReason() : current.getReason());
            current.setRemark(summary.getRemark() != null ? summary.getRemark() : current.getRemark());
            current.setSupplierId(summary.getSupplierId() != null && !summary.getSupplierId().isEmpty()
                    ? summary.getSupplierId()
                    : current.getSupplierId());
            current.setGroupId(summary.getGroupId() != null ? summary.getGroupId() : current.getGroupId());
            current.setProductType1Id(summary.getProductType1Id() != null ? summary.getProductType1Id() : current.getProductType1Id());
            current.setProductType2Id(summary.getProductType2Id() != null ? summary.getProductType2Id() : current.getProductType2Id());

            current.setUpdatedAt(LocalDateTime.now());

            SummaryRequisition updated = requisitionRepository.save(current);
            return ResponseEntity.ok(updated);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity
                    .status(400)
                    .body("Validation error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body("An error occurred while updating the requisition: " + e.getMessage());
        }
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
    public ResponseEntity<List<SummaryRequisitionDTO>> getAllByGroupId(@PathVariable String groupId) {
        List<SummaryRequisition> requisitions = requisitionRepository.findByGroupId(groupId);

        // Sort requisitions by updatedAt (if exists) and then createdAt (descending)
        List<SummaryRequisitionDTO> dtoList = requisitions.stream()
                .sorted((req1, req2) -> {
                    LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .map(req -> {
                    // Fetch SupplierProduct
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository.findById(req.getSupplierId()).orElse(null);
                    }

                    // Fetch Department Name for departmentRequestQty
                    List<SummaryRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequestQty().entrySet().stream()
                            .map(entry -> {
                                String departmentId = entry.getKey();
                                Double quantityDouble = entry.getValue();
                                Integer quantity = quantityDouble != null ? quantityDouble.intValue() : 0;
                                Department department = departmentRepository.findById(departmentId).orElse(null);
                                String departmentName = department != null ? department.getDepartmentName() : "Unknown";
                                return new SummaryRequisitionDTO.DepartmentRequestDTO(departmentId, departmentName, quantity);
                            })
                            .collect(Collectors.toList());

                    // Fetch ProductType1 and ProductType2 Names
                    String productType1Name = null;
                    if (req.getProductType1Id() != null) {
                        ProductType1 productType1 = productType1Service.getById(req.getProductType1Id());
                        productType1Name = productType1 != null ? productType1.getName() : "Unknown";
                    }

                    String productType2Name = null;
                    if (req.getProductType2Id() != null) {
                        ProductType2 productType2 = productType2Service.getById(req.getProductType2Id());
                        productType2Name = productType2 != null ? productType2.getName() : "Unknown";
                    }

                    // Create DTO with all required fields
                    return new SummaryRequisitionDTO(
                            req, supplierProduct, departmentRequests, productType1Name, productType2Name
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<SummaryRequisitionDTO>> searchRequisitions(
            @RequestParam String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String newSapCode,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String departmentName) {

        List<SummaryRequisitionDTO> dtoList = requisitionRepository.findByGroupId(groupId).stream()
                .filter(req -> {
                    boolean matches = true;

                    // Fetch ProductType1 and ProductType2 Names for filtering
                    String reqProductType1Name = req.getProductType1Id() != null ?
                            (productType1Service.getById(req.getProductType1Id()) != null ?
                                    productType1Service.getById(req.getProductType1Id()).getName() : "Unknown") : "Unknown";
                    String reqProductType2Name = req.getProductType2Id() != null ?
                            (productType2Service.getById(req.getProductType2Id()) != null ?
                                    productType2Service.getById(req.getProductType2Id()).getName() : "Unknown") : "Unknown";

                    // Fetch Department Names for filtering
                    List<String> deptNames = req.getDepartmentRequestQty().entrySet().stream()
                            .map(entry -> {
                                String deptId = entry.getKey();
                                Department dept = departmentRepository.findById(deptId).orElse(null);
                                return dept != null ? dept.getDepartmentName() : "Unknown";
                            })
                            .collect(Collectors.toList());

                    // Apply filters
                    if (productType1Name != null && !productType1Name.isEmpty()) {
                        matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                    }
                    if (productType2Name != null && !productType2Name.isEmpty()) {
                        matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                    }
                    if (englishName != null && !englishName.isEmpty()) {
                        matches = matches && req.getEnglishName() != null && req.getEnglishName().toLowerCase().contains(englishName.toLowerCase());
                    }
                    if (vietnameseName != null && !vietnameseName.isEmpty()) {
                        matches = matches && req.getVietnameseName() != null && req.getVietnameseName().toLowerCase().contains(vietnameseName.toLowerCase());
                    }
                    if (oldSapCode != null && !oldSapCode.isEmpty()) {
                        matches = matches && req.getOldSapCode() != null && req.getOldSapCode().toLowerCase().contains(oldSapCode.toLowerCase());
                    }
                    if (newSapCode != null && !newSapCode.isEmpty()) {
                        matches = matches && req.getNewSapCode() != null && req.getNewSapCode().toLowerCase().contains(newSapCode.toLowerCase());
                    }
                    if (unit != null && !unit.isEmpty()) {
                        SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;
                        String reqUnit = supplierProduct != null ? supplierProduct.getUnit() : "";
                        matches = matches && reqUnit.toLowerCase().contains(unit.toLowerCase());
                    }
                    if (departmentName != null && !departmentName.isEmpty()) {
                        matches = matches && deptNames.stream().anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                    }

                    return matches;
                })
                .sorted((req1, req2) -> {
                    LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .map(req -> convertToDto(req))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    private SummaryRequisitionDTO convertToDto(SummaryRequisition req) {
        // Fetch SupplierProduct
        SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;

        // Fetch Department Name for departmentRequestQty
        List<SummaryRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequestQty().entrySet().stream()
                .map(entry -> {
                    String departmentId = entry.getKey();
                    Double quantityDouble = entry.getValue();
                    Integer quantity = quantityDouble != null ? quantityDouble.intValue() : 0;
                    Department department = departmentRepository.findById(departmentId).orElse(null);
                    String departmentName = department != null ? department.getDepartmentName() : "Unknown";
                    return new SummaryRequisitionDTO.DepartmentRequestDTO(departmentId, departmentName, quantity);
                })
                .collect(Collectors.toList());

        // Fetch ProductType1 and ProductType2 Names
        String productType1Name = req.getProductType1Id() != null ?
                (productType1Service.getById(req.getProductType1Id()) != null ?
                        productType1Service.getById(req.getProductType1Id()).getName() : "Unknown") : "Unknown";
        String productType2Name = req.getProductType2Id() != null ?
                (productType2Service.getById(req.getProductType2Id()) != null ?
                        productType2Service.getById(req.getProductType2Id()).getName() : "Unknown") : "Unknown";

        return new SummaryRequisitionDTO(req, supplierProduct, departmentRequests, productType1Name, productType2Name);
    }

    private List<SummaryRequisitionDTO> convertToDtoList(List<SummaryRequisition> requisitions) {
        return requisitions.stream()
                .sorted((req1, req2) -> {
                    LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}