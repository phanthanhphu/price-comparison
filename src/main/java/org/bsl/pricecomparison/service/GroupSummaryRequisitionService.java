package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.GroupSummaryRequisition;
import org.bsl.pricecomparison.model.SummaryRequisition;
import org.bsl.pricecomparison.repository.GroupSummaryRequisitionRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

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

    public GroupSummaryRequisition createGroupSummaryRequisition(GroupSummaryRequisition groupSummaryRequisition) {
        boolean exists = groupSummaryRequisitionRepository
                .findByNameContainingIgnoreCase(groupSummaryRequisition.getName())
                .stream()
                .anyMatch(g -> g.getName().equalsIgnoreCase(groupSummaryRequisition.getName()));

        if (exists) {
            throw new IllegalArgumentException("Group with name '" + groupSummaryRequisition.getName() + "' already exists");
        }

        return groupSummaryRequisitionRepository.save(groupSummaryRequisition);
    }


    public Optional<GroupSummaryRequisition> updateGroupSummaryRequisition(String id, GroupSummaryRequisition groupSummaryRequisition) {
        Optional<GroupSummaryRequisition> existingGroup = groupSummaryRequisitionRepository.findById(id);
        if (existingGroup.isPresent()) {
            GroupSummaryRequisition updatedGroup = existingGroup.get();

            updatedGroup.setName(groupSummaryRequisition.getName());
            updatedGroup.setType(groupSummaryRequisition.getType());
            updatedGroup.setStatus(groupSummaryRequisition.getStatus());
            updatedGroup.setCreatedBy(groupSummaryRequisition.getCreatedBy());
            updatedGroup.setCreatedDate(groupSummaryRequisition.getCreatedDate());  // Nếu ngày được set trong controller

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
}
