package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.dto.UpdateRequisitionMonthlyDTO;
import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RequisitionMonthlyRepository extends MongoRepository<RequisitionMonthly, String> {
    List<RequisitionMonthly> findByGroupId(String groupId);

    List<UpdateRequisitionMonthlyDTO> findRequestByGroupId(String groupId);

    Optional<RequisitionMonthly> findByGroupIdAndOldSAPCode(String groupId, String oldSAPCode);

    @Query(value = "{ 'departmentRequisitions.id': ?0 }", exists = true)
    boolean existsByDepartmentRequisitionsId(String departmentId);

    @Query(value = "{ 'supplierId': ?0 }", exists = true)
    boolean existsBySupplierId(String supplierId);

    boolean existsByGroupId(String groupId);
}