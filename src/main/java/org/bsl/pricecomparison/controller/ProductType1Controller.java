package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.ProductType1;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-type-1")
public class ProductType1Controller {

    @Autowired
    private ProductType1Service productType1Service;

    @PostMapping
    public ResponseEntity<?> create(@RequestParam String name) {
        try {
            ProductType1 type1 = productType1Service.create(name);
            return ResponseEntity.ok(type1);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestParam String name) {
        try {
            ProductType1 updated = productType1Service.update(id, name);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            productType1Service.delete(id);
            return ResponseEntity.ok("Deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            ProductType1 type1 = productType1Service.getById(id);
            return ResponseEntity.ok(type1);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllPaged(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Page<ProductType1> result = productType1Service.getAllPaged(page, size);
        return ResponseEntity.ok(result);
    }
}
