package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SummaryRequisition;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SummaryRequisitionRepository extends MongoRepository<SummaryRequisition, String> {

    List<SummaryRequisition> findByGroupId(String groupId);
}
