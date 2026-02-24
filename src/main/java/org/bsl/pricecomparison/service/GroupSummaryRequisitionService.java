package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.bsl.pricecomparison.model.SummaryRequisition;
import org.bsl.pricecomparison.model.User;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupSummaryRequisitionService {

    @Autowired
    private GroupSummaryRequisitionRepository groupRepository;

    @Autowired
    private GroupSummaryRequisitionRepository groupSummaryRequisitionRepository;

    @Autowired
    private SummaryRequisitionRepository summaryRequisitionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public GroupSummaryRequisition createGroupSummaryRequisition(GroupSummaryRequisition groupSummaryRequisition) {
        // Validate input
        if (groupSummaryRequisition == null || groupSummaryRequisition.getName() == null || groupSummaryRequisition.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (groupSummaryRequisition.getType() == null || groupSummaryRequisition.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type cannot be empty");
        }
        // Set createdDate if not provided
        LocalDate createdDate = groupSummaryRequisition.getCreatedDate() != null
                ? groupSummaryRequisition.getCreatedDate().toLocalDate()
                : LocalDate.now();
        // Check for duplicate name, type, and createdDate
        boolean existsByNameTypeAndDate = existsByNameTypeAndCreatedDate(
                groupSummaryRequisition.getName(), groupSummaryRequisition.getType(), createdDate);
        if (existsByNameTypeAndDate) {
            throw new IllegalArgumentException("Group summary requisition with this name, type, and created date already exists");
        }
        // Set createdDate and stockDate if not provided
        if (groupSummaryRequisition.getCreatedDate() == null) {
            groupSummaryRequisition.setCreatedDate(LocalDateTime.now());
        }
        if (groupSummaryRequisition.getStockDate() == null) {
            groupSummaryRequisition.setStockDate(LocalDateTime.now());
        }
        return groupSummaryRequisitionRepository.save(groupSummaryRequisition);
    }

    public boolean existsByNameTypeAndCreatedDate(String name, String type, LocalDate createdDate) {
        LocalDateTime startOfDay = createdDate.atStartOfDay();
        LocalDateTime endOfDay = createdDate.atTime(23, 59, 59, 999999999);

        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        query.addCriteria(Criteria.where("type").is(type));
        query.addCriteria(Criteria.where("createdDate").gte(startOfDay).lte(endOfDay));

        return mongoTemplate.exists(query, GroupSummaryRequisition.class);
    }

    public Optional<GroupSummaryRequisition> updateGroupSummaryRequisition(String id, GroupSummaryRequisition groupSummaryRequisition) {
        // Validate input
        if (groupSummaryRequisition == null || groupSummaryRequisition.getName() == null || groupSummaryRequisition.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (groupSummaryRequisition.getType() == null || groupSummaryRequisition.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Type cannot be empty");
        }
        // Validate currency
        String currency = groupSummaryRequisition.getCurrency();
        if (currency != null && !currency.trim().isEmpty()) {
            currency = currency.toUpperCase();
            if (!List.of("VND", "EURO", "USD").contains(currency)) {
                throw new IllegalArgumentException("Invalid currency. Must be VND, EURO, or USD.");
            }
        } else {
            currency = null; // Retain existing currency if not provided
        }
        // Check if group exists
        Optional<GroupSummaryRequisition> existingGroup = groupSummaryRequisitionRepository.findById(id);
        if (!existingGroup.isPresent()) {
            return Optional.empty();
        }
        // Check for duplicate name, type, and createdDate (excluding current id)
        LocalDate createdDate = groupSummaryRequisition.getCreatedDate() != null
                ? groupSummaryRequisition.getCreatedDate().toLocalDate()
                : existingGroup.get().getCreatedDate().toLocalDate();
        GroupSummaryRequisition duplicate = findByNameTypeAndCreatedDateAndIdNot(
                groupSummaryRequisition.getName(), groupSummaryRequisition.getType(), createdDate, id);
        if (duplicate != null) {
            throw new IllegalArgumentException("Group summary requisition with name '" + groupSummaryRequisition.getName() +
                    "', type '" + groupSummaryRequisition.getType() + "', and created date '" + createdDate + "' already exists");
        }
        // Validate status transition
        String currentStatus = existingGroup.get().getStatus();
        String newStatus = groupSummaryRequisition.getStatus();
        if (currentStatus == null) {
            throw new IllegalArgumentException("Current status is null for group with id '" + id + "'");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from '" + currentStatus + "' to '" + newStatus + "'");
        }
        // Update fields
        GroupSummaryRequisition updatedGroup = existingGroup.get();
        updatedGroup.setName(groupSummaryRequisition.getName());
        updatedGroup.setType(groupSummaryRequisition.getType());
        updatedGroup.setStatus(newStatus);
        updatedGroup.setCreatedBy(groupSummaryRequisition.getCreatedBy());
        updatedGroup.setCreatedDate(groupSummaryRequisition.getCreatedDate() != null
                ? groupSummaryRequisition.getCreatedDate()
                : existingGroup.get().getCreatedDate());
        updatedGroup.setStockDate(groupSummaryRequisition.getStockDate() != null
                ? groupSummaryRequisition.getStockDate()
                : existingGroup.get().getStockDate());
        updatedGroup.setCurrency(currency != null
                ? currency
                : existingGroup.get().getCurrency()); // Update currency or retain existing

        return Optional.of(groupSummaryRequisitionRepository.save(updatedGroup));
    }


    public Optional<GroupSummaryRequisition> updateStatusOnly(
            String id,
            String newStatus,
            String userId) {

        GroupSummaryRequisition group = groupSummaryRequisitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Group not found with ID: " + id));

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("New status cannot be empty");
        }
        String trimmedStatus = newStatus.trim();

        // Chỉ ADMIN mới được quyền
        boolean isAdmin = false;
        if (userId != null && !userId.isBlank()) {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isPresent()) {
                String role = userOpt.get().getRole();
                isAdmin = "ADMIN".equalsIgnoreCase(role);
            }
        }

        if (group.getStatus().equalsIgnoreCase("Completed") && !isAdmin) {
            throw new IllegalArgumentException("This request has already been completed. If you need to change the status, please contact the administrator."
            );
        }


        group.setStatus(trimmedStatus);
        GroupSummaryRequisition saved = groupSummaryRequisitionRepository.save(group);
        return Optional.of(saved);
    }

    public GroupSummaryRequisition findByNameTypeAndCreatedDateAndIdNot(String name, String type, LocalDate createdDate, String id) {
        LocalDateTime startOfDay = createdDate.atStartOfDay();
        LocalDateTime endOfDay = createdDate.atTime(23, 59, 59, 999999999);

        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(name));
        query.addCriteria(Criteria.where("type").is(type));
        query.addCriteria(Criteria.where("createdDate").gte(startOfDay).lte(endOfDay));
        query.addCriteria(Criteria.where("id").ne(id));

        return mongoTemplate.findOne(query, GroupSummaryRequisition.class);
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
            String currency, // Added currency parameter
            LocalDate startDate,
            LocalDate endDate,
            LocalDate stockStartDate,
            LocalDate stockEndDate,
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

        if (currency != null && !currency.trim().isEmpty()) {
            query.addCriteria(Criteria.where("currency").is(currency.trim().toUpperCase()));
        }

        if (startDate != null || endDate != null) {
            LocalDateTime startOfDay = (startDate != null)
                    ? startDate.atStartOfDay()
                    : null;
            LocalDateTime endOfDay = (endDate != null)
                    ? endDate.atTime(23, 59, 59, 999_999_999)
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
                    ? stockStartDate.atStartOfDay()
                    : null;
            LocalDateTime stockEndOfDay = (stockEndDate != null)
                    ? stockEndDate.atTime(23, 59, 59, 999_999_999)
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

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        switch (currentStatus) {
            case "Not Started":
                return newStatus.equals("Not Started") || newStatus.equals("In Progress");
            case "In Progress":
                return newStatus.equals("In Progress") || newStatus.equals("Completed");
            case "Completed":
                return newStatus.equals("Completed");
            default:
                return false;
        }
    }

    public List<String> getFilteredGroupIds(String name, String type, String status, String currency) {
        Query query = new Query();

        if (name != null && !name.trim().isEmpty()) {
            query.addCriteria(Criteria.where("name").regex("(?i).*" + name.trim() + ".*"));
        }

        if (type != null && !type.trim().isEmpty()) {
            query.addCriteria(Criteria.where("type").regex("(?i).*" + type.trim() + ".*"));
        }

        if (status != null && !status.trim().isEmpty()) {
            query.addCriteria(Criteria.where("status").regex("(?i).*" + status.trim() + ".*"));
        }

        if (currency != null && !currency.trim().isEmpty()) {
            query.addCriteria(Criteria.where("currency").is(currency.trim().toUpperCase()));
        }

        return mongoTemplate.find(query, GroupSummaryRequisition.class).stream()
                .map(GroupSummaryRequisition::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getAllGroupIds() {
        return groupSummaryRequisitionRepository.findAll().stream()
                .map(GroupSummaryRequisition::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public Map<String, String> getCurrenciesByGroupIds(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Query query = new Query(Criteria.where("_id").in(groupIds));
        query.fields().include("_id").include("currency");
        return mongoTemplate.find(query, GroupSummaryRequisition.class).stream()
                .filter(g -> g.getCurrency() != null)
                .collect(Collectors.toMap(GroupSummaryRequisition::getId, GroupSummaryRequisition::getCurrency));
    }
}