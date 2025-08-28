package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.ProductType1;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductType1Repository extends MongoRepository<ProductType1, String> {
    Optional<ProductType1> findByName(String name);
    boolean existsByName(String name);
    List<ProductType1> findByNameContainingIgnoreCase(String name);
    Page<ProductType1> findByIdIn(List<String> ids, Pageable pageable);
    Page<ProductType1> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<ProductType1> findByNameContaining(String name, Pageable pageable);
}
