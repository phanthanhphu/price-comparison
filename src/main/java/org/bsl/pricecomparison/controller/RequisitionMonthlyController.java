package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsl.pricecomparison.dto.*;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.RequisitionMonthlyRepository;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.bsl.pricecomparison.request.CreateRequisitionMonthlyRequest;
import org.bsl.pricecomparison.request.UpdateRequisitionMonthlyRequest;
import org.bsl.pricecomparison.response.RequisitionMonthlyPagedResponse;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class RequisitionMonthlyController {

    @Autowired
    private RequisitionMonthlyRepository requisitionMonthlyRepository;

    @Autowired
    private SupplierProductRepository supplierRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private ProductType2Service productType2Service;

    @Autowired
    private SupplierProductRepository supplierProductRepository;

    @Autowired
    private GroupSummaryRequisitionService groupSummaryRequisitionService;

    private static final String UPLOAD_DIR = "./uploads/";

    @PostMapping(value = "/requisition-monthly", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addRequisitionMonthly(@ModelAttribute CreateRequisitionMonthlyRequest request) {
        try {
            // Validate oldSAPCode
            if (request.getOldSAPCode() == null || request.getOldSAPCode().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "oldSAPCode is required"));
            }

            if (request.getGroupId() != null && request.getOldSAPCode() != null) {
                Optional<RequisitionMonthly> existing = requisitionMonthlyRepository.findByGroupIdAndOldSAPCode(
                        request.getGroupId(), request.getOldSAPCode()
                );
                if (existing.isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "Duplicate entry: groupId and oldSapCode must be unique together."));
                }
            }

            // Validate and fetch supplier
            SupplierProduct supplier = null;
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                Optional<SupplierProduct> supplierOptional = supplierRepository.findById(request.getSupplierId());
                if (supplierOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Supplier not found for supplierId: " + request.getSupplierId()));
                }
                supplier = supplierOptional.get();
            }

            // Create RequisitionMonthly entity
            RequisitionMonthly requisition = new RequisitionMonthly();
            requisition.setGroupId(request.getGroupId());
            requisition.setItemDescriptionEN(request.getItemDescriptionEN());
            requisition.setItemDescriptionVN(request.getItemDescriptionVN());
            requisition.setOldSAPCode(request.getOldSAPCode());
            requisition.setHanaSAPCode(request.getHanaSAPCode());
            requisition.setUnit(supplier != null ? supplier.getUnit() : null);
            requisition.setDailyMedInventory(request.getDailyMedInventory() != null ?
                    request.getDailyMedInventory() : BigDecimal.ZERO);
            requisition.setSafeStock(request.getSafeStock() != null ? request.getSafeStock() : BigDecimal.ZERO);
            requisition.setPrice(supplier != null && supplier.getPrice() != null ? supplier.getPrice() : BigDecimal.ZERO);
            requisition.setGoodType(supplier != null && supplier.getGoodType() != null ? supplier.getGoodType() : "");
            requisition.setCurrency(supplier != null && supplier.getCurrency() != null ? supplier.getCurrency() : "VND");
            requisition.setSupplierId(request.getSupplierId());
            requisition.setSupplierName(supplier != null ? supplier.getSupplierName() : null);
            requisition.setFullDescription(request.getFullDescription());
            requisition.setReason(request.getReason());
            requisition.setRemark(request.getRemark());
            requisition.setRemarkComparison(request.getRemarkComparison());
            requisition.setProductType1Id(request.getProductType1Id());
            requisition.setProductType2Id(request.getProductType2Id());
            requisition.setCreatedDate(LocalDateTime.now());
            requisition.setUpdatedDate(LocalDateTime.now());

            // Process department requisitions
            List<DepartmentRequisitionMonthly> deptRequisitions = new ArrayList<>();
            if (request.getDepartmentRequisitions() != null && !request.getDepartmentRequisitions().isEmpty()) {
                try {
                    List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptDTOs = objectMapper.readValue(
                            request.getDepartmentRequisitions(),
                            new TypeReference<List<DepartmentRequisitionMonthly.DepartmentRequestDTO>>() {
                            }
                    );
                    for (DepartmentRequisitionMonthly.DepartmentRequestDTO dto : deptDTOs) {
                        deptRequisitions.add(new DepartmentRequisitionMonthly(
                                dto.getId(),
                                dto.getName(),
                                dto.getQty(),
                                dto.getBuy()
                        ));
                    }
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid departmentRequisitions JSON format: " + e.getMessage()));
                }
            }
            requisition.setDepartmentRequisitions(deptRequisitions);

            // Calculate totalRequestQty
            BigDecimal totalRequestQty = deptRequisitions.stream()
                    .map(DepartmentRequisitionMonthly::getBuy)
                    .filter(buy -> buy != null)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            requisition.setTotalRequestQty(totalRequestQty);

            // Calculate useStockQty
            BigDecimal useStockQty = (requisition.getDailyMedInventory() != null ? requisition.getDailyMedInventory() : BigDecimal.ZERO)
                    .subtract(requisition.getSafeStock() != null ? requisition.getSafeStock() : BigDecimal.ZERO);
            requisition.setUseStockQty(useStockQty);

            // Calculate orderQty
            BigDecimal orderQty = totalRequestQty.subtract(useStockQty);
            requisition.setOrderQty(orderQty);

            // Calculate amount
            BigDecimal amount = (requisition.getPrice() != null ? requisition.getPrice() : BigDecimal.ZERO)
                    .multiply(orderQty);
            requisition.setAmount(amount);

            // Handle image uploads
            List<String> imageUrls = new ArrayList<>();
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        if (imageUrl != null) {
                            imageUrls.add(imageUrl);
                        }
                    }
                }
            }

            // Validate image count
            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }
            requisition.setImageUrls(imageUrls);

            // Save to database
            RequisitionMonthly savedRequisition = requisitionMonthlyRepository.save(requisition);

            // Return success response
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Requisition created successfully", "data", savedRequisition));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error processing file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error: " + e.getMessage()));
        }
    }

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IOException("Only JPEG, PNG, and GIF files are allowed");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());

        file.transferTo(path);

        return "/uploads/" + fileName;
    }

    @GetMapping("/requisition-monthly/filter")
    public ResponseEntity<RequisitionMonthlyPagedResponse> filterRequisitions(
            @RequestParam String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String hanaSapCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "false") boolean hasFilter,
            @RequestParam(defaultValue = "false") boolean disablePagination,
            Pageable pageable) {

        List<RequisitionMonthly> requisitions;

        if (hasFilter) {
            // Apply filters if hasFilter is true
            requisitions = requisitionMonthlyRepository.findByGroupId(groupId).stream()
                    .filter(req -> {
                        boolean matches = true;

                        // Resolve product type names for filtering
                        String resolvedProductType1Name = null;
                        if (req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()) {
                            ProductType1 productType1 = productType1Service.getById(req.getProductType1Id());
                            resolvedProductType1Name = productType1 != null ? productType1.getName() : "Unknown";
                        }

                        String resolvedProductType2Name = null;
                        if (req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()) {
                            ProductType2 productType2 = productType2Service.getById(req.getProductType2Id());
                            resolvedProductType2Name = productType2 != null ? productType2.getName() : "Unknown";
                        }

                        // Apply filters
                        if (productType1Name != null && !productType1Name.isEmpty()) {
                            matches = matches && resolvedProductType1Name != null &&
                                    resolvedProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                        }
                        if (productType2Name != null && !productType2Name.isEmpty()) {
                            matches = matches && resolvedProductType2Name != null &&
                                    resolvedProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                        }
                        if (englishName != null && !englishName.isEmpty()) {
                            matches = matches && req.getItemDescriptionEN() != null &&
                                    req.getItemDescriptionEN().toLowerCase().contains(englishName.toLowerCase());
                        }
                        if (vietnameseName != null && !vietnameseName.isEmpty()) {
                            matches = matches && req.getItemDescriptionVN() != null &&
                                    req.getItemDescriptionVN().toLowerCase().contains(vietnameseName.toLowerCase());
                        }
                        if (oldSapCode != null && !oldSapCode.isEmpty()) {
                            matches = matches && req.getOldSAPCode() != null &&
                                    req.getOldSAPCode().toLowerCase().contains(oldSapCode.toLowerCase());
                        }
                        if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                            matches = matches && req.getHanaSAPCode() != null &&
                                    req.getHanaSAPCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                        }
                        if (supplierName != null && !supplierName.isEmpty()) {
                            matches = matches && req.getSupplierName() != null &&
                                    req.getSupplierName().toLowerCase().contains(supplierName.toLowerCase());
                        }
                        if (departmentName != null && !departmentName.isEmpty()) {
                            List<String> deptNames = req.getDepartmentRequisitions() != null ?
                                    req.getDepartmentRequisitions().stream()
                                            .filter(dept -> dept != null)
                                            .map(DepartmentRequisitionMonthly::getName)
                                            .filter(dept -> dept != null)
                                            .collect(Collectors.toList()) : Collections.emptyList();
                            matches = matches && deptNames.stream()
                                    .anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                        }

                        return matches;
                    })
                    .sorted((req1, req2) -> {
                        LocalDateTime date1 = req1.getUpdatedDate() != null ? req1.getUpdatedDate() :
                                req1.getCreatedDate() != null ? req1.getCreatedDate() : LocalDateTime.MIN;
                        LocalDateTime date2 = req2.getUpdatedDate() != null ? req2.getUpdatedDate() :
                                req2.getCreatedDate() != null ? req2.getCreatedDate() : LocalDateTime.MIN;
                        return date2.compareTo(date1); // Descending order (newest first)
                    })
                    .collect(Collectors.toList());
        } else {
            // No filters, fetch all records for groupId with sorting
            requisitions = requisitionMonthlyRepository.findByGroupId(groupId).stream()
                    .sorted((req1, req2) -> {
                        LocalDateTime date1 = req1.getUpdatedDate() != null ? req1.getUpdatedDate() :
                                req1.getCreatedDate() != null ? req1.getCreatedDate() : LocalDateTime.MIN;
                        LocalDateTime date2 = req2.getUpdatedDate() != null ? req2.getUpdatedDate() :
                                req2.getCreatedDate() != null ? req2.getCreatedDate() : LocalDateTime.MIN;
                        return date2.compareTo(date1); // Descending order (newest first)
                    })
                    .collect(Collectors.toList());
        }

        // Calculate totals across all requisitions
        BigDecimal totalSumDailyMedInventory = requisitions.stream()
                .map(req -> req.getDailyMedInventory() != null ? req.getDailyMedInventory() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSumSafeStock = requisitions.stream()
                .map(req -> req.getSafeStock() != null ? req.getSafeStock() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSumRequestQty = requisitions.stream()
                .map(req -> req.getTotalRequestQty() != null ? req.getTotalRequestQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSumUseStockQty = requisitions.stream()
                .map(req -> req.getUseStockQty() != null ? req.getUseStockQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSumOrderQty = requisitions.stream()
                .map(req -> req.getOrderQty() != null ? req.getOrderQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSumAmount = requisitions.stream()
                .map(req -> req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSumPrice = requisitions.stream()
                .map(req -> req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map RequisitionMonthly to RequisitionMonthlyDTO with product type names
        List<RequisitionMonthlyDTO> requisitionDTOs = requisitions.stream()
                .map(req -> {
                    String resolvedProductType1Name = null;
                    if (req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()) {
                        ProductType1 productType1 = productType1Service.getById(req.getProductType1Id());
                        resolvedProductType1Name = productType1 != null ? productType1.getName() : "Unknown";
                    }

                    String resolvedProductType2Name = null;
                    if (req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()) {
                        ProductType2 productType2 = productType2Service.getById(req.getProductType2Id());
                        resolvedProductType2Name = productType2 != null ? productType2.getName() : "Unknown";
                    }

                    return new RequisitionMonthlyDTO(
                            req.getId(),
                            req.getGroupId(),
                            resolvedProductType1Name,
                            resolvedProductType2Name,
                            req.getItemDescriptionEN(),
                            req.getItemDescriptionVN(),
                            req.getOldSAPCode(),
                            req.getHanaSAPCode(),
                            req.getUnit(),
                            req.getDepartmentRequisitions(),
                            req.getDailyMedInventory(),
                            req.getSafeStock(),
                            req.getTotalRequestQty(),
                            req.getUseStockQty(),
                            req.getOrderQty(),
                            req.getAmount(),
                            req.getPrice(),
                            req.getCurrency(),
                            req.getGoodType(),
                            req.getSupplierName(),
                            req.getCreatedDate(),
                            req.getUpdatedDate(),
                            req.getFullDescription(),
                            req.getReason(),
                            req.getRemark(),
                            req.getRemarkComparison(),
                            req.getImageUrls()
                    );
                })
                .collect(Collectors.toList());

        // Apply pagination or return all records based on disablePagination
        List<RequisitionMonthlyDTO> resultRequisitionDTOs;
        Page<RequisitionMonthlyDTO> pagedResult;

        if (disablePagination) {
            // Return all records without pagination
            resultRequisitionDTOs = requisitionDTOs;
            pagedResult = new PageImpl<>(resultRequisitionDTOs, PageRequest.of(0, Integer.MAX_VALUE), requisitionDTOs.size());
        } else {
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), requisitionDTOs.size());
            resultRequisitionDTOs = requisitionDTOs.subList(start, end);
            pagedResult = new PageImpl<>(resultRequisitionDTOs, pageable, requisitionDTOs.size());
        }

        // Prepare paged response with totals
        RequisitionMonthlyPagedResponse response = new RequisitionMonthlyPagedResponse(pagedResult,
                totalSumDailyMedInventory, totalSumSafeStock, totalSumRequestQty, totalSumUseStockQty,
                totalSumOrderQty, totalSumAmount, totalSumPrice);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/requisition-monthly/search-by-group-id")
//    public ResponseEntity<List<RequisitionMonthlyDTO>> filterRequisitions(
//            @RequestParam String groupId) {
//
//        // Fetch all records for groupId and sort by updated/created date (newest first)
//        List<RequisitionMonthly> requisitions = requisitionMonthlyRepository.findByGroupId(groupId).stream()
//                .sorted((req1, req2) -> {
//                    LocalDateTime date1 = req1.getUpdatedDate() != null ? req1.getUpdatedDate() :
//                            req1.getCreatedDate() != null ? req1.getCreatedDate() : LocalDateTime.MIN;
//                    LocalDateTime date2 = req2.getUpdatedDate() != null ? req2.getUpdatedDate() :
//                            req2.getCreatedDate() != null ? req2.getCreatedDate() : LocalDateTime.MIN;
//                    return date2.compareTo(date1);
//                })
//                .collect(Collectors.toList());
//
//        // Calculate totals across all requisitions (once)
//        Double totalSumDailyMedInventory = requisitions.stream()
//                .mapToDouble(req -> req.getDailyMedInventory() != null ? req.getDailyMedInventory() : 0.0)
//                .sum();
//        Double totalSumSafeStock = requisitions.stream()
//                .mapToDouble(req -> req.getSafeStock() != null ? req.getSafeStock() : 0.0)
//                .sum();
//        Double totalSumRequestQty = requisitions.stream()
//                .mapToDouble(req -> req.getTotalRequestQty() != null ? req.getTotalRequestQty() : 0.0)
//                .sum();
//        Double totalSumUseStockQty = requisitions.stream()
//                .mapToDouble(req -> req.getUseStockQty() != null ? req.getUseStockQty() : 0.0)
//                .sum();
//        Double totalSumOrderQty = requisitions.stream()
//                .mapToDouble(req -> req.getOrderQty() != null ? req.getOrderQty() : 0.0)
//                .sum();
//        Double totalSumAmount = requisitions.stream()
//                .mapToDouble(req -> req.getAmount() != null ? req.getAmount() : 0.0)
//                .sum();
//        Double totalSumPrice = requisitions.stream()
//                .mapToDouble(req -> req.getPrice() != null ? req.getPrice() : 0.0)
//                .sum();
//
//        // Map to DTO with product type names and reuse totals
//        List<RequisitionMonthlyDTO> requisitionDTOs = requisitions.stream()
//                .map(req -> {
//                    String resolvedProductType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty() ?
//                            productType1Service.getById(req.getProductType1Id()).getName() : "Unknown";
//                    String resolvedProductType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty() ?
//                            productType2Service.getById(req.getProductType2Id()).getName() : "Unknown";
//
//                    return new RequisitionMonthlyDTO(
//                            req.getId(), req.getGroupId(), resolvedProductType1Name, resolvedProductType2Name,
//                            req.getItemDescriptionEN(), req.getItemDescriptionVN(), req.getOldSAPCode(),
//                            req.getHanaSAPCode(), req.getUnit(), req.getDepartmentRequisitions(),
//                            req.getDailyMedInventory(), req.getSafeStock(), req.getTotalRequestQty(),
//                            req.getUseStockQty(), req.getOrderQty(), req.getAmount(), req.getPrice(),
//                            req.getSupplierName(), req.getCreatedDate(), req.getUpdatedDate(),
//                            req.getFullDescription(), req.getReason(), req.getRemark(),
//                            req.getRemarkComparison(), req.getImageUrls(),
//                            totalSumDailyMedInventory, totalSumSafeStock, totalSumRequestQty,
//                            totalSumUseStockQty, totalSumOrderQty, totalSumAmount, totalSumPrice
//                    );
//                })
//                .collect(Collectors.toList());
//
//        return ResponseEntity.ok(requisitionDTOs);
//    }

    @DeleteMapping("/requisition-monthly/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            var requisitionOptional = requisitionMonthlyRepository.findById(id);
            if (requisitionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("ID not found: " + id);
            }

            var requisition = requisitionOptional.get();

            List<String> imageUrls = requisition.getImageUrls();
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (String imageUrl : imageUrls) {
                    if (imageUrl != null && !imageUrl.isBlank()) {
                        try {
                            String filePath = imageUrl.startsWith("/uploads/") ? "." + imageUrl : imageUrl;
                            Path path = Paths.get(filePath);
                            if (Files.exists(path)) {
                                Files.delete(path);
                                System.out.println("Deleted image: " + filePath);
                            }
                        } catch (IOException e) {
                            System.err.println("Error deleting image: " + imageUrl + ", error: " + e.getMessage());
                        }
                    }
                }
            }

            requisitionMonthlyRepository.deleteById(id);
            return ResponseEntity.ok("Successfully deleted ID: " + id);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deletion error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/requisition-monthly/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateRequisitionMonthly(
            @PathVariable String id,
            @ModelAttribute UpdateRequisitionMonthlyRequest request) {
        try {
            // Validate ID
            Optional<RequisitionMonthly> requisitionOptional = requisitionMonthlyRepository.findById(id);
            if (requisitionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Requisition not found for ID: " + id);
            }

            RequisitionMonthly requisition = requisitionOptional.get();

            // Validate oldSAPCode
            if (request.getOldSAPCode() != null && !request.getOldSAPCode().isEmpty()) {
                requisition.setOldSAPCode(request.getOldSAPCode());
            }

            // Validate and set supplier
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                Optional<SupplierProduct> supplierOptional = supplierRepository.findById(request.getSupplierId());
                if (supplierOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Supplier not found for supplierId: " + request.getSupplierId());
                }
                SupplierProduct supplier = supplierOptional.get();
                requisition.setSupplierId(request.getSupplierId());
                requisition.setSupplierName(supplier.getSupplierName());
                requisition.setUnit(supplier.getUnit());
                requisition.setPrice(supplier.getPrice() != null ? supplier.getPrice() : BigDecimal.ZERO);
                requisition.setGoodType(supplier.getGoodType() != null ? supplier.getGoodType(): "");
                requisition.setCurrency(supplier.getCurrency() != null ? supplier.getCurrency(): "VND");
            }

            // Update fields if provided
            if (request.getGroupId() != null) requisition.setGroupId(request.getGroupId());
            if (request.getItemDescriptionEN() != null) requisition.setItemDescriptionEN(request.getItemDescriptionEN());
            if (request.getItemDescriptionVN() != null) requisition.setItemDescriptionVN(request.getItemDescriptionVN());
            if (request.getOldSAPCode() != null) requisition.setOldSAPCode(request.getOldSAPCode());
            if (request.getHanaSAPCode() != null) requisition.setHanaSAPCode(request.getHanaSAPCode());
            if (request.getDailyMedInventory() != null) requisition.setDailyMedInventory(request.getDailyMedInventory());
            if (request.getSafeStock() != null) requisition.setSafeStock(request.getSafeStock());
            if (request.getFullDescription() != null) requisition.setFullDescription(request.getFullDescription());
            if (request.getReason() != null) requisition.setReason(request.getReason());
            if (request.getRemark() != null) requisition.setRemark(request.getRemark());
            if (request.getRemarkComparison() != null) requisition.setRemarkComparison(request.getRemarkComparison());
            if (request.getProductType1Id() != null) requisition.setProductType1Id(request.getProductType1Id());
            if (request.getProductType2Id() != null) requisition.setProductType2Id(request.getProductType2Id());

            // Update department requisitions
            List<DepartmentRequisitionMonthly> deptRequisitions = requisition.getDepartmentRequisitions();
            if (request.getDepartmentRequisitions() != null && !request.getDepartmentRequisitions().isEmpty()) {
                try {
                    List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptDTOs = objectMapper.readValue(
                            request.getDepartmentRequisitions(),
                            new TypeReference<List<DepartmentRequisitionMonthly.DepartmentRequestDTO>>() {}
                    );
                    deptRequisitions = deptDTOs.stream()
                            .map(dto -> new DepartmentRequisitionMonthly(
                                    dto.getId(),
                                    dto.getName(),
                                    dto.getQty(),
                                    dto.getBuy()
                            ))
                            .collect(Collectors.toList());
                    requisition.setDepartmentRequisitions(deptRequisitions);
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid departmentRequisitions JSON format: " + e.getMessage());
                }
            }

            // Calculate totalRequestQty
            BigDecimal totalRequestQty = deptRequisitions.stream()
                    .map(DepartmentRequisitionMonthly::getBuy)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            requisition.setTotalRequestQty(totalRequestQty);

            // Calculate useStockQty
            BigDecimal useStockQty = (requisition.getDailyMedInventory() != null ? requisition.getDailyMedInventory() : BigDecimal.ZERO)
                    .subtract(requisition.getSafeStock() != null ? requisition.getSafeStock() : BigDecimal.ZERO);
            requisition.setUseStockQty(useStockQty);

            // Calculate orderQty
            BigDecimal orderQty = totalRequestQty.subtract(useStockQty);
            requisition.setOrderQty(orderQty);

            // Calculate amount
            BigDecimal amount = (requisition.getPrice() != null ? requisition.getPrice() : BigDecimal.ZERO)
                    .multiply(orderQty);
            requisition.setAmount(amount);

            // Handle image deletion (JSON array string)
            List<String> currentImageUrls = requisition.getImageUrls() != null ? requisition.getImageUrls() : new ArrayList<>();
            if (request.getImagesToDelete() != null && !request.getImagesToDelete().isBlank()) {
                try {
                    List<String> imagesToDelete = objectMapper.readValue(request.getImagesToDelete(), new TypeReference<List<String>>() {});
                    for (String imageUrl : imagesToDelete) {
                        if (imageUrl != null && !imageUrl.isBlank() && currentImageUrls.contains(imageUrl)) {
                            try {
                                String filePath = imageUrl.startsWith("/uploads/") ? "." + imageUrl : imageUrl;
                                Path path = Paths.get(filePath);
                                if (Files.exists(path)) {
                                    Files.delete(path);
                                    System.out.println("Deleted image: " + filePath);
                                }
                                currentImageUrls.remove(imageUrl);
                            } catch (IOException e) {
                                System.err.println("Error deleting image: " + imageUrl + ", error: " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid imagesToDelete JSON format: " + e.getMessage());
                }
            }

            // Handle new image uploads
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        if (imageUrl != null) {
                            currentImageUrls.add(imageUrl);
                        }
                    }
                }
            }

            // Validate image count
            if (currentImageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Maximum 10 images allowed.");
            }
            requisition.setImageUrls(currentImageUrls);

            // Update timestamp
            requisition.setUpdatedDate(LocalDateTime.now());

            // Save to database
            RequisitionMonthly updatedRequisition = requisitionMonthlyRepository.save(requisition);

            // Convert to DTO for response
            UpdateRequisitionMonthlyDTO responseDTO = new UpdateRequisitionMonthlyDTO(
                    updatedRequisition.getId(),
                    updatedRequisition.getGroupId(),
                    updatedRequisition.getProductType1Id(),
                    updatedRequisition.getProductType2Id(),
                    updatedRequisition.getItemDescriptionEN(),
                    updatedRequisition.getItemDescriptionVN(),
                    updatedRequisition.getOldSAPCode(),
                    updatedRequisition.getHanaSAPCode(),
                    updatedRequisition.getUnit(),
                    updatedRequisition.getDepartmentRequisitions(),
                    updatedRequisition.getDailyMedInventory(),
                    updatedRequisition.getSafeStock(),
                    updatedRequisition.getTotalRequestQty(),
                    updatedRequisition.getUseStockQty(),
                    updatedRequisition.getOrderQty(),
                    updatedRequisition.getAmount(),
                    updatedRequisition.getPrice(),
                    updatedRequisition.getSupplierName(),
                    updatedRequisition.getCreatedDate(),
                    updatedRequisition.getUpdatedDate(),
                    updatedRequisition.getFullDescription(),
                    updatedRequisition.getReason(),
                    updatedRequisition.getRemark(),
                    updatedRequisition.getRemarkComparison(),
                    updatedRequisition.getImageUrls()
            );

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }


    @GetMapping("/requisition-monthly/{id}")
    public ResponseEntity<?> getRequisitionMonthlyById(@PathVariable String id) {
        try {
            Optional<RequisitionMonthly> requisitionOptional = requisitionMonthlyRepository.findById(id);
            if (requisitionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Requisition not found for ID: " + id);
            }

            RequisitionMonthly requisition = requisitionOptional.get();

            // Resolve ProductType1 name
            if (requisition.getProductType1Id() != null && !requisition.getProductType1Id().isEmpty()) {
                ProductType1 productType1 = productType1Service.getById(requisition.getProductType1Id());
                requisition.setProductType1Name(productType1 != null ? productType1.getName() : "Unknown");
            }

            // Resolve ProductType2 name
            if (requisition.getProductType2Id() != null && !requisition.getProductType2Id().isEmpty()) {
                ProductType2 productType2 = productType2Service.getById(requisition.getProductType2Id());
                requisition.setProductType2Name(productType2 != null ? productType2.getName() : "Unknown");
            }

            return ResponseEntity.ok(requisition);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/search/comparison-monthly")
    public ResponseEntity<MonthlyComparisonRequisitionResponseDTO> searchComparisonMonthly(
            @RequestParam String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String hanaSapCode,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "false") Boolean filter) {

        // Fetch GroupSummaryRequisition once using groupId
        Optional<GroupSummaryRequisition> group = groupSummaryRequisitionService.getGroupSummaryRequisitionById(groupId);
        String currency = group.isPresent() && group.get().getCurrency() != null ? group.get().getCurrency() : "";

        List<RequisitionMonthly> requisitions = requisitionMonthlyRepository.findByGroupId(groupId);

        List<RequisitionMonthly> filteredRequisitions = requisitions;
        if (Boolean.TRUE.equals(filter)) {
            filteredRequisitions = requisitions.stream()
                    .filter(req -> {
                        boolean matches = true;

                        String reqProductType1Name = "";
                        if (req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()) {
                            ProductType1 productType1 = productType1Service.getById(req.getProductType1Id());
                            reqProductType1Name = productType1 != null ? productType1.getName() : "";
                        }

                        String reqProductType2Name = "";
                        if (req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()) {
                            ProductType2 productType2 = productType2Service.getById(req.getProductType2Id());
                            reqProductType2Name = productType2 != null ? productType2.getName() : "";
                        }

                        List<String> deptNames = req.getDepartmentRequisitions() != null ?
                                req.getDepartmentRequisitions().stream()
                                        .filter(dept -> dept != null && dept.getName() != null)
                                        .map(DepartmentRequisitionMonthly::getName)
                                        .collect(Collectors.toList()) : Collections.emptyList();

                        if (productType1Name != null && !productType1Name.isEmpty()) {
                            matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                        }
                        if (productType2Name != null && !productType2Name.isEmpty()) {
                            matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                        }
                        if (englishName != null && !englishName.isEmpty()) {
                            matches = matches && req.getItemDescriptionEN() != null &&
                                    req.getItemDescriptionEN().toLowerCase().contains(englishName.toLowerCase());
                        }
                        if (vietnameseName != null && !vietnameseName.isEmpty()) {
                            matches = matches && req.getItemDescriptionVN() != null &&
                                    req.getItemDescriptionVN().toLowerCase().contains(vietnameseName.toLowerCase());
                        }
                        if (oldSapCode != null && !oldSapCode.isEmpty()) {
                            matches = matches && req.getOldSAPCode() != null &&
                                    req.getOldSAPCode().toLowerCase().contains(oldSapCode.toLowerCase());
                        }
                        if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                            matches = matches && req.getHanaSAPCode() != null &&
                                    req.getHanaSAPCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                        }
                        if (unit != null && !unit.isEmpty()) {
                            String reqUnit = req.getUnit() != null ? req.getUnit() : "";
                            matches = matches && reqUnit.toLowerCase().contains(unit.toLowerCase());
                        }
                        if (departmentName != null && !departmentName.isEmpty()) {
                            matches = matches && deptNames.stream()
                                    .anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                        }

                        return matches;
                    })
                    .collect(Collectors.toList());
        }

        filteredRequisitions.sort((req1, req2) -> {
            LocalDateTime date1 = req1.getUpdatedDate() != null ? req1.getUpdatedDate() :
                    req1.getCreatedDate() != null ? req1.getCreatedDate() : LocalDateTime.MIN;
            LocalDateTime date2 = req2.getUpdatedDate() != null ? req2.getUpdatedDate() :
                    req2.getCreatedDate() != null ? req2.getCreatedDate() : LocalDateTime.MIN;
            return date2.compareTo(date1);
        });

        List<MonthlyComparisonRequisitionDTO> dtoList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalAmtDifference = BigDecimal.ZERO;
        BigDecimal totalDifferencePercentage = BigDecimal.ZERO;

        for (RequisitionMonthly req : filteredRequisitions) {
            // Pass currency to convertToComparisonDTO
            MonthlyComparisonRequisitionDTO dto = convertToComparisonDTO(req, currency);
            dtoList.add(dto);
            if (dto.getAmount() != null) {
                totalAmount = totalAmount.add(dto.getAmount());
            }
            if (dto.getAmtDifference() != null) {
                totalAmtDifference = totalAmtDifference.add(dto.getAmtDifference());
            }
            if (dto.getPercentage() != null) {
                totalDifferencePercentage = totalDifferencePercentage.add(dto.getPercentage());
            }
        }

        MonthlyComparisonRequisitionResponseDTO response = new MonthlyComparisonRequisitionResponseDTO(
                dtoList,
                totalAmount,
                totalAmtDifference,
                totalDifferencePercentage
        );

        return ResponseEntity.ok(response);
    }

    private MonthlyComparisonRequisitionDTO convertToComparisonDTO(RequisitionMonthly req, String currency) {
        List<MonthlyComparisonRequisitionDTO.SupplierDTO> supplierDTOs = new ArrayList<>();

        String sapCode = req.getOldSAPCode() != null && !req.getOldSAPCode().isEmpty() ? req.getOldSAPCode() : null;
        String selectedSupplierId = req.getSupplierId();

        String unit = req.getUnit() != null ? req.getUnit() : "";
        String goodtype = ""; // Default goodtype

        if (sapCode != null && !sapCode.isEmpty()) {
            // Use currency passed from searchComparisonMonthly
            List<SupplierProduct> suppliers = supplierProductRepository.findBySapCodeAndCurrency(sapCode, currency);

            supplierDTOs = suppliers.stream()
                    .map(sp -> new MonthlyComparisonRequisitionDTO.SupplierDTO(
                            sp.getPrice(),
                            sp.getSupplierName(),
                            selectedSupplierId != null && !selectedSupplierId.isEmpty() && selectedSupplierId.equals(sp.getId()) ? 1 : 0,
                            sp.getUnit()))
                    .sorted(Comparator.comparing(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                    .collect(Collectors.toList());

            if (selectedSupplierId != null && !selectedSupplierId.isEmpty()) {
                Optional<SupplierProduct> selectedSupplier = suppliers.stream()
                        .filter(sp -> sp.getId().equals(selectedSupplierId))
                        .findFirst();
                if (selectedSupplier.isPresent()) {
                    unit = selectedSupplier.get().getUnit() != null ? selectedSupplier.get().getUnit() : unit;
                    currency = selectedSupplier.get().getCurrency() != null ? selectedSupplier.get().getCurrency() : currency; // Update currency if available
                    goodtype = selectedSupplier.get().getGoodType() != null ? selectedSupplier.get().getGoodType() : "";
                }
            }
        }

        BigDecimal price = null;
        BigDecimal highestPrice = null;
        if (selectedSupplierId != null && !selectedSupplierId.isEmpty() && !supplierDTOs.isEmpty()) {
            price = supplierDTOs.stream()
                    .filter(dto -> dto.getIsSelected() == 1)
                    .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            highestPrice = supplierDTOs.stream()
                    .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);
        }

        BigDecimal orderQty = req.getOrderQty() != null ? req.getOrderQty() : BigDecimal.ZERO;
        BigDecimal amount = price != null ? price.multiply(orderQty) : null;
        BigDecimal amtDifference = (amount != null && highestPrice != null) ? amount.subtract(highestPrice.multiply(orderQty)) : null;
        BigDecimal percentage = (amount != null && amtDifference != null && amount.compareTo(BigDecimal.ZERO) != 0) ?
                amtDifference.divide(amount, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

        List<MonthlyComparisonRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequisitions() != null ?
                req.getDepartmentRequisitions().stream()
                        .filter(Objects::nonNull)
                        .map(dept -> new MonthlyComparisonRequisitionDTO.DepartmentRequestDTO(
                                dept.getId(),
                                dept.getName(),
                                dept.getQty() != null ? dept.getQty() : 0,
                                dept.getBuy() != null ? dept.getBuy() : 0))
                        .collect(Collectors.toList()) : Collections.emptyList();

        String type1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty() ?
                productType1Service.getById(req.getProductType1Id()).getName() : "Unknown";
        String type2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty() ?
                productType2Service.getById(req.getProductType2Id()).getName() : "Unknown";

        BigDecimal dailyMedInventory = req.getDailyMedInventory() != null ? req.getDailyMedInventory() : null;
        BigDecimal totalRequestQty = req.getTotalRequestQty() != null ? req.getTotalRequestQty() : null;
        BigDecimal safeStock = req.getSafeStock() != null ? req.getSafeStock() : null;
        BigDecimal useStockQty = req.getUseStockQty() != null ? req.getUseStockQty() : null;

        return new MonthlyComparisonRequisitionDTO(
                req.getItemDescriptionEN(),
                req.getItemDescriptionVN(),
                req.getOldSAPCode(),
                req.getHanaSAPCode(),
                supplierDTOs,
                req.getRemarkComparison(),
                departmentRequests,
                amount,
                amtDifference,
                percentage,
                highestPrice,
                req.getProductType1Id(),
                req.getProductType2Id(),
                type1Name,
                type2Name,
                unit,
                dailyMedInventory,
                totalRequestQty,
                safeStock,
                useStockQty,
                orderQty,
                price,
                currency,
                goodtype
        );
    }
}