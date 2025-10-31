package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.model.ProductType1;
import org.bsl.pricecomparison.repository.ProductType1Repository;
import org.bsl.pricecomparison.repository.ProductType2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductType1Service {

    @Autowired
    private ProductType1Repository productType1Repository;

    @Autowired
    private ProductType2Repository productType2Repository;

    public ProductType1 create(String name) {
        if (productType1Repository.existsByName(name)) {
            throw new IllegalArgumentException("Name already exists.");
        }

        ProductType1 type1 = new ProductType1(name, LocalDateTime.now());
        return productType1Repository.save(type1);
    }

    public ProductType1 update(String id, String newName) {
        ProductType1 existing = productType1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProductType1 ID not found."));

        if (!existing.getName().equals(newName) && productType1Repository.existsByName(newName)) {
            throw new IllegalArgumentException("Name already exists.");
        }

        existing.setName(newName);
        return productType1Repository.save(existing);
    }

    public void delete(String id) {
        if (!productType1Repository.existsById(id)) {
            throw new IllegalArgumentException("ProductType1 ID not found.");
        }

        if (productType2Repository.existsByProductType1Id(id)) {
            throw new IllegalStateException("Cannot delete: there are associated ProductType2 items.");
        }

        productType1Repository.deleteById(id);
    }

    public ProductType1 getById(String id) {
        return productType1Repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProductType1 ID not found."));
    }

    public Page<ProductType1> getAllPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productType1Repository.findAll(pageable);
    }

    public Page<ProductType1> getAllPaged(int page, int size, String name) {
        Pageable pageable = PageRequest.of(page, size);
        if (name != null && !name.isEmpty()) {
            return productType1Repository.findByNameContaining(name, pageable);
        }
        return productType1Repository.findAll(pageable);
    }

    public Page<ProductType1> searchByName(String name, Pageable pageable) {
        if (name == null || name.trim().isEmpty()) {
            return productType1Repository.findAll(pageable);
        }
        return productType1Repository.findByNameContainingIgnoreCase(name, pageable);
    }

    public Page<ProductType1> getByIds(List<String> ids, Pageable pageable) {
        if (ids == null || ids.isEmpty()) {
            return Page.empty(pageable);
        }
        return productType1Repository.findByIdIn(ids, pageable);
    }

    public Map<String, String> findNamesByIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        // 🔥 MONGODB: Trả List<ProductType1> thay vì Object[]
        return productType1Repository.findNamesByIds(ids)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ProductType1::getId,
                        ProductType1::getName,
                        (existing, replacement) -> existing
                ));
    }

    public String getNameById(String id) {
        if (id == null || id.isBlank()) return null;
        return productType1Repository.findById(id)
                .map(ProductType1::getName)
                .orElse(null);
    }
}
