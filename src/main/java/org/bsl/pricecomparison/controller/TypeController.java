package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.ProductType1;
import org.bsl.pricecomparison.model.ProductType2;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/search")
public class TypeController {

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private ProductType2Service productType2Service;

    @GetMapping
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) String type1Name,
            @RequestParam(required = false) String type2Name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> resultList = new ArrayList<>();
            Page<?> paginationPage = null;

            // Case 2: Filter by type2Name
            if (type2Name != null && !type2Name.trim().isEmpty()) {
                Page<ProductType2> productType2Page = productType2Service.searchByName(type2Name, pageable);
                paginationPage = productType2Page;

                for (ProductType2 type2 : productType2Page.getContent()) {
                    ProductType1 parent = productType1Service.getById(type2.getProductType1Id());
                    if (parent != null) {
                        // Tìm trong resultList đã có parent chưa
                        Map<String, Object> parentMap = resultList.stream()
                                .filter(p -> p.get("id").equals(parent.getId()))
                                .findFirst()
                                .orElseGet(() -> {
                                    Map<String, Object> newParent = new HashMap<>();
                                    newParent.put("id", parent.getId());
                                    newParent.put("name", parent.getName());
                                    newParent.put("createdDate", parent.getCreatedDate());
                                    newParent.put("code", new ArrayList<Map<String, Object>>());
                                    resultList.add(newParent);
                                    return newParent;
                                });

                        // Add type2 vào code
                        Map<String, Object> codeMap = new HashMap<>();
                        codeMap.put("id", type2.getId());
                        codeMap.put("name", type2.getName());
                        codeMap.put("createdDate", type2.getCreatedDate());
                        ((List<Map<String, Object>>) parentMap.get("code")).add(codeMap);
                    }
                }
            }
            // Case 1: Filter by type1Name
            else if (type1Name != null && !type1Name.trim().isEmpty()) {
                Page<ProductType1> productType1Page = productType1Service.searchByName(type1Name, pageable);
                paginationPage = productType1Page;

                for (ProductType1 type1 : productType1Page.getContent()) {
                    Map<String, Object> type1Map = new HashMap<>();
                    type1Map.put("id", type1.getId());
                    type1Map.put("name", type1.getName());
                    type1Map.put("createdDate", type1.getCreatedDate());

                    List<Map<String, Object>> codeList = new ArrayList<>();
                    Page<ProductType2> children = productType2Service.getByProductType1IdPaged(type1.getId(), 0, Integer.MAX_VALUE);
                    for (ProductType2 type2 : children) {
                        Map<String, Object> codeMap = new HashMap<>();
                        codeMap.put("id", type2.getId());
                        codeMap.put("name", type2.getName());
                        codeMap.put("createdDate", type2.getCreatedDate());
                        codeList.add(codeMap);
                    }
                    type1Map.put("code", codeList);

                    resultList.add(type1Map);
                }
            }
            // Case 3: No filters
            else {
                Page<ProductType1> productType1Page = productType1Service.getAllPaged(page, size);
                paginationPage = productType1Page;

                for (ProductType1 type1 : productType1Page.getContent()) {
                    Map<String, Object> type1Map = new HashMap<>();
                    type1Map.put("id", type1.getId());
                    type1Map.put("name", type1.getName());
                    type1Map.put("createdDate", type1.getCreatedDate());

                    List<Map<String, Object>> codeList = new ArrayList<>();
                    Page<ProductType2> children = productType2Service.getByProductType1IdPaged(type1.getId(), 0, Integer.MAX_VALUE);
                    for (ProductType2 type2 : children) {
                        Map<String, Object> codeMap = new HashMap<>();
                        codeMap.put("id", type2.getId());
                        codeMap.put("name", type2.getName());
                        codeMap.put("createdDate", type2.getCreatedDate());
                        codeList.add(codeMap);
                    }
                    type1Map.put("code", codeList);

                    resultList.add(type1Map);
                }
            }

            // Build response
            response.put("data", resultList);

            if (paginationPage != null) {
                Map<String, Object> pagination = new HashMap<>();
                pagination.put("page", paginationPage.getNumber());
                pagination.put("size", paginationPage.getSize());
                pagination.put("totalElements", paginationPage.getTotalElements());
                pagination.put("totalPages", paginationPage.getTotalPages());
                response.put("pagination", pagination);
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}