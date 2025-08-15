package org.bsl.pricecomparison.impl;

import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class GroupSummaryRequisitionServiceImpl implements GroupSummaryRequisitionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<GroupSummaryRequisition> filterGroupSummaryRequisitions(
            String name,
            String status,
            String createdBy,
            String fromDate,
            String toDate,
            int page,
            int size) {

        Query query = new Query();

        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
        }

        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (createdBy != null && !createdBy.isEmpty()) {
            query.addCriteria(Criteria.where("createdBy").regex(createdBy, "i"));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        if (fromDate != null && !fromDate.isEmpty()) {
            LocalDateTime from = LocalDateTime.parse(fromDate, formatter);
            query.addCriteria(Criteria.where("createdDate").gte(from));
        }

        if (toDate != null && !toDate.isEmpty()) {
            LocalDateTime to = LocalDateTime.parse(toDate, formatter);
            query.addCriteria(Criteria.where("createdDate").lte(to));
        }

        long total = mongoTemplate.count(query, GroupSummaryRequisition.class);

        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));
        List<GroupSummaryRequisition> results = mongoTemplate.find(query, GroupSummaryRequisition.class);

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }
}
