// src/main/java/org/bsl/pricecomparison/impl/RequisitionMonthlyCustomRepositoryImpl.java
package org.bsl.pricecomparison.impl;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.repository.RequisitionMonthlyCustomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RequisitionMonthlyCustomRepositoryImpl implements RequisitionMonthlyCustomRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String COLLECTION = "requisition_monthly";

    @Override
    public Page<RequisitionMonthly> searchRequisitions(
            String groupId, String type, LocalDateTime startDate, LocalDateTime endDate,
            String p1Id, String p2Id,
            String en, String vn, String oldSap, String hanaSap,
            String sup, String dept, Pageable pageable) {

        List<AggregationOperation> stages = buildMatchStages(
                groupId, type, startDate, endDate,
                p1Id, p2Id, en, vn, oldSap, hanaSap, sup, dept);

        long total = mongoTemplate.aggregate(Aggregation.newAggregation(stages), COLLECTION, RequisitionMonthly.class)
                .getMappedResults().size();

        List<AggregationOperation> pageStages = new ArrayList<>(stages);

        if (pageable.getSort().isSorted()) {
            List<Sort.Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> orders.add(new Sort.Order(order.getDirection(), order.getProperty())));
            pageStages.add(Aggregation.sort(Sort.by(orders)));
        } else {
            pageStages.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdDate")));
        }

        pageStages.add(Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize()));
        pageStages.add(Aggregation.limit(pageable.getPageSize()));

        List<RequisitionMonthly> content = mongoTemplate.aggregate(
                Aggregation.newAggregation(pageStages), COLLECTION, RequisitionMonthly.class)
                .getMappedResults();

        return new PageImpl<>(content, pageable, total);
    }

    private List<AggregationOperation> buildMatchStages(
            String groupId, String type, LocalDateTime startDate, LocalDateTime endDate,
            String p1Id, String p2Id,
            String en, String vn, String oldSap, String hanaSap,
            String sup, String dept) {

        List<Criteria> criteriaList = new ArrayList<>();

        if (groupId != null && !groupId.isBlank()) criteriaList.add(Criteria.where("groupId").is(groupId));
        if (type != null && !type.isBlank()) criteriaList.add(Criteria.where("type").is(type));

        if (startDate != null || endDate != null) {
            Criteria dateCriteria = Criteria.where("createdDate");
            if (startDate != null) dateCriteria = dateCriteria.gte(startDate);
            if (endDate != null) dateCriteria = dateCriteria.lte(endDate);
            criteriaList.add(dateCriteria);
        }

        if (p1Id != null && !p1Id.isBlank()) criteriaList.add(Criteria.where("productType1Id").is(p1Id));
        if (p2Id != null && !p2Id.isBlank()) criteriaList.add(Criteria.where("productType2Id").is(p2Id));

        if (en != null && !en.isBlank()) criteriaList.add(Criteria.where("itemDescriptionEN").regex(en, "i"));
        if (vn != null && !vn.isBlank()) criteriaList.add(Criteria.where("itemDescriptionVN").regex(vn, "i"));
        if (oldSap != null && !oldSap.isBlank()) criteriaList.add(Criteria.where("oldSAPCode").regex(oldSap, "i"));
        if (hanaSap != null && !hanaSap.isBlank()) criteriaList.add(Criteria.where("hanaSAPCode").regex(hanaSap, "i"));
        if (sup != null && !sup.isBlank()) criteriaList.add(Criteria.where("supplierName").regex(sup, "i"));
        if (dept != null && !dept.isBlank()) criteriaList.add(Criteria.where("departmentRequisitions.name").regex(dept, "i"));

        List<AggregationOperation> stages = new ArrayList<>();
        if (!criteriaList.isEmpty()) {
            stages.add(Aggregation.match(new Criteria().andOperator(criteriaList.toArray(new Criteria[0]))));
        } else {
            stages.add(Aggregation.match(new Criteria()));
        }
        return stages;
    }
}