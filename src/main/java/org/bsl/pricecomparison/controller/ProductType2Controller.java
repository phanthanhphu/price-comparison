package org.bsl.pricecomparison.controller;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.bsl.pricecomparison.model.ProductType2;
import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.model.SummaryRequisition;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.service.ProductType2Service;
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
@RequestMapping("/api/product-type-2")
public class ProductType2Controller {

    @Autowired
    private ProductType2Service productType2Service;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping
    public ResponseEntity<?> create(@RequestParam String name, @RequestParam String productType1Id) {
        try {
            ProductType2 created = productType2Service.create(name, productType1Id);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create ProductType2: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestParam String newName) {
        try {
            // Check if ProductType2 exists
            ProductType2 productType2 = productType2Service.getById(id);
            if (productType2 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "ProductType2 not found with ID: " + id));
            }

            // Check for references in SupplierProduct
            Query supplierProductQuery = new Query(Criteria.where("productType2Id").is(id)).limit(1);
            List<SupplierProduct> supplierProducts = mongoTemplate.find(supplierProductQuery, SupplierProduct.class);

            // If SupplierProduct references exist
            if (!supplierProducts.isEmpty()) {
                List<String> conflictingItems = supplierProducts.stream()
                        .map(SupplierProduct::getItemNo)
                        .filter(itemName -> itemName != null)
                        .collect(Collectors.toList());

                String message = String.format("Cannot update ProductType2 '%s' because it is referenced by %d SupplierProduct item(s).",
                        productType2.getName(), supplierProducts.size());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message, "conflictingItems", conflictingItems));
            }

            // Update ProductType2 if no references exist
            ProductType2 updated = productType2Service.update(id, newName);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update ProductType2: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            // Check if ProductType2 exists
            ProductType2 productType2 = productType2Service.getById(id);
            if (productType2 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "ProductType2 not found with ID: " + id));
            }

            // Check for references in SupplierProduct
            Query supplierProductQuery = new Query(Criteria.where("productType2Id").is(id)).limit(1);
            List<SupplierProduct> supplierProducts = mongoTemplate.find(supplierProductQuery, SupplierProduct.class);

            // If SupplierProduct references exist
            if (!supplierProducts.isEmpty()) {
                List<String> conflictingItems = supplierProducts.stream()
                        .map(SupplierProduct::getItemNo)
                        .filter(itemName -> itemName != null)
                        .collect(Collectors.toList());

                String message = String.format("Cannot delete ProductType2 '%s' because it is referenced by %d SupplierProduct item(s).",
                        productType2.getName(), supplierProducts.size());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", message, "conflictingItems", conflictingItems));
            }

            // Delete ProductType2 if no references exist
            productType2Service.delete(id);
            return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete ProductType2: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            ProductType2 type2 = productType2Service.getById(id);
            if (type2 == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "ProductType2 not found with ID: " + id));
            }
            return ResponseEntity.ok(type2);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch ProductType2: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getByProductType1IdPaged(
            @RequestParam String productType1Id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ProductType2> result = productType2Service.getByProductType1IdPaged(productType1Id, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch ProductType2 list: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> getByProductType1IdPaged(
            @RequestParam(required = false) String productType1Id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        try {
            Page<ProductType2> result = productType2Service.getByProductType1IdPaged(productType1Id, page, size, name);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to search ProductType2 list: " + e.getMessage()));
        }
    }
}