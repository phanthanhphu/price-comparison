package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SummaryRequisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SummaryRequisitionRepository extends MongoRepository<SummaryRequisition, String> {

    List<SummaryRequisition> findByGroupId(String groupId);

    Page<SummaryRequisition> findByGroupId(String groupId, Pageable pageable);

    Optional<SummaryRequisition> findByProductType1IdAndProductType2IdAndOldSapCode(
            String productType1Id,
            String productType2Id,
            String oldSapCode
    );

    Optional<SummaryRequisition> findByGroupIdAndOldSapCode(String groupId, String oldSapCode);

    @Query(value = "{ 'departmentRequestQty.?0': { $exists: true } }", exists = true)
    boolean existsByDepartmentRequestQtyKey(String departmentId);

    @Query(value = "{ 'supplierId': ?0 }", exists = true)
    boolean existsBySupplierId(String supplierId);

    boolean existsByGroupId(String groupId);
}
