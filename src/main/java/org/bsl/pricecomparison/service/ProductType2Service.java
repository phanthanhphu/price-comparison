package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.ProductType2;
import org.bsl.pricecomparison.repository.ProductType2Repository;
import org.bsl.pricecomparison.repository.ProductType1Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProductType2Service {

    @Autowired
    private ProductType2Repository productType2Repository;

    @Autowired
    private ProductType1Repository productType1Repository;

    public ProductType2 create(String name, String productType1Id) {
        if (!productType1Repository.existsById(productType1Id)) {
            throw new IllegalArgumentException("ProductType1 ID not found.");
        }

        if (productType2Repository.existsByNameAndProductType1Id(name, productType1Id)) {
            throw new IllegalArgumentException("Name already exists under this ProductType1.");
        }

        ProductType2 type2 = new ProductType2(name, LocalDateTime.now(), productType1Id);
        return productType2Repository.save(type2);
    }

    public ProductType2 update(String id, String newName) {
        ProductType2 existing = productType2Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProductType2 ID not found."));

        String productType1Id = existing.getProductType1Id();

        if (!existing.getName().equals(newName) && productType2Repository.existsByNameAndProductType1Id(newName, productType1Id)) {
            throw new IllegalArgumentException("Name already exists under this ProductType1.");
        }

        existing.setName(newName);
        return productType2Repository.save(existing);
    }

    public void delete(String id) {
        if (!productType2Repository.existsById(id)) {
            throw new IllegalArgumentException("ProductType2 ID not found.");
        }

        productType2Repository.deleteById(id);
    }

    public ProductType2 getById(String id) {
        return productType2Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProductType2 ID not found."));
    }

    public Page<ProductType2> getByProductType1IdPaged(String productType1Id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productType2Repository.findByProductType1Id(productType1Id, pageable);
    }
}
