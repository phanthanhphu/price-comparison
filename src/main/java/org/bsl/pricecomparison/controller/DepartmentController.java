package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.Department;
import org.bsl.pricecomparison.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Department getDepartmentById(@PathVariable String id) {
        return departmentRepository.findById(id).orElse(null);
    }

    @PostMapping
    public ResponseEntity<String> createDepartment(@RequestBody Department department) {
        if (departmentRepository.findByDepartmentName(department.getDepartmentName()) != null) {
            return new ResponseEntity<>("Department with this name already exists", HttpStatus.BAD_REQUEST);
        }
        departmentRepository.save(department);
        return new ResponseEntity<>("Department created successfully", HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable String id, @RequestBody Department updated) {
        Optional<Department> optional = departmentRepository.findById(id);
        if (optional.isPresent()) {
            Department dept = optional.get();
            dept.setDivision(updated.getDivision());
            dept.setDepartmentName(updated.getDepartmentName());
            return ResponseEntity.ok(departmentRepository.save(dept));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable String id) {
        if (departmentRepository.existsById(id)) {
            departmentRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public List<Department> searchDepartments(@RequestParam String keyword) {
        return departmentRepository.findByDepartmentNameContainingIgnoreCase(keyword);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<Department>> getAllDepartments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("createdAt")));
        Page<Department> departmentsPage = departmentRepository.findAll(pageable);

        return ResponseEntity.ok(departmentsPage);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<Department>> filterDepartments(
            @RequestParam(defaultValue = "") String departmentName,
            @RequestParam(defaultValue = "") String division,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));

        Page<Department> result = departmentRepository
                .searchByDepartmentNameAndDivision(departmentName, division, pageable);

        return ResponseEntity.ok(result);
    }
}
