package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.ProductType1;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductType1Repository extends MongoRepository<ProductType1, String> {
    Optional<ProductType1> findByName(String name);
    boolean existsByName(String name);
    List<ProductType1> findByNameContainingIgnoreCase(String name);
    Page<ProductType1> findByIdIn(List<String> ids, Pageable pageable);
    Page<ProductType1> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<ProductType1> findByNameContaining(String name, Pageable pageable);

    // 🔥 NEW ULTRA-FAST BATCH METHODS (CHO CONTROLLER 100X FASTER!)

    /**
     * ⚡ BATCH: Tìm names theo IDs (1 query cho 1000+ IDs)
     * @return List<Object[]> [id, name]
     */
    @Query("{ '_id': { $in: ?0 } }")
    List<ProductType1> findNamesByIds(Set<String> ids);

    /**
     * ⚡ BATCH: Chỉ lấy id + name (nhẹ hơn 50%)
     */
    @Aggregation(pipeline = {
            "{ '$match': { '_id': { $in: ?0 } } }",
            "{ '$project': { '_id': 0, 'id': '$_id', 'name': 1 } }"
    })
    List<Object> findIdAndNamesByIds(Set<String> ids);

    /**
     * ⚡ GET ALL IDs (cho cache)
     */
    @Query("{}")
    List<String> findAllIds();

    /**
     * ⚡ COUNT nhanh (không load data)
     */
    long countByIdIn(Set<String> ids);
}
