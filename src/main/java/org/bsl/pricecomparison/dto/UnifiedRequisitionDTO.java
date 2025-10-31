package org.bsl.pricecomparison.dto;

import java.math.BigDecimal;
import java.util.List;

public record UnifiedRequisitionDTO(
        String id,
        String groupId,
        String englishName,
        String vietnameseName,
        String oldSapCode,
        String hanaSapCode,
        String supplierName,
        List<DepartmentRequestDTO> departmentRequests,
        int sumBuy,
        BigDecimal totalPrice,
        String productType1Name,
        String productType2Name,
        String createdDate,
        String updatedDate,
        String dataSource // "summary" | "monthly"
) {
    public record DepartmentRequestDTO(
            String departmentId,
            String departmentName,
            int qty,
            int buy
    ) {}
}