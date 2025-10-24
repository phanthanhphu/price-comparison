package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.ProductType2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProductType2Repository extends MongoRepository<ProductType2, String> {

    Page<ProductType2> findByProductType1Id(String productType1Id, Pageable pageable);

    boolean existsByProductType1Id(String productType1Id);

    boolean existsByNameAndProductType1Id(String name, String productType1Id);

    Optional<ProductType2> findByNameAndProductType1Id(String name, String productType1Id);

    Page<ProductType2> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<ProductType2> findByProductType1IdIn(List<String> productType1Ids, Pageable pageable);

    Page<ProductType2> findByProductType1IdAndNameContaining(String productType1Id, String name, Pageable pageable);
    Page<ProductType2> findByNameContaining(String name, Pageable pageable);

    /**
     * ⚡ BATCH: Tìm names theo IDs (1 query cho 1000+ IDs)
     * @return List<ProductType2> [id, name]
     */
    @Query("{ '_id': { $in: ?0 } }")
    List<ProductType2> findNamesByIds(Set<String> ids);

    /**
     * ⚡ BATCH: Chỉ lấy id + name (nhẹ hơn 50% memory)
     */
    @Query(value = "{ '_id': { $in: ?0 } }", fields = "{ '_id': 1, 'name': 1 }")
    List<ProductType2> findIdAndNameOnlyByIds(Set<String> ids);

    /**
     * ⚡ GET ALL IDs (cho cache - 1ms)
     */
    @Query(value = "{}", fields = "{ '_id': 1 }")
    List<String> findAllIds();

    /**
     * ⚡ COUNT nhanh theo IDs (không load data)
     */
    long countByIdIn(Set<String> ids);
}