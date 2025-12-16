// File: GroupedByTypeComparisonResponseDTO.java
package org.bsl.pricecomparison.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Response nhóm theo Type1 → bên trong có nhiều Type2")
public record GroupedByTypeComparisonResponseDTO(
        List<Type1Group> groups,
        GrandTotal grandTotal
) {
    public record Type1Group(
            String type1,
            String type1Name,

            // Tổng của cả Type1 (tổng tất cả Type2 con)
            GroupTotal total,

            // Các nhóm con Type2
            List<Type2Subgroup> subgroups
    ) {}

    public record Type2Subgroup(
            String type2,
            String type2Name,

            // Tổng riêng của Type2 này
            GroupTotal total,

            // Danh sách chi tiết các requisition thuộc Type2 này
            List<MonthlyComparisonRequisitionDTO> requisitions
    ) {}

    public record GroupTotal(
            BigDecimal totalBuyQty,         // Tổng số lượng được duyệt mua (từ tất cả department)
            BigDecimal totalAmount,         // Tổng tiền thực tế
            BigDecimal totalAmtDifference,  // Tổng chênh lệch
            BigDecimal totalPercentage      // % chênh lệch
    ) {}

    public record GrandTotal(
            BigDecimal totalAmount,
            BigDecimal totalAmtDifference,
            BigDecimal totalPercentage
    ) {}
}