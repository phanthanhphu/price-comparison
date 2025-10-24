package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RequisitionMonthlyRepository extends MongoRepository<RequisitionMonthly, String> {
    Optional<RequisitionMonthly> findByProductType1IdAndProductType2IdAndOldSAPCode(
            Integer productType1Id, Integer productType2Id, String oldSAPCode);

    List<RequisitionMonthly> findByGroupId(String groupId);

    Optional<RequisitionMonthly> findByGroupIdAndOldSAPCode(String groupId, String oldSAPCode);

    @Query("{'groupId': ?0, 'oldSAPCode': ?1}")
    boolean existsByGroupIdAndOldSAPCode(String groupId, String oldSAPCode);

    @Query(value = "{ 'departmentRequisitions.id': ?0 }", exists = true)
    boolean existsByDepartmentRequisitionsId(String departmentId);

    @Query(value = "{ 'supplierId': ?0 }", exists = true)
    boolean existsBySupplierId(String supplierId);

    boolean existsByGroupId(String groupId);

    @Query("{ 'groupId': { $in: ?0 } }")
    List<RequisitionMonthly> findByGroupIdIn(List<String> groupIds);

    /**
     * ⚡ BATCH: Tìm theo MULTIPLE groupIds + SORT by updatedDate DESC
     * (Cho pagination tự động sorted)
     */
    @Query(value = "{ 'groupId': { $in: ?0 } }", sort = "{ 'updatedDate': -1 }")
    List<RequisitionMonthly> findByGroupIdInOrderByUpdatedDateDesc(List<String> groupIds);

    /**
     * ⚡ BATCH: Tìm theo groupIds + FILTERS (ProductType, Name, SAP Code)
     * CHO CASE hasFilter=true
     */
    @Query("{ $and: [ "
            + "{ 'groupId': { $in: ?0 } }, "
            + "{ ?1: { $regex: ?2, $options: 'i' } }, "
            + "{ ?3: { $regex: ?4, $options: 'i' } }, "
            + "{ ?5: { $regex: ?6, $options: 'i' } }, "
            + "{ ?7: { $regex: ?8, $options: 'i' } } "
            + " ] }")
    List<RequisitionMonthly> findByGroupIdInAndFilters(
            List<String> groupIds, String filterField1, String filterValue1,
            String filterField2, String filterValue2,
            String filterField3, String filterValue3,
            String filterField4, String filterValue4);

    /**
     * ⚡ COUNT theo groupIds (cho pagination totalElements)
     */
    @Query(value = "{ 'groupId': { $in: ?0 } }", count = true)
    long countByGroupIdIn(List<String> groupIds);

    /**
     * ⚡ GET ALL groupIds UNIQUE (cho cache)
     */
    @Query(value = "{}", fields = "{ 'groupId': 1, '_id': 0 }")
    List<String> findDistinctGroupIds();

    /**
     * ⚡ BATCH theo SAP Codes (cho monthly suppliers lookup)
     */
    @Query("{ 'oldSAPCode': { $in: ?0 } }")
    List<RequisitionMonthly> findByOldSAPCodeIn(List<String> sapCodes);
}