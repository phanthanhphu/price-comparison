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

    Optional<RequisitionMonthly>
    findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByCompletedDateDesc(
            String supplierId, String oldSAPCode, String currency, String idNot
    );

    Optional<RequisitionMonthly>
    findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByUpdatedDateDesc(
            String supplierId, String oldSAPCode, String currency, String idNot
    );

    // ===================== ✅ LAST PURCHASE (HANA SAP) ====================
    Optional<RequisitionMonthly>
    findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByCompletedDateDesc(
            String supplierId, String hanaSAPCode, String currency, String idNot
    );

    Optional<RequisitionMonthly>
    findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByUpdatedDateDesc(
            String supplierId, String hanaSAPCode, String currency, String idNot
    );

    //Supplier
    Optional<RequisitionMonthly> findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueOrderByCompletedDateDesc(
            String supplierId, String oldSAPCode, String currency
    );

    Optional<RequisitionMonthly> findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueOrderByUpdatedDateDesc(
            String supplierId, String oldSAPCode, String currency
    );

    Optional<RequisitionMonthly> findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueOrderByCompletedDateDesc(
            String supplierId, String hanaSAPCode, String currency
    );

    Optional<RequisitionMonthly> findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueOrderByUpdatedDateDesc(
            String supplierId, String hanaSAPCode, String currency
    );
}