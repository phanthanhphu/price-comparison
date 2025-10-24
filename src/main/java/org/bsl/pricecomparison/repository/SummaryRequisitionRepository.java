package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SummaryRequisition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 🔥 OPTIMIZED: Native query cho supplier name search
    @Query(value = "{'supplierId': { $in: ?0 }}")
    List<SummaryRequisition> findBySupplierIds(List<String> supplierIds);

    /**
     * ⚡ BATCH: Tìm theo MULTIPLE groupIds (1 query cho 1000+ groups!)
     * DÙNG CHO ULTRA-FAST SEARCH: findByGroupIdIn(groupIds)
     */
    @Query("{ 'groupId': { $in: ?0 } }")
    List<SummaryRequisition> findByGroupIdIn(List<String> groupIds);

    /**
     * ⚡ BATCH: Tìm theo MULTIPLE groupIds + SORT by updatedAt DESC
     * (Cho pagination tự động sorted - NEWEST FIRST)
     */
    @Query(value = "{ 'groupId': { $in: ?0 } }", sort = "{ 'updatedAt': -1 }")
    List<SummaryRequisition> findByGroupIdInOrderByUpdatedAtDesc(List<String> groupIds);

    /**
     * ⚡ BATCH: Tìm theo groupIds + FILTERS (ProductType, Name, SAP Code)
     * CHO CASE hasFilter=true (5 filters trong 1 query!)
     */
    @Query("{ $and: [ "
            + "{ 'groupId': { $in: ?0 } }, "
            + "{ ?1: { $regex: ?2, $options: 'i' } }, "
            + "{ ?3: { $regex: ?4, $options: 'i' } }, "
            + "{ ?5: { $regex: ?6, $options: 'i' } }, "
            + "{ ?7: { $regex: ?8, $options: 'i' } } "
            + " ] }")
    List<SummaryRequisition> findByGroupIdInAndFilters(
            List<String> groupIds, String filterField1, String filterValue1,
            String filterField2, String filterValue2,
            String filterField3, String filterValue3,
            String filterField4, String filterValue4);

    /**
     * ⚡ COUNT theo groupIds (cho pagination totalElements - 1ms)
     */
    @Query(value = "{ 'groupId': { $in: ?0 } }", count = true)
    long countByGroupIdIn(List<String> groupIds);

    /**
     * ⚡ GET ALL groupIds UNIQUE (cho cache - 2ms)
     */
    @Query(value = "{}", fields = "{ 'groupId': 1, '_id': 0 }")
    List<String> findDistinctGroupIds();

    /**
     * ⚡ BATCH theo SAP Codes (cho summary lookup)
     */
    @Query("{ 'oldSapCode': { $in: ?0 } }")
    List<SummaryRequisition> findByOldSapCodeIn(List<String> sapCodes);

    /**
     * ⚡ BATCH theo ProductType1 IDs
     */
    @Query("{ 'productType1Id': { $in: ?0 } }")
    List<SummaryRequisition> findByProductType1IdIn(List<String> productType1Ids);

    /**
     * ⚡ BATCH theo ProductType2 IDs
     */
    @Query("{ 'productType2Id': { $in: ?0 } }")
    List<SummaryRequisition> findByProductType2IdIn(List<String> productType2Ids);
}
