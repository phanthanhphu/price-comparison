package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DepartmentRepository extends MongoRepository<Department, String> {
    Department findByName(String name);

    List<Department> findByNameContainingIgnoreCase(String name);
}
