package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/group-summary-requisitions")
public class GroupSummaryRequisitionController {

    @Autowired
    private GroupSummaryRequisitionService groupSummaryRequisitionService;

    @PostMapping
    public ResponseEntity<GroupSummaryRequisition> createGroupSummaryRequisition(@RequestBody GroupSummaryRequisition groupSummaryRequisition) {
        if (groupSummaryRequisition.getCreatedDate() == null) {
            groupSummaryRequisition.setCreatedDate(LocalDateTime.now());
        }
        GroupSummaryRequisition savedGroup = groupSummaryRequisitionService.createGroupSummaryRequisition(groupSummaryRequisition);
        return ResponseEntity.ok(savedGroup);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupSummaryRequisition> updateGroupSummaryRequisition(
            @PathVariable String id,
            @RequestBody GroupSummaryRequisition groupSummaryRequisition) {
        Optional<GroupSummaryRequisition> existingGroup = groupSummaryRequisitionService.getGroupSummaryRequisitionById(id);

        if (groupSummaryRequisition.getCreatedDate() == null) {
            groupSummaryRequisition.setCreatedDate(LocalDateTime.now());
        }

        if (!existingGroup.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Optional<GroupSummaryRequisition> updatedGroup = groupSummaryRequisitionService.updateGroupSummaryRequisition(id, groupSummaryRequisition);

        return updatedGroup.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupSummaryRequisition(@PathVariable String id) {
        boolean isDeleted = groupSummaryRequisitionService.deleteGroupSummaryRequisition(id);
        return isDeleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupSummaryRequisition> getGroupSummaryRequisition(@PathVariable String id) {
        Optional<GroupSummaryRequisition> group = groupSummaryRequisitionService.getGroupSummaryRequisitionById(id);
        return group.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<GroupSummaryRequisition>> getAllGroupSummaryRequisitions() {
        List<GroupSummaryRequisition> groups = groupSummaryRequisitionService.getAllGroupSummaryRequisitions();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GroupSummaryRequisition>> searchGroupSummaryRequisitions(
            @RequestParam String name) {
        List<GroupSummaryRequisition> groups = groupSummaryRequisitionService.searchGroupSummaryRequisitionsByName(name);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<GroupSummaryRequisition>> getAllGroupSummaryRequisitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("createdDate"))); // Sort theo createdDate giảm dần
        Page<GroupSummaryRequisition> groupsPage = groupSummaryRequisitionService.getAllGroupSummaryRequisitions(pageable);
        return ResponseEntity.ok(groupsPage);
    }

}
