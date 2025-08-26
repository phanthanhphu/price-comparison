package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.ProductType2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductType2Repository extends MongoRepository<ProductType2, String> {

    Page<ProductType2> findByProductType1Id(String productType1Id, Pageable pageable);

    boolean existsByProductType1Id(String productType1Id);

    boolean existsByNameAndProductType1Id(String name, String productType1Id);

    Optional<ProductType2> findByNameAndProductType1Id(String name, String productType1Id);

    List<ProductType2> findByNameContainingIgnoreCase(String name);
}
