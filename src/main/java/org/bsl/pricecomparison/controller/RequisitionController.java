// src/main/java/org/bsl/pricecomparison/controller/RequisitionController.java
package org.bsl.pricecomparison.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.bsl.pricecomparison.dto.RequisitionMonthlyDTO;
import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.repository.RequisitionMonthlyCustomRepository;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RequisitionController {

    @Autowired
    private RequisitionMonthlyCustomRepository customRepository;

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private ProductType2Service productType2Service;

    @GetMapping("/requisitions/search_2")
    @Operation(summary = "Tìm kiếm theo createdDate, sort createdDate/updatedDate")
    public ResponseEntity<Page<RequisitionMonthlyDTO>> searchRequisitions(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String typeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String productType1Id,
            @RequestParam(required = false) String productType2Id,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String hanaSapCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate,desc") String sort) {

        // === SỬA SORT: CHỈ CHO PHÉP createdDate / updatedDate ===
        String[] sortParams = sort.split(",");
        String field = sortParams[0].trim();
        String direction = sortParams.length > 1 ? sortParams[1].trim() : "desc";

        // Validate field
        if (!"createdDate".equalsIgnoreCase(field) && !"updatedDate".equalsIgnoreCase(field)) {
            field = "createdDate";
            direction = "desc";
        }

        // Validate direction
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            direction = "desc";  // SỬA: replace → "desc"
        }

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), field);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        // === HẾT SỬA ===

        String type = null;
        if (typeId != null && !typeId.isBlank()) {
            type = switch (typeId) {
                case "1" -> "WEEKLY";
                case "2" -> "MONTHLY";
                default -> null;
            };
        }

        Page<RequisitionMonthly> pageResult = customRepository.searchRequisitions(
                groupId, type, startDate, endDate,
                productType1Id, productType2Id,
                englishName, vietnameseName, oldSapCode, hanaSapCode,
                supplierName, departmentName, pageable
        );

        List<RequisitionMonthlyDTO> dtoList = pageResult.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        Page<RequisitionMonthlyDTO> dtoPage = new PageImpl<>(dtoList, pageable, pageResult.getTotalElements());

        return ResponseEntity.ok(dtoPage);
    }

    private RequisitionMonthlyDTO toDTO(RequisitionMonthly rm) {
        String p1Name = productType1Service.getNameById(rm.getProductType1Id());
        String p2Name = productType2Service.getNameById(rm.getProductType2Id());

        return new RequisitionMonthlyDTO(
                rm.getId(), rm.getGroupId(),
                p1Name != null ? p1Name : rm.getProductType1Name(),
                p2Name != null ? p2Name : rm.getProductType2Name(),
                rm.getItemDescriptionEN(), rm.getItemDescriptionVN(),
                rm.getOldSAPCode(), rm.getHanaSAPCode(), rm.getUnit(),
                rm.getDepartmentRequisitions(),
                rm.getDailyMedInventory(), rm.getSafeStock(),
                rm.getTotalRequestQty(), rm.getUseStockQty(), rm.getOrderQty(),
                rm.getAmount(), rm.getPrice(), rm.getCurrency(),
                rm.getGoodType(), rm.getSupplierName(),
                rm.getCreatedDate(), rm.getUpdatedDate(),
                rm.getFullDescription(), rm.getReason(), rm.getRemark(),
                rm.getRemarkComparison(), rm.getImageUrls(), rm.getType()
        );
    }
}