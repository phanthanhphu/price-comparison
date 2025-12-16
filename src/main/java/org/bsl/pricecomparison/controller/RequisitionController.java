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
            @RequestParam(defaultValue = "createdDate,desc") String sort
    ) {

        // === SORT: chỉ cho phép createdDate / updatedDate ===
        String[] sortParams = sort.split(",");
        String field = sortParams[0].trim();
        String direction = sortParams.length > 1 ? sortParams[1].trim() : "desc";

        if (!"createdDate".equalsIgnoreCase(field) && !"updatedDate".equalsIgnoreCase(field)) {
            field = "createdDate";
            direction = "desc";
        }

        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            direction = "desc";
        }

        Sort sortObj = Sort.by(Sort.Direction.fromString(direction), field);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        // === hết SORT ===

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
        // Resolve name theo id (fallback sang field cache trong document)
        String p1Name = productType1Service.getNameById(rm.getProductType1Id());
        String p2Name = productType2Service.getNameById(rm.getProductType2Id());

        RequisitionMonthlyDTO dto = new RequisitionMonthlyDTO();

        dto.setId(rm.getId());
        dto.setGroupId(rm.getGroupId());

        // product types
        dto.setProductType1Id(rm.getProductType1Id());
        dto.setProductType2Id(rm.getProductType2Id());
        dto.setProductType1Name(p1Name != null ? p1Name : rm.getProductType1Name());
        dto.setProductType2Name(p2Name != null ? p2Name : rm.getProductType2Name());

        // item
        dto.setItemDescriptionEN(rm.getItemDescriptionEN());
        dto.setItemDescriptionVN(rm.getItemDescriptionVN());
        dto.setOldSAPCode(rm.getOldSAPCode());
        dto.setHanaSAPCode(rm.getHanaSAPCode());
        dto.setUnit(rm.getUnit());

        // departments
        dto.setDepartmentRequisitions(rm.getDepartmentRequisitions());

        // qty/amount
        dto.setDailyMedInventory(rm.getDailyMedInventory());
        dto.setStock(rm.getStock());
        dto.setTotalRequestQty(rm.getTotalRequestQty());
        dto.setSafeStock(rm.getSafeStock());
        dto.setUseStockQty(rm.getUseStockQty());
        dto.setOrderQty(rm.getOrderQty());
        dto.setAmount(rm.getAmount());
        dto.setPrice(rm.getPrice());

        dto.setCurrency(rm.getCurrency());
        dto.setGoodType(rm.getGoodType());

        // supplier
        dto.setSupplierId(rm.getSupplierId());
        dto.setSupplierName(rm.getSupplierName());

        // dates
        dto.setCreatedDate(rm.getCreatedDate());
        dto.setUpdatedDate(rm.getUpdatedDate());

        // audit email
        dto.setCreatedByEmail(rm.getCreatedByEmail());
        dto.setUpdatedByEmail(rm.getUpdatedByEmail());
        dto.setCompletedByEmail(rm.getCompletedByEmail());
        dto.setUncompletedByEmail(rm.getUncompletedByEmail());

        // completion
        dto.setCompletedDate(rm.getCompletedDate());
        dto.setIsCompleted(rm.getIsCompleted()); // null-safe handled trong setter DTO nếu bạn set như mình đã gợi ý

        // extra
        dto.setImageUrls(rm.getImageUrls());
        dto.setFullDescription(rm.getFullDescription());
        dto.setReason(rm.getReason());
        dto.setRemark(rm.getRemark());
        dto.setRemarkComparison(rm.getRemarkComparison());

        // type + comparison
        dto.setType(rm.getType());
        dto.setSupplierComparisonList(rm.getSupplierComparisonList());
        dto.setStatusBestPrice(rm.getStatusBestPrice());

        return dto;
    }
}
