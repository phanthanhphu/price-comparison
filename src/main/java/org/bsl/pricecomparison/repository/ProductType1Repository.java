package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.ProductType1;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductType1Repository extends MongoRepository<ProductType1, String> {
    Optional<ProductType1> findByName(String name);
    boolean existsByName(String name);
}
