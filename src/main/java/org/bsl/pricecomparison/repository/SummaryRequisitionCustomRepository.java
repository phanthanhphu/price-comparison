//package org.bsl.pricecomparison.repository;
//
//import org.bsl.pricecomparison.model.SummaryRequisition;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface SummaryRequisitionCustomRepository extends JpaRepository<SummaryRequisition, String> {
//
//    /**
//     * Custom query với JOIN FETCH để tránh N+1 problem
//     */
//    @Query("SELECT DISTINCT sr FROM SummaryRequisition sr " +
//            "LEFT JOIN FETCH sr.departmentRequestQty " +
//            "WHERE sr.groupId IN :groupIds " +
//            "AND (:productType1Id IS NULL OR sr.productType1Id = :productType1Id) " +
//            "AND (:productType2Id IS NULL OR sr.productType2Id = :productType2Id) " +
//            "AND (:oldSapCode IS NULL OR LOWER(sr.oldSapCode) LIKE LOWER(CONCAT('%', :oldSapCode, '%'))) " +
//            "AND (:hanaSapCode IS NULL OR LOWER(sr.hanaSapCode) LIKE LOWER(CONCAT('%', :hanaSapCode, '%')))")
//    List<SummaryRequisition> findByGroupIdsWithFilter(
//            @Param("groupIds") List<String> groupIds,
//            @Param("productType1Id") String productType1Id,
//            @Param("productType2Id") String productType2Id,
//            @Param("oldSapCode") String oldSapCode,
//            @Param("hanaSapCode") String hanaSapCode);
//
//    /**
//     * Paginated query với filters
//     */
//    @Query("SELECT sr FROM SummaryRequisition sr " +
//            "WHERE sr.groupId IN :groupIds " +
//            "ORDER BY COALESCE(sr.updatedAt, sr.createdAt) DESC")
//    Page<SummaryRequisition> findByGroupIdsPaginated(
//            @Param("groupIds") List<String> groupIds,
//            Pageable pageable);
//}