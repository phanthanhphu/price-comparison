package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.dto.UpdateRequisitionMonthlyDTO;
import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // 1. Theo hanaSAPCode
    @Query("{ 'isCompleted': true, 'hanaSAPCode': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $gte: ?2, $lt: ?3 } }")
    List<RequisitionMonthly> findLastPurchaseByHanaSapCodeAndCurrency(
            String hanaSapCode, String currency, LocalDateTime start, LocalDateTime endExclusive);

    // 2. Theo oldSAPCode
    @Query("{ 'isCompleted': true, 'oldSAPCode': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $gte: ?2, $lt: ?3 } }")
    List<RequisitionMonthly> findLastPurchaseByOldSapCodeAndCurrency(
            String oldSapCode, String currency, LocalDateTime start, LocalDateTime endExclusive);

    // 3. Theo itemDescriptionVN
    @Query("{ 'isCompleted': true, 'itemDescriptionVN': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $gte: ?2, $lt: ?3 } }")
    List<RequisitionMonthly> findLastPurchaseByItemDescriptionVNAndCurrency(
            String itemDescriptionVN, String currency, LocalDateTime start, LocalDateTime endExclusive);

    // 4. Theo itemDescriptionEN
    @Query("{ 'isCompleted': true, 'itemDescriptionEN': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $gte: ?2, $lt: ?3 } }")
    List<RequisitionMonthly> findLastPurchaseByItemDescriptionENAndCurrency(
            String itemDescriptionEN, String currency, LocalDateTime start, LocalDateTime endExclusive);


    // SUMMARY
    @Query(value = "{ 'isCompleted': true, 'oldSAPCode': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null }, " +
            "'_id': { $ne: ?2 } }")
    List<RequisitionMonthly> findLatestPurchaseByOldSapCodeAndCurrency(
            String oldSapCode, String currency, String idNot, Pageable pageable);

    @Query(value = "{ 'isCompleted': true, 'hanaSAPCode': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null }, " +
            "'_id': { $ne: ?2 } }")
    List<RequisitionMonthly> findLatestPurchaseByHanaSapCodeAndCurrency(
            String hanaSapCode, String currency, String idNot, Pageable pageable);

    @Query(value = "{ 'isCompleted': true, 'itemDescriptionVN': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null }, " +
            "'_id': { $ne: ?2 } }")
    List<RequisitionMonthly> findLatestPurchaseByItemDescriptionVNAndCurrency(
            String itemDescriptionVN, String currency, String idNot, Pageable pageable);

    @Query(value = "{ 'isCompleted': true, 'itemDescriptionEN': ?0, 'currency': ?1, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null }, " +
            "'_id': { $ne: ?2 } }")
    List<RequisitionMonthly> findLatestPurchaseByItemDescriptionENAndCurrency(
            String itemDescriptionEN, String currency, String idNot, Pageable pageable);

    List<RequisitionMonthly> findAllByGroupId(String groupId);


}