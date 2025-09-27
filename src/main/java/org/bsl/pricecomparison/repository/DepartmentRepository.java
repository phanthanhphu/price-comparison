package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface DepartmentRepository extends MongoRepository<Department, String> {

    Department findByDepartmentName(String departmentName);

    List<Department> findByDepartmentNameContainingIgnoreCase(String departmentName);

    Page<Department> findAll(Pageable pageable);

    @Query("{ 'departmentName': { $regex: ?0, $options: 'i' }, 'division': { $regex: ?1, $options: 'i' } }")
    Page<Department> searchByDepartmentNameAndDivision(String departmentName, String division, Pageable pageable);

    @Query("{ 'departmentName': ?0, 'division': ?1, 'id': { $ne: ?2 } }")
    Department findByDepartmentNameAndDivisionAndIdNot(String departmentName, String division, String id);

    @Query("{ 'departmentName': ?0, 'division': ?1 }")
    Department findByDepartmentNameAndDivision(String departmentName, String division);
}
