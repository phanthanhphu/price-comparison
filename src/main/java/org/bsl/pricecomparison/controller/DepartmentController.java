package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.Department;
import org.bsl.pricecomparison.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (departmentRepository.findByName(department.getName()) != null) {
            return new ResponseEntity<>("Department with this name already exists", HttpStatus.BAD_REQUEST);
        }
        departmentRepository.save(department);
        return new ResponseEntity<>("Department created successfully", HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public Department updateDepartment(@PathVariable String id, @RequestBody Department updated) {
        Optional<Department> optional = departmentRepository.findById(id);
        if (optional.isPresent()) {
            Department dept = optional.get();
            dept.setName(updated.getName());
            dept.setEnglishName(updated.getEnglishName());
            return departmentRepository.save(dept);
        } else {
            return null;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable String id) {
        departmentRepository.deleteById(id);
    }

    @GetMapping("/search")
    public List<Department> searchDepartments(@RequestParam String name) {
        return departmentRepository.findByNameContainingIgnoreCase(name);
    }
}
