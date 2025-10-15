package org.bsl.pricecomparison.controller;
import org.bsl.pricecomparison.dto.ErrorResponseDTO;
import org.bsl.pricecomparison.model.ProductType1;
import org.bsl.pricecomparison.model.ProductType2;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product-type-1")
public class ProductType1Controller {

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping
    public ResponseEntity<?> create(@RequestParam String name) {
        try {
            ProductType1 type1 = productType1Service.create(name);
            return ResponseEntity.ok(type1);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create ProductType1: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestParam String name) {
        try {
            // Check if ProductType1 exists
            ProductType1 productType1 = productType1Service.getById(id);
            if (productType1 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "ProductType1 not found with ID: " + id));
            }

            // Check for references in ProductType2
            Query productType2Query = new Query(Criteria.where("productType1Id").is(id)).limit(1);
            List<ProductType2> productType2s = mongoTemplate.find(productType2Query, ProductType2.class);

            // If ProductType2 references exist
            if (!productType2s.isEmpty()) {
                List<String> conflictingItems = productType2s.stream()
                        .map(ProductType2::getName)
                        .filter(itemName -> itemName != null)
                        .collect(Collectors.toList());

                String message = String.format("Cannot update ProductType1 '%s' because it is referenced by %d ProductType2 item(s).",
                        productType1.getName(), productType2s.size());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message, "conflictingItems", conflictingItems));
            }

            // Update ProductType1 if no references exist
            ProductType1 updated = productType1Service.update(id, name);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update ProductType1: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            // Check if ProductType1 exists
            ProductType1 productType1 = productType1Service.getById(id);
            if (productType1 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "ProductType1 not found with ID: " + id));
            }

            // Check for references in ProductType2
            Query productType2Query = new Query(Criteria.where("productType1Id").is(id)).limit(1);
            List<ProductType2> productType2s = mongoTemplate.find(productType2Query, ProductType2.class);

            // If ProductType2 references exist
            if (!productType2s.isEmpty()) {
                List<String> conflictingItems = productType2s.stream()
                        .map(ProductType2::getName)
                        .filter(name -> name != null)
                        .collect(Collectors.toList());

                String message = String.format("Cannot delete ProductType1 '%s' because it is referenced by %d ProductType2 item(s).",
                        productType1.getName(), productType2s.size());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message, "conflictingItems", conflictingItems));
            }

            // Delete ProductType1 if no references exist
            productType1Service.delete(id);
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete ProductType1: " + e.getMessage()));
        }
    }
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            ProductType1 type1 = productType1Service.getById(id);
            if (type1 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "ProductType1 not found with ID: " + id));
            }
            return ResponseEntity.ok(type1);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch ProductType1: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllPaged(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ProductType1> result = productType1Service.getAllPaged(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch ProductType1 list: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        try {
            Page<ProductType1> result = productType1Service.getAllPaged(page, size, name);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search ProductType1 list: " + e.getMessage()));
        }
    }
}