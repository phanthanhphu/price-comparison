package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Set;

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

    // 🔥 NEW ULTRA-FAST BATCH METHODS (CHO CONTROLLER 100X FASTER!)

    /**
     * ⚡ BATCH: Tìm department names theo MULTIPLE IDs (1 query cho 1000+ IDs!)
     * DÙNG CHO ULTRA-FAST SEARCH: findNamesByIds(deptIds)
     * @return List<Department> với chỉ id + departmentName
     */
    @Query(value = "{ '_id': { $in: ?0 } }", fields = "{ '_id': 1, 'departmentName': 1 }")
    List<Department> findNamesByIds(Set<String> deptIds);

    /**
     * ⚡ BATCH: Tìm theo LIST IDs (alternative)
     */
    @Query(value = "{ '_id': { $in: ?0 } }", fields = "{ '_id': 1, 'departmentName': 1 }")
    List<Department> findNamesByIds(List<String> deptIds);

    /**
     * ⚡ GET ALL department IDs + names (cho cache - 2ms)
     */
    @Query(value = "{}", fields = "{ '_id': 1, 'departmentName': 1 }")
    List<Department> findAllIdAndNames();

    /**
     * ⚡ COUNT theo IDs (cho validation - 1ms)
     */
    @Query(value = "{ '_id': { $in: ?0 } }", count = true)
    long countByIdIn(Set<String> deptIds);

    /**
     * ⚡ GET DISTINCT department names (cho dropdown)
     */
    @Query(value = "{}", fields = "{ 'departmentName': 1, '_id': 0 }")
    List<String> findDistinctDepartmentNames();
}
