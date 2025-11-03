package org.bsl.pricecomparison.controller;

import jakarta.validation.Valid;
import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.bsl.pricecomparison.repository.RequisitionMonthlyRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.bsl.pricecomparison.request.UpdateGroupStatusRequest;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/group-summary-requisitions")
public class GroupSummaryRequisitionController {

    @Autowired
    private GroupSummaryRequisitionService groupSummaryRequisitionService;

    @Autowired
    private SummaryRequisitionRepository summaryRequisitionRepository;

    @Autowired
    private RequisitionMonthlyRepository requisitionMonthlyRepository;

    @PostMapping
    public ResponseEntity<?> createGroupSummaryRequisition(@RequestBody GroupSummaryRequisition groupSummaryRequisition) {
        try {
            // Set createdDate and stockDate if not provided
            if (groupSummaryRequisition.getCreatedDate() == null) {
                groupSummaryRequisition.setCreatedDate(LocalDateTime.now());
            }
            if (groupSummaryRequisition.getStockDate() == null) {
                groupSummaryRequisition.setStockDate(LocalDateTime.now());
            }
            // Validate currency
            if (groupSummaryRequisition.getCurrency() == null || groupSummaryRequisition.getCurrency().trim().isEmpty()) {
                groupSummaryRequisition.setCurrency("VND"); // Default to VND if not provided
            } else {
                String currency = groupSummaryRequisition.getCurrency().toUpperCase();
                if (!List.of("VND", "EURO", "USD").contains(currency)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid currency. Must be VND, EURO, or USD."));
                }
                groupSummaryRequisition.setCurrency(currency);
            }
            GroupSummaryRequisition savedGroup = groupSummaryRequisitionService.createGroupSummaryRequisition(groupSummaryRequisition);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedGroup);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create group summary requisition: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroupSummaryRequisition(
            @PathVariable String id,
            @RequestBody GroupSummaryRequisition groupSummaryRequisition) {
        try {
            Optional<GroupSummaryRequisition> existingGroup = groupSummaryRequisitionService.getGroupSummaryRequisitionById(id);
            if (!existingGroup.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Group summary requisition not found with ID: " + id));
            }
            // Set createdDate and stockDate if not provided
            if (groupSummaryRequisition.getCreatedDate() == null) {
                groupSummaryRequisition.setCreatedDate(existingGroup.get().getCreatedDate());
            }
            if (groupSummaryRequisition.getStockDate() == null) {
                groupSummaryRequisition.setStockDate(existingGroup.get().getStockDate());
            }
            // Validate currency
            if (groupSummaryRequisition.getCurrency() == null || groupSummaryRequisition.getCurrency().trim().isEmpty()) {
                groupSummaryRequisition.setCurrency(existingGroup.get().getCurrency()); // Retain existing currency
            } else {
                String currency = groupSummaryRequisition.getCurrency().toUpperCase();
                if (!List.of("VND", "EURO", "USD").contains(currency)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid currency. Must be VND, EURO, or USD."));
                }
                groupSummaryRequisition.setCurrency(currency);
            }
            Optional<GroupSummaryRequisition> updatedGroup = groupSummaryRequisitionService.updateGroupSummaryRequisition(id, groupSummaryRequisition);
            return updatedGroup.map(group -> ResponseEntity.ok(group))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((GroupSummaryRequisition) Map.of("message", "Group summary requisition not found with ID: " + id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update group summary requisition: " + e.getMessage()));
        }
    }

    @PutMapping("/status")
    public ResponseEntity<?> updateGroupStatus(
            @Valid @RequestBody UpdateGroupStatusRequest request) {

        String groupId = request.getGroupId();
        String userId = request.getUserId();
        String newStatus = request.getStatus();

        try {
            Optional<GroupSummaryRequisition> updated = groupSummaryRequisitionService
                    .updateStatusOnly(groupId, newStatus, userId);

            return updated
                    .map(g -> ResponseEntity.ok(Map.of(
                            "message", "Status updated successfully!"
                    )))
                    .orElse(ResponseEntity.notFound().build());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGroupSummaryRequisition(@PathVariable String id) {
        try {
            // Validate ID
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid group ID"));
            }

            // Check if group exists
            Optional<GroupSummaryRequisition> groupOptional = groupSummaryRequisitionService.getGroupSummaryRequisitionById(id);
            if (!groupOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Group summary requisition not found with ID: " + id));
            }

            GroupSummaryRequisition group = groupOptional.get();
            String groupName = group.getName() != null ? group.getName() : "Group ID " + id;

            // Check for dependencies
            if (summaryRequisitionRepository.existsByGroupId(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", String.format(
                                "Cannot delete group '%s' because it is referenced by urgent requisition(s).",
                                groupName)));
            }

            if (requisitionMonthlyRepository.existsByGroupId(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", String.format(
                                "Cannot delete group '%s' because it is referenced by monthly requisition(s).",
                                groupName)));
            }

            // Perform deletion
            boolean isDeleted = groupSummaryRequisitionService.deleteGroupSummaryRequisition(id);
            if (isDeleted) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(Map.of("message", String.format("Group '%s' deleted successfully", groupName)));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to delete group summary requisition"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete group summary requisition: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroupSummaryRequisition(@PathVariable String id) {
        try {
            Optional<GroupSummaryRequisition> group = groupSummaryRequisitionService.getGroupSummaryRequisitionById(id);
            return group.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body((GroupSummaryRequisition) Map.of("message", "Group summary requisition not found with ID: " + id)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch group summary requisition: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllGroupSummaryRequisitions() {
        try {
            List<GroupSummaryRequisition> groups = groupSummaryRequisitionService.getAllGroupSummaryRequisitions();
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch group summary requisitions: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchGroupSummaryRequisitions(@RequestParam String name) {
        try {
            List<GroupSummaryRequisition> groups = groupSummaryRequisitionService.searchGroupSummaryRequisitionsByName(name);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search group summary requisitions: " + e.getMessage()));
        }
    }

    @GetMapping("/page")
    public ResponseEntity<?> getAllGroupSummaryRequisitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("createdDate")));
            Page<GroupSummaryRequisition> groupsPage = groupSummaryRequisitionService.getAllGroupSummaryRequisitions(pageable);
            return ResponseEntity.ok(groupsPage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch group summary requisitions: " + e.getMessage()));
        }
    }

//    @GetMapping("/filter")
//    public ResponseEntity<?> filterGroupSummaryRequisitions(
//            @RequestParam(required = false) String name,
//            @RequestParam(required = false) String status,
//            @RequestParam(required = false) String createdBy,
//            @RequestParam(required = false) String type,
//            @RequestParam(required = false) String currency,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockStartDate,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockEndDate,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//        try {
//            // Validate currency if provided
//            if (currency != null && !currency.trim().isEmpty()) {
//                String upperCurrency = currency.toUpperCase();
//                if (!List.of("VND", "EURO", "USD").contains(upperCurrency)) {
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                            .body(Map.of("message", "Invalid currency. Must be VND, EURO, or USD."));
//                }
//                currency = upperCurrency;
//            }
//            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdDate")));
//            Page<GroupSummaryRequisition> result = groupSummaryRequisitionService
//                    .filterGroupSummaryRequisitions(name, status, createdBy, type, currency, startDate, endDate, stockStartDate, stockEndDate, pageable);
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("message", "Failed to filter group summary requisitions: " + e.getMessage()));
//        }
//    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterGroupSummaryRequisitions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockStartDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate stockEndDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Validate currency if provided
            if (currency != null && !currency.trim().isEmpty()) {
                String upperCurrency = currency.toUpperCase();
                if (!List.of("VND", "EURO", "USD").contains(upperCurrency)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid currency. Must be VND, EURO, or USD."));
                }
                currency = upperCurrency;
            }
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdDate")));
            Page<GroupSummaryRequisition> result = groupSummaryRequisitionService
                    .filterGroupSummaryRequisitions(name, status, createdBy, type, currency, startDate, endDate, stockStartDate, stockEndDate, pageable);

            // Set isUsed for each GroupSummaryRequisition, stopping after first match
            result.getContent().forEach(group -> {
                String groupId = group.getId();
                boolean isUsed = false;
                if (groupId != null) {
                    // Check SummaryRequisitionRepository first
                    if (summaryRequisitionRepository.existsByGroupId(groupId)) {
                        isUsed = true; // Stop checking after match
                    }
                    // Only check RequisitionMonthlyRepository if no match found
                    else if (requisitionMonthlyRepository.existsByGroupId(groupId)) {
                        isUsed = true; // Stop checking after match
                    }
                }
                group.setUsed(isUsed); // Set to false if no match or groupId is null
            });

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to filter group summary requisitions: " + e.getMessage()));
        }
    }
}