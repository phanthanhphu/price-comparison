package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.Department;
import org.bsl.pricecomparison.repository.DepartmentRepository;
import org.bsl.pricecomparison.repository.RequisitionMonthlyRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SummaryRequisitionRepository summaryRequisitionRepository;

    @Autowired
    private RequisitionMonthlyRepository requisitionMonthlyRepository;

    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        try {
            List<Department> departments = departmentRepository.findAll();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch departments: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable String id) {
        try {
            Optional<Department> optional = departmentRepository.findById(id);
            if (optional.isPresent()) {
                return ResponseEntity.ok(optional.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Department not found with ID: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch department: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createDepartment(@RequestBody Department department) {
        try {
            if (department == null || department.getDepartmentName() == null || department.getDepartmentName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Department name cannot be empty"));
            }
            if (department.getDivision() == null || department.getDivision().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Division cannot be empty"));
            }
            // Check for duplicate departmentName and division
            Department existing = departmentRepository.findByDepartmentNameAndDivision(
                    department.getDepartmentName(), department.getDivision());
            if (existing != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Department with this name and division already exists"));
            }
            departmentRepository.save(department);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Department created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create department: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable String id, @RequestBody Department updated) {
        try {
            if (updated == null || updated.getDepartmentName() == null || updated.getDepartmentName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Department name cannot be empty"));
            }
            if (updated.getDivision() == null || updated.getDivision().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Division cannot be empty"));
            }
            // Check for duplicate departmentName and division (excluding current id)
            Department existing = departmentRepository.findByDepartmentNameAndDivisionAndIdNot(
                    updated.getDepartmentName(), updated.getDivision(), id);
            if (existing != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Department with this name and division already exists"));
            }
            Optional<Department> optional = departmentRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Department not found with ID: " + id));
            }

            // Check if the department is referenced in requisitions
            Department department = optional.get();
            if (summaryRequisitionRepository.existsByDepartmentRequestQtyKey(id)) {
                String message = String.format(
                        "Cannot update department '%s' because it is referenced by requisition(s).",
                        department.getDepartmentName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message));
            }
            if (requisitionMonthlyRepository.existsByDepartmentRequisitionsId(id)) {
                String message = String.format(
                        "Cannot update department '%s' because it is referenced by requisition(s).",
                        department.getDepartmentName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message));
            }

            // Proceed with the update
            department.setDivision(updated.getDivision());
            department.setDepartmentName(updated.getDepartmentName());
            Department saved = departmentRepository.save(department);
            return ResponseEntity.ok(Map.of("message", "Department updated successfully", "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update department: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDepartment(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid department ID"));
            }

            Optional<Department> optional = departmentRepository.findById(id);
            if (!optional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Department not found with ID: " + id));
            }

            Department department = optional.get();
            if (summaryRequisitionRepository.existsByDepartmentRequestQtyKey(id)) {
                String message = String.format(
                        "Cannot delete department '%s' because it is referenced by requisition(s).",
                        department.getDepartmentName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message));
            }

            if (requisitionMonthlyRepository.existsByDepartmentRequisitionsId(id)) {
                String message = String.format(
                        "Cannot delete department '%s' because it is referenced by requisition(s).",
                        department.getDepartmentName());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message));
            }

            departmentRepository.deleteById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "Department deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete department: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDepartments(@RequestParam String keyword) {
        try {
            List<Department> departments = departmentRepository.findByDepartmentNameContainingIgnoreCase(keyword);
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search departments: " + e.getMessage()));
        }
    }

    @GetMapping("/page")
    public ResponseEntity<?> getAllDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("createdAt")));
            Page<Department> departmentsPage = departmentRepository.findAll(pageable);
            return ResponseEntity.ok(departmentsPage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch departments: " + e.getMessage()));
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filterDepartments(
            @RequestParam(defaultValue = "") String departmentName,
            @RequestParam(defaultValue = "") String division,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
            Page<Department> result = departmentRepository
                    .searchByDepartmentNameAndDivision(departmentName, division, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to filter departments: " + e.getMessage()));
        }
    }
}