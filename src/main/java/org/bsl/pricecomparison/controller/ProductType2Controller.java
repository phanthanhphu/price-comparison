package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.ProductType2;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-type-2")
public class ProductType2Controller {

    @Autowired
    private ProductType2Service productType2Service;

    @PostMapping
    public ResponseEntity<?> create(@RequestParam String name, @RequestParam String productType1Id) {
        try {
            ProductType2 created = productType2Service.create(name, productType1Id);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestParam String name) {
        try {
            ProductType2 updated = productType2Service.update(id, name);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            productType2Service.delete(id);
            return ResponseEntity.ok("Deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try {
            ProductType2 type2 = productType2Service.getById(id);
            return ResponseEntity.ok(type2);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getByProductType1IdPaged(
            @RequestParam String productType1Id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductType2> result = productType2Service.getByProductType1IdPaged(productType1Id, page, size);
        return ResponseEntity.ok(result);
    }
}
