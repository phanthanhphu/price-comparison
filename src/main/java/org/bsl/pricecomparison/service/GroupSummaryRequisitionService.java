package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.bsl.pricecomparison.model.SummaryRequisition;
import org.bsl.pricecomparison.repository.GroupSummaryRequisitionRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GroupSummaryRequisitionService {

    @Autowired
    private GroupSummaryRequisitionRepository groupRepository;

    @Autowired
    private GroupSummaryRequisitionRepository groupSummaryRequisitionRepository;

    @Autowired
    private SummaryRequisitionRepository summaryRequisitionRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public GroupSummaryRequisition createGroupSummaryRequisition(GroupSummaryRequisition groupSummaryRequisition) {
        // Check for duplicate name and createdDate (date part only)
        LocalDate createdDate = groupSummaryRequisition.getCreatedDate() != null
                ? groupSummaryRequisition.getCreatedDate().toLocalDate()
                : LocalDate.now();
        boolean existsByNameAndDate = existsByNameAndCreatedDate(groupSummaryRequisition.getName(), createdDate);

        if (existsByNameAndDate) {
            throw new IllegalArgumentException("Group with name '" + groupSummaryRequisition.getName() + "' and created date '" + createdDate + "' already exists");
        }

        // Existing duplicate name check
        boolean existsByName = groupSummaryRequisitionRepository
                .findByNameContainingIgnoreCase(groupSummaryRequisition.getName())
                .stream()
                .anyMatch(g -> g.getName().equalsIgnoreCase(groupSummaryRequisition.getName()));

        if (existsByName) {
            throw new IllegalArgumentException("Group with name '" + groupSummaryRequisition.getName() + "' already exists");
        }

        return groupSummaryRequisitionRepository.save(groupSummaryRequisition);
    }

    public boolean existsByNameAndCreatedDate(String name, LocalDate createdDate) {
        LocalDateTime startOfDay = createdDate.atStartOfDay();
        LocalDateTime endOfDay = createdDate.atTime(23, 59, 59, 999999999);

        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        query.addCriteria(Criteria.where("createdDate").gte(startOfDay).lte(endOfDay));

        return mongoTemplate.exists(query, GroupSummaryRequisition.class);
    }

    public Optional<GroupSummaryRequisition> updateGroupSummaryRequisition(String id, GroupSummaryRequisition groupSummaryRequisition) {
        Optional<GroupSummaryRequisition> existingGroup = groupSummaryRequisitionRepository.findById(id);
        if (existingGroup.isPresent()) {
            GroupSummaryRequisition updatedGroup = existingGroup.get();

            updatedGroup.setName(groupSummaryRequisition.getName());
            updatedGroup.setType(groupSummaryRequisition.getType());
            updatedGroup.setStatus(groupSummaryRequisition.getStatus());
            updatedGroup.setCreatedBy(groupSummaryRequisition.getCreatedBy());
            updatedGroup.setCreatedDate(groupSummaryRequisition.getCreatedDate());
            updatedGroup.setStockDate(groupSummaryRequisition.getStockDate());

            return Optional.of(groupSummaryRequisitionRepository.save(updatedGroup));
        }
        return Optional.empty();
    }

    public boolean deleteGroupSummaryRequisition(String id) {
        if (!groupRepository.existsById(id)) {
            return false;
        }

        List<SummaryRequisition> requisitions = summaryRequisitionRepository.findByGroupId(id);
        summaryRequisitionRepository.deleteAll(requisitions);

        groupRepository.deleteById(id);

        return true;
    }

    public Optional<GroupSummaryRequisition> getGroupSummaryRequisitionById(String id) {
        return groupSummaryRequisitionRepository.findById(id);
    }

    public List<GroupSummaryRequisition> getAllGroupSummaryRequisitions() {
        return groupSummaryRequisitionRepository.findAll();
    }

    public Page<GroupSummaryRequisition> getAllGroupSummaryRequisitions(Pageable pageable) {
        return groupSummaryRequisitionRepository.findAll(pageable);
    }

    public List<GroupSummaryRequisition> searchGroupSummaryRequisitionsByName(String name) {
        return groupSummaryRequisitionRepository.findByNameContainingIgnoreCase(name);
    }

    public Page<GroupSummaryRequisition> filterGroupSummaryRequisitions(
            String name,
            String status,
            String createdBy,
            String type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime stockStartDate,
            LocalDateTime stockEndDate,
            Pageable pageable) {

        Query query = new Query().with(pageable);

        if (name != null && !name.trim().isEmpty()) {
            query.addCriteria(Criteria.where("name").regex("(?i).*" + name.trim() + ".*"));
        }

        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("status").regex("(?i).*" + status.trim() + ".*"));
        }

        if (createdBy != null && !createdBy.trim().isEmpty()) {
            query.addCriteria(Criteria.where("createdBy").regex("(?i).*" + createdBy.trim() + ".*"));
        }

        if (type != null && !type.trim().isEmpty()) {
            query.addCriteria(Criteria.where("type").regex("(?i).*" + type.trim() + ".*"));
        }

        if (startDate != null || endDate != null) {
            LocalDateTime startOfDay = (startDate != null)
                    ? startDate.withHour(0).withMinute(0).withSecond(0).withNano(0)
                    : null;
            LocalDateTime endOfDay = (endDate != null)
                    ? endDate.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)
                    : null;

            Criteria dateCriteria = Criteria.where("createdDate");
            if (startOfDay != null && endOfDay != null) {
                dateCriteria = dateCriteria.gte(startOfDay).lte(endOfDay);
            } else if (startOfDay != null) {
                dateCriteria = dateCriteria.gte(startOfDay);
            } else if (endOfDay != null) {
                dateCriteria = dateCriteria.lte(endOfDay);
            }

            query.addCriteria(dateCriteria);
        }

        if (stockStartDate != null || stockEndDate != null) {
            LocalDateTime stockStartOfDay = (stockStartDate != null)
                    ? stockStartDate.withHour(0).withMinute(0).withSecond(0).withNano(0)
                    : null;
            LocalDateTime stockEndOfDay = (stockEndDate != null)
                    ? stockEndDate.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)
                    : null;

            Criteria stockDateCriteria = Criteria.where("stockDate");
            if (stockStartOfDay != null && stockEndOfDay != null) {
                stockDateCriteria = stockDateCriteria.gte(stockStartOfDay).lte(stockEndOfDay);
            } else if (stockStartOfDay != null) {
                stockDateCriteria = stockDateCriteria.gte(stockStartOfDay);
            } else if (stockEndOfDay != null) {
                stockDateCriteria = stockDateCriteria.lte(stockEndOfDay);
            }

            query.addCriteria(stockDateCriteria);
        }

        List<GroupSummaryRequisition> results = mongoTemplate.find(query, GroupSummaryRequisition.class);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), GroupSummaryRequisition.class);

        return new PageImpl<>(results, pageable, total);
    }
}