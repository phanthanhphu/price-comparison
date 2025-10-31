// src/main/java/org/bsl/pricecomparison/repository/RequisitionMonthlyCustomRepository.java
package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface RequisitionMonthlyCustomRepository {

    Page<RequisitionMonthly> searchRequisitions(
            String groupId,
            String type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productType1Id,
            String productType2Id,
            String englishName,
            String vietnameseName,
            String oldSapCode,
            String hanaSapCode,
            String supplierName,
            String departmentName,
            Pageable pageable);
}