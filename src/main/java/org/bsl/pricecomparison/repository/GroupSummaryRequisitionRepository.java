package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GroupSummaryRequisitionRepository extends MongoRepository<GroupSummaryRequisition, String> {
    List<GroupSummaryRequisition> findByNameContainingIgnoreCase(String name);
}
