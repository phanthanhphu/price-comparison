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
    // 1) oldSAPCode
    List<RequisitionMonthly> findBySupplierIdAndOldSAPCodeAndUnitAndCurrencyAndIsCompletedTrue(
            String supplierId,
            String oldSAPCode,
            String unit,
            String currency,
            Pageable pageable
    );

    // 2) hanaSAPCode
    List<RequisitionMonthly> findBySupplierIdAndHanaSAPCodeAndUnitAndCurrencyAndIsCompletedTrue(
            String supplierId,
            String hanaSAPCode,
            String unit,
            String currency,
            Pageable pageable
    );

    // 3) itemDescriptionVN contains
    List<RequisitionMonthly> findBySupplierIdAndItemDescriptionVNContainingIgnoreCaseAndUnitAndCurrencyAndIsCompletedTrue(
            String supplierId,
            String keyword,
            String unit,
            String currency,
            Pageable pageable
    );

    // 4) itemDescriptionEN contains
    List<RequisitionMonthly> findBySupplierIdAndItemDescriptionENContainingIgnoreCaseAndUnitAndCurrencyAndIsCompletedTrue(
            String supplierId,
            String keyword,
            String unit,
            String currency,
            Pageable pageable
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

    // Start get Purchasing được mua mới nhất
    // ===== LATEST PURCHASE (ALL TIME) - NOT depend on previous month window =====

    // 1) Latest theo hanaSAPCode
    @Query(
            value = "{ 'isCompleted': true, 'hanaSAPCode': ?0, 'currency': ?1, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByHanaSapCodeAndCurrency(
            String hanaSapCode, String currency, Pageable pageable
    );

    // 2) Latest theo oldSAPCode
    @Query(
            value = "{ 'isCompleted': true, 'oldSAPCode': ?0, 'currency': ?1, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByOldSapCodeAndCurrency(
            String oldSapCode, String currency, Pageable pageable
    );

    // 3) Latest theo itemDescriptionVN
    @Query(
            value = "{ 'isCompleted': true, 'itemDescriptionVN': ?0, 'currency': ?1, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByItemDescriptionVNAndCurrency(
            String itemDescriptionVN, String currency, Pageable pageable
    );

    // 4) Latest theo itemDescriptionEN
    @Query(
            value = "{ 'isCompleted': true, 'itemDescriptionEN': ?0, 'currency': ?1, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByItemDescriptionENAndCurrency(
            String itemDescriptionEN, String currency, Pageable pageable
    );



    // End

    // 1) Latest theo hanaSAPCode + unit
    @Query(
            value = "{ 'isCompleted': true, 'hanaSAPCode': ?0, 'currency': ?1, 'unit': ?2, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByHanaSapCodeAndCurrencyAndUnit(
            String hanaSapCode, String currency, String unit, Pageable pageable
    );

    // 2) Latest theo oldSAPCode + unit
    @Query(
            value = "{ 'isCompleted': true, 'oldSAPCode': ?0, 'currency': ?1, 'unit': ?2, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByOldSapCodeAndCurrencyAndUnit(
            String oldSapCode, String currency, String unit, Pageable pageable
    );

    // 3) Latest theo itemDescriptionVN + unit
    @Query(
            value = "{ 'isCompleted': true, 'itemDescriptionVN': ?0, 'currency': ?1, 'unit': ?2, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByItemDescriptionVNAndCurrencyAndUnit(
            String itemDescriptionVN, String currency, String unit, Pageable pageable
    );

    // 4) Latest theo itemDescriptionEN + unit
    @Query(
            value = "{ 'isCompleted': true, 'itemDescriptionEN': ?0, 'currency': ?1, 'unit': ?2, " +
                    "  'supplierId': { $nin: [null, ''] }, " +
                    "  'completedDate': { $ne: null } }",
            sort  = "{ 'completedDate': -1 }"
    )
    List<RequisitionMonthly> findLatestPurchaseAllTimeByItemDescriptionENAndCurrencyAndUnit(
            String itemDescriptionEN, String currency, String unit, Pageable pageable
    );


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

    Optional<RequisitionMonthly> findFirstByGroupIdAndOldSAPCodeIgnoreCaseAndSupplierId(String groupId, String oldSAPCode, String supplierId);

    Optional<RequisitionMonthly> findFirstByGroupIdAndHanaSAPCodeIgnoreCaseAndSupplierId(String groupId, String hanaSAPCode, String supplierId);

    Optional<RequisitionMonthly> findFirstByGroupIdAndItemDescriptionVNIgnoreCaseAndSupplierId(String groupId, String itemDescriptionVN, String supplierId);

    Optional<RequisitionMonthly> findFirstByGroupIdAndItemDescriptionENIgnoreCaseAndSupplierId(String groupId, String itemDescriptionEN, String supplierId);


    Optional<RequisitionMonthly>
    findFirstByGroupIdAndUnitIgnoreCaseAndOldSAPCodeIgnoreCase(
            String groupId, String unit, String oldSAPCode
    );

    Optional<RequisitionMonthly>
    findFirstByGroupIdAndUnitIgnoreCaseAndHanaSAPCodeIgnoreCase(
            String groupId, String unit, String hanaSAPCode
    );

    Optional<RequisitionMonthly>
    findFirstByGroupIdAndUnitIgnoreCaseAndItemDescriptionVNIgnoreCase(
            String groupId, String unit, String itemDescriptionVN
    );

    Optional<RequisitionMonthly>
    findFirstByGroupIdAndUnitIgnoreCaseAndItemDescriptionENIgnoreCase(
            String groupId, String unit, String itemDescriptionEN
    );

    // =============================
// PREVIOUS MONTH: SUM QTY WINDOW
// Add: unit
// =============================

    // 1) Theo oldSAPCode + currency + unit + completedDate window
    @Query("{ 'isCompleted': true, 'oldSAPCode': ?0, 'currency': ?1, 'unit': ?2, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null, $gte: ?3, $lt: ?4 } }")
    List<RequisitionMonthly> findLastPurchaseByOldSapCodeAndCurrencyAndUnit(
            String oldSapCode,
            String currency,
            String unit,
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    // 2) Theo hanaSAPCode + currency + unit + completedDate window
    @Query("{ 'isCompleted': true, 'hanaSAPCode': ?0, 'currency': ?1, 'unit': ?2, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null, $gte: ?3, $lt: ?4 } }")
    List<RequisitionMonthly> findLastPurchaseByHanaSapCodeAndCurrencyAndUnit(
            String hanaSapCode,
            String currency,
            String unit,
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    // 3) Theo itemDescriptionVN + currency + unit + completedDate window
    @Query("{ 'isCompleted': true, 'itemDescriptionVN': ?0, 'currency': ?1, 'unit': ?2, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null, $gte: ?3, $lt: ?4 } }")
    List<RequisitionMonthly> findLastPurchaseByItemDescriptionVNAndCurrencyAndUnit(
            String itemDescriptionVN,
            String currency,
            String unit,
            LocalDateTime start,
            LocalDateTime endExclusive
    );

    // 4) Theo itemDescriptionEN + currency + unit + completedDate window
    @Query("{ 'isCompleted': true, 'itemDescriptionEN': ?0, 'currency': ?1, 'unit': ?2, " +
            "'supplierId': { $nin: [null, ''] }, " +
            "'completedDate': { $ne: null, $gte: ?3, $lt: ?4 } }")
    List<RequisitionMonthly> findLastPurchaseByItemDescriptionENAndCurrencyAndUnit(
            String itemDescriptionEN,
            String currency,
            String unit,
            LocalDateTime start,
            LocalDateTime endExclusive
    );

}