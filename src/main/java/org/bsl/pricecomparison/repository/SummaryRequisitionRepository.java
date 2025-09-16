package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SummaryRequisition;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SummaryRequisitionRepository extends MongoRepository<SummaryRequisition, String> {

    List<SummaryRequisition> findByGroupId(String groupId);

    Optional<SummaryRequisition> findByProductType1IdAndProductType2IdAndOldSapCode(
            String productType1Id,
            String productType2Id,
            String oldSapCode
    );
}
