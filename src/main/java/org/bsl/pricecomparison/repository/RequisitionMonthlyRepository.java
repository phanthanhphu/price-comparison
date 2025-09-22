package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RequisitionMonthlyRepository extends MongoRepository<RequisitionMonthly, Integer> {
    Optional<RequisitionMonthly> findByProductType1IdAndProductType2IdAndOldSAPCode(
            Integer productType1Id, Integer productType2Id, String oldSAPCode);

    List<RequisitionMonthly> findByGroupId(String groupId);
}