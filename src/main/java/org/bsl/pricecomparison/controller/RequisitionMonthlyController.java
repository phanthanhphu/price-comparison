package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.bsl.pricecomparison.dto.*;
import org.bsl.pricecomparison.enums.RequisitionType;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.DepartmentRepository;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private DepartmentRepository departmentRepository;

    private static final String UPLOAD_DIR = "./uploads/";



    @PostMapping(value = "/requisition-monthly", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addRequisitionMonthly(
            @RequestParam("email") String email,
            @ModelAttribute CreateRequisitionMonthlyRequest request
    ) {
        try {
            // ✅ Validate email
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "email is required"));
            }

            // Validate oldSAPCode
            if (request.getOldSAPCode() == null || request.getOldSAPCode().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "oldSAPCode is required"));
            }

            if (request.getGroupId() != null && request.getOldSAPCode() != null) {
                Optional<RequisitionMonthly> existing = requisitionMonthlyRepository
                        .findByGroupIdAndOldSAPCode(request.getGroupId(), request.getOldSAPCode());

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

            // Get dailyMedInventory + safeStock
            BigDecimal dailyMedInventory = request.getDailyMedInventory() != null ? request.getDailyMedInventory() : BigDecimal.ZERO;
            BigDecimal safeStock = request.getSafeStock() != null ? request.getSafeStock() : BigDecimal.ZERO;

            // Process department requisitions
            List<DepartmentRequisitionMonthly> deptRequisitions = new ArrayList<>();
            BigDecimal totalRequestQty = BigDecimal.ZERO;

            if (request.getDepartmentRequisitions() != null && !request.getDepartmentRequisitions().isEmpty()) {
                try {
                    List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptDTOs = objectMapper.readValue(
                            request.getDepartmentRequisitions(),
                            new TypeReference<List<DepartmentRequisitionMonthly.DepartmentRequestDTO>>() {}
                    );

                    for (DepartmentRequisitionMonthly.DepartmentRequestDTO dto : deptDTOs) {
                        BigDecimal qty = dto.getQty() != null ? dto.getQty() : BigDecimal.ZERO;
                        BigDecimal buy = dto.getBuy() != null ? dto.getBuy() : BigDecimal.ZERO;

                        DepartmentRequisitionMonthly deptReq = new DepartmentRequisitionMonthly(
                                dto.getId(),
                                dto.getName(),
                                qty,
                                buy
                        );
                        deptRequisitions.add(deptReq);
                        totalRequestQty = totalRequestQty.add(qty);
                    }
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid departmentRequisitions JSON format: " + e.getMessage()));
                }
            }

            // Create entity
            RequisitionMonthly requisition = new RequisitionMonthly();
            requisition.setGroupId(request.getGroupId());
            requisition.setItemDescriptionEN(request.getItemDescriptionEN());
            requisition.setItemDescriptionVN(request.getItemDescriptionVN());
            requisition.setOldSAPCode(request.getOldSAPCode());
            requisition.setHanaSAPCode(request.getHanaSAPCode());

            requisition.setUnit(supplier != null ? supplier.getUnit() : null);
            requisition.setDailyMedInventory(dailyMedInventory);
            requisition.setSafeStock(safeStock);

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

            requisition.setType(RequisitionType.MONTHLY);

            // ✅ LƯU EMAIL CREATE
            requisition.setCreatedByEmail(email);
            requisition.setUpdatedByEmail(email);

            requisition.setCreatedDate(LocalDateTime.now());
            requisition.setUpdatedDate(LocalDateTime.now());

            requisition.setDepartmentRequisitions(deptRequisitions);

            requisition.setTotalRequestQty(totalRequestQty);
            requisition.setUseStockQty(BigDecimal.ZERO);
            requisition.setOrderQty(dailyMedInventory);
            requisition.setAmount(requisition.getPrice().multiply(dailyMedInventory));

            // Handle image uploads
            List<String> imageUrls = new ArrayList<>();
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        if (imageUrl != null) imageUrls.add(imageUrl);
                    }
                }
            }

            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }
            requisition.setImageUrls(imageUrls);

            RequisitionMonthly savedRequisition = requisitionMonthlyRepository.save(requisition);

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
                            resolvedProductType1Name = productType1 != null ? productType1.getName() : "";
                        }

                        String resolvedProductType2Name = null;
                        if (req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()) {
                            ProductType2 productType2 = productType2Service.getById(req.getProductType2Id());
                            resolvedProductType2Name = productType2 != null ? productType2.getName() : "";
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

        List<RequisitionMonthlyDTO> requisitionDTOs = requisitions.stream()
                .map(req -> {
                    // Resolve product type names (ưu tiên resolve từ service, fallback sang field lưu sẵn)
                    String resolvedProductType1Name = req.getProductType1Name();
                    if (req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()) {
                        ProductType1 pt1 = productType1Service.getById(req.getProductType1Id());
                        if (pt1 != null && pt1.getName() != null) resolvedProductType1Name = pt1.getName();
                    }

                    String resolvedProductType2Name = req.getProductType2Name();
                    if (req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()) {
                        ProductType2 pt2 = productType2Service.getById(req.getProductType2Id());
                        if (pt2 != null && pt2.getName() != null) resolvedProductType2Name = pt2.getName();
                    }

                    RequisitionMonthlyDTO dto = new RequisitionMonthlyDTO();

                    dto.setId(req.getId());
                    dto.setGroupId(req.getGroupId());

                    dto.setProductType1Id(req.getProductType1Id());
                    dto.setProductType2Id(req.getProductType2Id());
                    dto.setProductType1Name(resolvedProductType1Name);
                    dto.setProductType2Name(resolvedProductType2Name);

                    dto.setItemDescriptionEN(req.getItemDescriptionEN());
                    dto.setItemDescriptionVN(req.getItemDescriptionVN());
                    dto.setOldSAPCode(req.getOldSAPCode());
                    dto.setHanaSAPCode(req.getHanaSAPCode());
                    dto.setUnit(req.getUnit());

                    dto.setDepartmentRequisitions(req.getDepartmentRequisitions());

                    dto.setDailyMedInventory(req.getDailyMedInventory());
                    dto.setStock(req.getStock());
                    dto.setTotalRequestQty(req.getTotalRequestQty());
                    dto.setSafeStock(req.getSafeStock());
                    dto.setUseStockQty(req.getUseStockQty());
                    dto.setOrderQty(req.getOrderQty());
                    dto.setAmount(req.getAmount());
                    dto.setPrice(req.getPrice());

                    dto.setCurrency(req.getCurrency());
                    dto.setGoodType(req.getGoodType());

                    dto.setSupplierId(req.getSupplierId());
                    dto.setSupplierName(req.getSupplierName());

                    dto.setCreatedDate(req.getCreatedDate());
                    dto.setUpdatedDate(req.getUpdatedDate());

                    // ===== AUDIT EMAIL =====
                    dto.setCreatedByEmail(req.getCreatedByEmail());
                    dto.setUpdatedByEmail(req.getUpdatedByEmail());
                    dto.setCompletedByEmail(req.getCompletedByEmail());
                    dto.setUncompletedByEmail(req.getUncompletedByEmail());

                    // ===== COMPLETION =====
                    dto.setCompletedDate(req.getCompletedDate());
                    dto.setIsCompleted(req.getIsCompleted());

                    dto.setImageUrls(req.getImageUrls());
                    dto.setFullDescription(req.getFullDescription());
                    dto.setReason(req.getReason());
                    dto.setRemark(req.getRemark());
                    dto.setRemarkComparison(req.getRemarkComparison());

                    dto.setType(req.getType());

                    dto.setSupplierComparisonList(req.getSupplierComparisonList());
                    dto.setStatusBestPrice(req.getStatusBestPrice());

                    return dto;
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
            @RequestParam("email") String email,
            @ModelAttribute UpdateRequisitionMonthlyRequest request
    ) {
        try {
            // ✅ Validate email (giống create)
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "email is required"));
            }

            // 1) Validate ID
            Optional<RequisitionMonthly> requisitionOptional = requisitionMonthlyRepository.findById(id);
            if (requisitionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Requisition not found for ID: " + id));
            }

            RequisitionMonthly requisition = requisitionOptional.get();

            // 2) ✅ UNIQUE CHECK giống create: groupId + oldSAPCode (chỉ check khi có thay đổi)
            String nextGroupId = (request.getGroupId() != null && !request.getGroupId().isBlank())
                    ? request.getGroupId()
                    : requisition.getGroupId();

            String nextOldSap = (request.getOldSAPCode() != null && !request.getOldSAPCode().isBlank())
                    ? request.getOldSAPCode()
                    : requisition.getOldSAPCode();

            if (nextGroupId != null && !nextGroupId.isBlank() && nextOldSap != null && !nextOldSap.isBlank()) {
                Optional<RequisitionMonthly> existing = requisitionMonthlyRepository
                        .findByGroupIdAndOldSAPCode(nextGroupId, nextOldSap);

                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "Duplicate entry: groupId and oldSapCode must be unique together."));
                }
            }

            // 3) Update oldSAPCode
            if (request.getOldSAPCode() != null && !request.getOldSAPCode().isEmpty()) {
                requisition.setOldSAPCode(request.getOldSAPCode());
            }

            // 4) Update supplier info if provided
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                Optional<SupplierProduct> supplierOptional = supplierRepository.findById(request.getSupplierId());
                if (supplierOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Supplier not found for supplierId: " + request.getSupplierId()));
                }

                SupplierProduct supplier = supplierOptional.get();

                requisition.setSupplierId(request.getSupplierId());
                requisition.setSupplierName(supplier.getSupplierName());
                requisition.setUnit(supplier.getUnit());

                requisition.setPrice(supplier.getPrice() != null ? supplier.getPrice() : BigDecimal.ZERO);
                requisition.setGoodType(supplier.getGoodType() != null ? supplier.getGoodType() : "");
                requisition.setCurrency(supplier.getCurrency() != null ? supplier.getCurrency() : "VND");

                // nếu bạn muốn update theo supplier
                requisition.setProductType1Id(supplier.getProductType1Id());
                requisition.setProductType2Id(supplier.getProductType2Id());
            }

            // 5) Update basic fields
            if (request.getGroupId() != null) requisition.setGroupId(request.getGroupId());
            if (request.getItemDescriptionEN() != null) requisition.setItemDescriptionEN(request.getItemDescriptionEN());
            if (request.getItemDescriptionVN() != null) requisition.setItemDescriptionVN(request.getItemDescriptionVN());
            if (request.getHanaSAPCode() != null) requisition.setHanaSAPCode(request.getHanaSAPCode());
            if (request.getDailyMedInventory() != null) requisition.setDailyMedInventory(request.getDailyMedInventory());

            if (request.getFullDescription() != null) requisition.setFullDescription(request.getFullDescription());
            if (request.getReason() != null) requisition.setReason(request.getReason());
            if (request.getRemark() != null) requisition.setRemark(request.getRemark());
            if (request.getRemarkComparison() != null) requisition.setRemarkComparison(request.getRemarkComparison());

            requisition.setType(RequisitionType.MONTHLY);

            // 6) Update department requisitions (fix null-safety)
            List<DepartmentRequisitionMonthly> deptRequisitions =
                    requisition.getDepartmentRequisitions() != null ? requisition.getDepartmentRequisitions() : new ArrayList<>();

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
                                    dto.getQty() != null ? dto.getQty() : BigDecimal.ZERO,
                                    dto.getBuy() != null ? dto.getBuy() : BigDecimal.ZERO
                            ))
                            .collect(Collectors.toList());

                    requisition.setDepartmentRequisitions(deptRequisitions);

                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid departmentRequisitions JSON format: " + e.getMessage()));
                }
            }

            // 7) Re-calc totals
            BigDecimal medConfirmedQty = requisition.getDailyMedInventory() != null
                    ? requisition.getDailyMedInventory()
                    : BigDecimal.ZERO;

            BigDecimal totalRequestQtyFromDepts = deptRequisitions.stream()
                    .filter(x -> x != null && x.getQty() != null)
                    .map(DepartmentRequisitionMonthly::getQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            requisition.setTotalRequestQty(totalRequestQtyFromDepts);
            requisition.setUseStockQty(BigDecimal.ZERO);
            requisition.setOrderQty(medConfirmedQty);

            BigDecimal amount = (requisition.getPrice() != null ? requisition.getPrice() : BigDecimal.ZERO)
                    .multiply(medConfirmedQty);
            requisition.setAmount(amount);

            // 8) Handle images deletion
            List<String> currentImageUrls = requisition.getImageUrls() != null ? requisition.getImageUrls() : new ArrayList<>();

            if (request.getImagesToDelete() != null && !request.getImagesToDelete().isBlank()) {
                try {
                    List<String> imagesToDelete = objectMapper.readValue(
                            request.getImagesToDelete(),
                            new TypeReference<List<String>>() {}
                    );

                    for (String imageUrl : imagesToDelete) {
                        if (imageUrl != null && !imageUrl.isBlank() && currentImageUrls.contains(imageUrl)) {
                            try {
                                String filePath = imageUrl.startsWith("/uploads/") ? "." + imageUrl : imageUrl;
                                Path path = Paths.get(filePath);
                                if (Files.exists(path)) Files.delete(path);
                                currentImageUrls.remove(imageUrl);
                            } catch (IOException e) {
                                System.err.println("Error deleting image: " + imageUrl + ", error: " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid imagesToDelete JSON format: " + e.getMessage()));
                }
            }

            // 9) Handle new image uploads
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        if (imageUrl != null) currentImageUrls.add(imageUrl);
                    }
                }
            }

            if (currentImageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }
            requisition.setImageUrls(currentImageUrls);

            // ✅ 10) LƯU EMAIL UPDATE (đúng như create)
            requisition.setUpdatedByEmail(email);

            // 11) Update timestamp & save
            requisition.setUpdatedDate(LocalDateTime.now());
            RequisitionMonthly updatedRequisition = requisitionMonthlyRepository.save(requisition);

            // ✅ trả về giống create cho “ăn chắc”: có full field (kể cả updatedByEmail)
            return ResponseEntity.ok(
                    Map.of(
                            "message", "Requisition updated successfully",
                            "data", updatedRequisition
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error: " + e.getMessage()));
        }
    }

    @PatchMapping("/requisition-monthly/{id}/best-price")
    public ResponseEntity<?> updateBestPriceAndRemark(
            @PathVariable String id,
            @RequestBody UpdateStatusBestPriceRequest request) {

        try {
            // 1. Find requisition
            Optional<RequisitionMonthly> optional = requisitionMonthlyRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Requisition not found with ID: " + id);
            }

            RequisitionMonthly requisition = optional.get();

            // 2. Update fields if provided
            if (request.getStatusBestPrice() != null && !request.getStatusBestPrice().trim().isEmpty()) {
                requisition.setStatusBestPrice(request.getStatusBestPrice().trim());
            }

            if (request.getRemarkComparison() != null) {
                requisition.setRemarkComparison(request.getRemarkComparison());
            }

            // 3. Always update timestamp
            requisition.setUpdatedDate(LocalDateTime.now());

            // 4. Save
            requisitionMonthlyRepository.save(requisition);

            // 5. Return success message
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Updated successfully!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Update failed. Please try again.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error);
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
                requisition.setProductType1Name(productType1 != null ? productType1.getName() : "");
            }

            // Resolve ProductType2 name
            if (requisition.getProductType2Id() != null && !requisition.getProductType2Id().isEmpty()) {
                ProductType2 productType2 = productType2Service.getById(requisition.getProductType2Id());
                requisition.setProductType2Name(productType2 != null ? productType2.getName() : "");
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
            @RequestParam(defaultValue = "false") Boolean filter,
            @RequestParam(defaultValue = "false") Boolean removeDuplicateSuppliers
    ) {
        // ✅ currency lấy từ group (group luôn có currency)
        final String groupCurrency = groupSummaryRequisitionService
                .getGroupSummaryRequisitionById(groupId)
                .map(g -> g.getCurrency() != null ? g.getCurrency() : "")
                .orElse("")
                .trim();

        // ===== cache để giảm query lặp + cache last purchase =====
        final Map<String, List<SupplierProduct>> supplierListCache = new HashMap<>();
        final Map<String, RequisitionMonthly> lastPurchaseCache = new HashMap<>();

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

                        List<String> deptNames = req.getDepartmentRequisitions() != null
                                ? req.getDepartmentRequisitions().stream()
                                .filter(dept -> dept != null && dept.getName() != null)
                                .map(DepartmentRequisitionMonthly::getName)
                                .collect(Collectors.toList())
                                : Collections.emptyList();

                        if (productType1Name != null && !productType1Name.isEmpty()) {
                            matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                        }
                        if (productType2Name != null && !productType2Name.isEmpty()) {
                            matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                        }
                        if (englishName != null && !englishName.isEmpty()) {
                            matches = matches && req.getItemDescriptionEN() != null
                                    && req.getItemDescriptionEN().toLowerCase().contains(englishName.toLowerCase());
                        }
                        if (vietnameseName != null && !vietnameseName.isEmpty()) {
                            matches = matches && req.getItemDescriptionVN() != null
                                    && req.getItemDescriptionVN().toLowerCase().contains(vietnameseName.toLowerCase());
                        }
                        if (oldSapCode != null && !oldSapCode.isEmpty()) {
                            matches = matches && req.getOldSAPCode() != null
                                    && req.getOldSAPCode().toLowerCase().contains(oldSapCode.toLowerCase());
                        }
                        if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                            matches = matches && req.getHanaSAPCode() != null
                                    && req.getHanaSAPCode().toLowerCase().contains(hanaSapCode.toLowerCase());
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
            LocalDateTime date1 = req1.getUpdatedDate() != null ? req1.getUpdatedDate()
                    : (req1.getCreatedDate() != null ? req1.getCreatedDate() : LocalDateTime.MIN);
            LocalDateTime date2 = req2.getUpdatedDate() != null ? req2.getUpdatedDate()
                    : (req2.getCreatedDate() != null ? req2.getCreatedDate() : LocalDateTime.MIN);
            return date2.compareTo(date1);
        });

        List<MonthlyComparisonRequisitionDTO> dtoList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalAmtDifference = BigDecimal.ZERO;
        BigDecimal totalDifferencePercentage = BigDecimal.ZERO;

        for (RequisitionMonthly req : filteredRequisitions) {
            MonthlyComparisonRequisitionDTO dto = convertToComparisonDTO(
                    req,
                    groupCurrency,
                    removeDuplicateSuppliers,
                    supplierListCache,
                    lastPurchaseCache
            );
            dtoList.add(dto);

            if (dto.getAmount() != null) totalAmount = totalAmount.add(dto.getAmount());
            if (dto.getAmtDifference() != null) totalAmtDifference = totalAmtDifference.add(dto.getAmtDifference());
            if (dto.getPercentage() != null) totalDifferencePercentage = totalDifferencePercentage.add(dto.getPercentage());
        }

        return ResponseEntity.ok(new MonthlyComparisonRequisitionResponseDTO(
                dtoList,
                totalAmount,
                totalAmtDifference,
                totalDifferencePercentage
        ));
    }

    private MonthlyComparisonRequisitionDTO convertToComparisonDTO(
            RequisitionMonthly req,
            String groupCurrency,
            Boolean removeDuplicateSuppliers,
            Map<String, List<SupplierProduct>> supplierListCache,
            Map<String, RequisitionMonthly> lastPurchaseCache
    ) {
        List<MonthlyComparisonRequisitionDTO.SupplierDTO> supplierDTOs = new ArrayList<>();

        // ✅ codeKey: ưu tiên oldSAPCode, thiếu thì hana
        String codeKey = null;
        boolean useOld = false;
        if (req.getOldSAPCode() != null && !req.getOldSAPCode().isBlank()) {
            codeKey = req.getOldSAPCode().trim();
            useOld = true;
        } else if (req.getHanaSAPCode() != null && !req.getHanaSAPCode().isBlank()) {
            codeKey = req.getHanaSAPCode().trim();
        }

        String selectedSupplierId = (req.getSupplierId() != null && !req.getSupplierId().isBlank())
                ? req.getSupplierId().trim()
                : null;

        String unit = req.getUnit() != null ? req.getUnit() : "";
        String goodtype = "";
        BigDecimal price = null;

        // giữ lại currency theo group để:
        // (1) lookup supplier list theo groupCurrency
        // (2) query last purchase theo groupCurrency
        final String finalGroupCurrency = groupCurrency != null ? groupCurrency.trim() : "";

        // === Supplier list: codeKey + currency(group) ===
        if (selectedSupplierId != null && codeKey != null && !codeKey.isBlank() && !finalGroupCurrency.isBlank()) {

            final String supplierListKey = codeKey + "|" + finalGroupCurrency;

            String finalCodeKey = codeKey;
            List<SupplierProduct> suppliers = supplierListCache.computeIfAbsent(supplierListKey, k ->
                    supplierProductRepository.findBySapCodeAndCurrencyIgnoreCase(finalCodeKey, finalGroupCurrency)
            );

            if (suppliers != null && !suppliers.isEmpty()) {

                // build all suppliers
                List<MonthlyComparisonRequisitionDTO.SupplierDTO> allSuppliers = suppliers.stream()
                        .map(sp -> new MonthlyComparisonRequisitionDTO.SupplierDTO(
                                sp.getPrice(),
                                sp.getSupplierName(),
                                selectedSupplierId.equals(sp.getId()) ? 1 : 0,
                                sp.getUnit(),
                                false
                        ))
                        .collect(Collectors.toList());

                // remove dup theo supplierName (ưu tiên selected, nếu không thì lấy giá thấp nhất)
                if (Boolean.TRUE.equals(removeDuplicateSuppliers)) {
                    Map<String, MonthlyComparisonRequisitionDTO.SupplierDTO> unique = new LinkedHashMap<>();
                    for (MonthlyComparisonRequisitionDTO.SupplierDTO s : allSuppliers) {
                        String key = s.getSupplierName();
                        MonthlyComparisonRequisitionDTO.SupplierDTO exist = unique.get(key);

                        if (exist == null) {
                            unique.put(key, s);
                        } else {
                            if (s.getIsSelected() != null && s.getIsSelected() == 1) {
                                unique.put(key, s);
                            } else if ((exist.getIsSelected() == null || exist.getIsSelected() == 0)
                                    && s.getPrice() != null
                                    && (exist.getPrice() == null || s.getPrice().compareTo(exist.getPrice()) < 0)) {
                                unique.put(key, s);
                            }
                        }
                    }
                    supplierDTOs = new ArrayList<>(unique.values());
                } else {
                    supplierDTOs = allSuppliers;
                }

                supplierDTOs = supplierDTOs.stream()
                        .sorted(Comparator.comparing(
                                MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice,
                                Comparator.nullsLast(BigDecimal::compareTo)))
                        .collect(Collectors.toList());

                // set selected info
                SupplierProduct selectedSupplier = suppliers.stream()
                        .filter(sp -> selectedSupplierId.equals(sp.getId()))
                        .findFirst()
                        .orElse(null);

                if (selectedSupplier != null) {
                    unit = selectedSupplier.getUnit() != null ? selectedSupplier.getUnit() : unit;
                    goodtype = selectedSupplier.getGoodType() != null ? selectedSupplier.getGoodType() : goodtype;
                    price = selectedSupplier.getPrice();
                }
            }
        }

        // fallback price
        if (price == null && req.getPrice() != null) {
            price = req.getPrice();
        }

        BigDecimal highestPrice = supplierDTOs.stream()
                .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal orderQty = req.getOrderQty() != null ? req.getOrderQty() : BigDecimal.ZERO;

        BigDecimal amount = price != null ? price.multiply(orderQty) : BigDecimal.ZERO;
        BigDecimal highestAmount = highestPrice != null ? highestPrice.multiply(orderQty) : BigDecimal.ZERO;
        BigDecimal amtDifference = amount.subtract(highestAmount);

        BigDecimal percentage = BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) != 0) {
            percentage = amtDifference.divide(amount, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        BigDecimal minPrice = supplierDTOs.stream()
                .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        Boolean isBestPrice = false;
        String statusBestPrice = req.getStatusBestPrice();
        if (statusBestPrice == null || statusBestPrice.trim().isEmpty() || "EMPTY".equalsIgnoreCase(statusBestPrice.trim())) {
            isBestPrice = price != null && minPrice != null && price.compareTo(minPrice) == 0;
        } else {
            isBestPrice = "Yes".equalsIgnoreCase(statusBestPrice.trim());
        }

        List<MonthlyComparisonRequisitionDTO.DepartmentRequestDTO> departmentRequests =
                req.getDepartmentRequisitions() != null
                        ? req.getDepartmentRequisitions().stream()
                        .filter(Objects::nonNull)
                        .map(dept -> new MonthlyComparisonRequisitionDTO.DepartmentRequestDTO(
                                dept.getId(),
                                dept.getName(),
                                dept.getQty() != null ? dept.getQty() : BigDecimal.ZERO,
                                dept.getBuy() != null ? dept.getBuy() : BigDecimal.ZERO
                        ))
                        .collect(Collectors.toList())
                        : Collections.emptyList();

        String type1Name = (req.getProductType1Id() != null && !req.getProductType1Id().isEmpty())
                ? productType1Service.getById(req.getProductType1Id()).getName()
                : "";
        String type2Name = (req.getProductType2Id() != null && !req.getProductType2Id().isEmpty())
                ? productType2Service.getById(req.getProductType2Id()).getName()
                : "";

        BigDecimal dailyMedInventory = req.getDailyMedInventory();
        BigDecimal totalRequestQty = req.getTotalRequestQty();
        BigDecimal safeStock = req.getSafeStock();
        BigDecimal useStockQty = req.getUseStockQty();

        // dto currency vẫn trả về groupCurrency (đúng logic của group)
        MonthlyComparisonRequisitionDTO dto = new MonthlyComparisonRequisitionDTO(
                req.getId(),
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
                isBestPrice,
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
                finalGroupCurrency,
                goodtype
        );

        // ✅ LAST PURCHASE: supplierId + (old|hana) + currency(group) + isCompleted=true, exclude current id
        applyLastPurchaseInfoMonthly(
                dto,
                req.getId(),
                selectedSupplierId,
                finalGroupCurrency,
                req.getOldSAPCode(),
                req.getHanaSAPCode(),
                useOld,
                lastPurchaseCache
        );

        return dto;
    }

    private void applyLastPurchaseInfoMonthly(
            MonthlyComparisonRequisitionDTO dto,
            String currentReqId,
            String supplierId,
            String currency,
            String oldSapCode,
            String hanaSapCode,
            boolean useOld,
            Map<String, RequisitionMonthly> lastPurchaseCache
    ) {
        if (dto == null) return;
        if (supplierId == null || supplierId.isBlank()) return;
        if (currency == null || currency.isBlank()) return;

        String idNot = (currentReqId == null || currentReqId.isBlank()) ? "__NONE__" : currentReqId.trim();

        String codeKey;
        String mode;
        if (useOld && oldSapCode != null && !oldSapCode.isBlank()) {
            codeKey = oldSapCode.trim();
            mode = "OLD";
        } else if (hanaSapCode != null && !hanaSapCode.isBlank()) {
            codeKey = hanaSapCode.trim();
            mode = "HANA";
        } else {
            return;
        }

        String cacheKey = supplierId.trim() + "|" + mode + "|" + codeKey + "|" + currency.trim() + "|" + idNot;

        RequisitionMonthly last = lastPurchaseCache.get(cacheKey);
        if (last == null && !lastPurchaseCache.containsKey(cacheKey)) {
            Optional<RequisitionMonthly> opt;

            if ("OLD".equals(mode)) {
                opt = requisitionMonthlyRepository
                        .findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByCompletedDateDesc(
                                supplierId.trim(), codeKey, currency.trim(), idNot
                        );
                if (opt.isEmpty()) {
                    opt = requisitionMonthlyRepository
                            .findFirstBySupplierIdAndOldSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByUpdatedDateDesc(
                                    supplierId.trim(), codeKey, currency.trim(), idNot
                            );
                }
            } else {
                opt = requisitionMonthlyRepository
                        .findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByCompletedDateDesc(
                                supplierId.trim(), codeKey, currency.trim(), idNot
                        );
                if (opt.isEmpty()) {
                    opt = requisitionMonthlyRepository
                            .findFirstBySupplierIdAndHanaSAPCodeAndCurrencyAndIsCompletedTrueAndIdNotOrderByUpdatedDateDesc(
                                    supplierId.trim(), codeKey, currency.trim(), idNot
                            );
                }
            }

            last = opt.orElse(null);
            // cache cả null để khỏi query lại
            lastPurchaseCache.put(cacheKey, last);
        }

        if (last == null) return;

        dto.setLastPurchaseSupplierName(last.getSupplierName());

        LocalDateTime lastDate = last.getCompletedDate();
        if (lastDate == null) lastDate = last.getUpdatedDate();
        if (lastDate == null) lastDate = last.getCreatedDate();
        dto.setLastPurchaseDate(lastDate);

        dto.setLastPurchasePrice(last.getPrice());
        dto.setLastPurchaseOrderQty(last.getOrderQty() != null ? last.getOrderQty() : BigDecimal.ZERO);
    }

    @PostMapping(value = "/requisition-monthly/upload-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload Requisition File",
            description = "Upload Excel .xlsx file. Data starts from **row 9 (N9)**, column B (Item VN). " +
                    "Columns: B=Item VN, C=Item EN, D=Hana SAP, G=Request, H=Inhand, I=Buy, J=Unit, M=Dept, N=Picture, O=Remarks"
    )
    public ResponseEntity<List<RequisitionMonthly>> uploadRequisitionFile(
            @Parameter(description = "Excel .xlsx file") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Group ID", required = true) @RequestParam("groupId") String groupId) {

        List<RequisitionMonthly> allMonthly = requisitionMonthlyRepository.findByGroupId(groupId);
        Set<String> existingSapCodes = allMonthly.stream()
                .map(RequisitionMonthly::getOldSAPCode)
                .filter(code -> code != null && !code.trim().isEmpty() && !"NEW".equalsIgnoreCase(code.trim()))
                .map(code -> code.trim().toUpperCase())
                .collect(Collectors.toSet());

        List<RequisitionMonthly> requisitions = new ArrayList<>();
        Map<String, Integer> sapCodeMap = new HashMap<>();     // <sapCode, index>
        Map<String, Integer> materialMap = new HashMap<>();    // <itemVN, index> for SAP = NEW

        if (file == null || file.isEmpty()) {
            return badRequest("No file uploaded.");
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            return badRequest("Only .xlsx files are allowed.");
        }

        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return badRequest("Sheet not found.");
            }

            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

            // === BẮT ĐẦU TỪ DÒNG 9 (index = 8) ===
            for (int i = 8; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // === CỘT B (Item VN) - BẮT BUỘC ===
                Cell itemVNCell = getMergedCellValue(sheet, i, 1, mergedRegions); // Cột B (index 1)
                if (itemVNCell == null) {
                    itemVNCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                }
                String itemVN = getCellValue(itemVNCell);

                // Bỏ qua nếu cột B rỗng
                if (itemVN == null || itemVN.trim().isEmpty()) {
                    continue;
                }

                // === CÁC CỘT KHÁC ===
                Cell itemENCell   = getMergedCellValue(sheet, i, 2, mergedRegions);  // Cột C
                Cell sapCell      = getMergedCellValue(sheet, i, 3, mergedRegions);  // Cột D
                Cell hanaCell     = getMergedCellValue(sheet, i, 4, mergedRegions);  // Cột E
                Cell requestCell  = getMergedCellValue(sheet, i, 8, mergedRegions);  // Cột I
                Cell unitCell     = getMergedCellValue(sheet, i, 9, mergedRegions);  // Cột J
                Cell deptCell     = getMergedCellValue(sheet, i, 10, mergedRegions); // Cột K
                Cell reasonCell   = getMergedCellValue(sheet, i, 14, mergedRegions); // Cột O (Remark)

                String itemEN   = getCellValue(itemENCell);

                // sapCode là String, dù Excel lưu số
                String sapCode  = getCellValue(sapCell);
                String hanaCode = getCellValue(hanaCell);

                BigDecimal requestQty = parseBigDecimal(requestCell);
                if (requestQty == null) requestQty = BigDecimal.ZERO;

                // Inhand và Buy không còn trong file → mặc định 0
                BigDecimal inhand = BigDecimal.ZERO;
//                BigDecimal buy = requestQty.subtract(inhand).max(BigDecimal.ZERO); // = requestQty
                BigDecimal buy = BigDecimal.ZERO;
                String unit     = getCellValue(unitCell);
                String deptName = getCellValue(deptCell);
                String reason   = getCellValue(reasonCell); // Remark → reason

                // === KIỂM TRA TRÙNG SAP CODE TRONG DB ===
                boolean isSapValid = sapCode != null && !sapCode.trim().isEmpty() && !"NEW".equalsIgnoreCase(sapCode.trim());
                if (isSapValid) {
                    String normalizedSapCode = sapCode.trim().toUpperCase();
                    if (existingSapCodes.contains(normalizedSapCode)) {
                        return badRequest("SAP Code '" + sapCode + "' already exists in the system for this group.");
                    }
                }

                // === XỬ LÝ HÌNH ẢNH (CỘT N - index 13) - SỬA CHÍNH XÁC VỚI MERGED CELL ===
                // === XỬ LÝ HÌNH ẢNH (CỘT O - index 14) - ĐÃ ĐỒNG NHẤT VỚI MONTHLY ===
                List<String> imageUrls = new ArrayList<>();
                for (XSSFShape shape : drawing.getShapes()) {
                    if (shape instanceof XSSFPicture pic) {
                        XSSFClientAnchor anchor = pic.getClientAnchor();

                        boolean inRow = anchor.getRow1() <= i && anchor.getRow2() >= i;
                        boolean inCol = anchor.getCol1() <= 13 && anchor.getCol2() >= 13;

                        if (inRow && inCol) {
                            try {
                                byte[] imgBytes = pic.getPictureData().getData();
                                String mimeType = pic.getPictureData().getMimeType();
                                String ext = getImageExtension(mimeType);
                                String fileName = "req_" + groupId + "_" + (i + 1) + "_" + System.currentTimeMillis() + ext;

                                String imgPath = saveImage(imgBytes, fileName, mimeType);
                                if (imgPath != null) {
                                    imageUrls.add(imgPath);
                                }
                            } catch (Exception e) {
                                System.err.println("Image save failed at row " + (i + 1) + ": " + e.getMessage());
                            }
                        }
                    }
                }

                // === XỬ LÝ DEPARTMENT ===
                String deptId = null;
                if (deptName != null && !deptName.trim().isEmpty()) {
                    Department dept = departmentRepository.findByDepartmentName(deptName.trim());
                    if (dept == null) {
                        dept = new Department();
                        dept.setDepartmentName(deptName.trim());
                        dept = departmentRepository.save(dept);
                    }
                    deptId = dept.getId();
                }

                // === XÁC ĐỊNH KEY ĐỂ GỘP ===
                boolean isSapNewOrEmpty = !isSapValid;
                String groupKey = isSapNewOrEmpty
                        ? itemVN.toLowerCase().trim()
                        : sapCode.trim().toLowerCase();

                Map<String, Integer> currentMap = isSapNewOrEmpty ? materialMap : sapCodeMap;

                // === GỘP DỮ LIỆU TRONG FILE ===
                if (currentMap.containsKey(groupKey)) {
                    int idx = currentMap.get(groupKey);
                    RequisitionMonthly existing = requisitions.get(idx);

                    // Gộp theo phòng ban
                    boolean deptExists = false;
                    for (DepartmentRequisitionMonthly dr : existing.getDepartmentRequisitions()) {
                        if (dr.getId() != null && dr.getId().equals(deptId)) {
                            dr.setQty(dr.getQty().add(requestQty));
                            dr.setBuy(dr.getBuy().add(buy));
                            deptExists = true;
                            break;
                        }
                    }
                    if (!deptExists && deptId != null) {
                        DepartmentRequisitionMonthly ndr = new DepartmentRequisitionMonthly();
                        ndr.setId(deptId); ndr.setName(deptName);
                        ndr.setQty(requestQty); ndr.setBuy(buy);
                        existing.getDepartmentRequisitions().add(ndr);
                    }

                    existing.setTotalRequestQty(existing.getTotalRequestQty().add(requestQty));
                    existing.setOrderQty(existing.getOrderQty().add(buy));

                    if (existing.getUnit() == null && unit != null) existing.setUnit(unit);
                    if (existing.getReason() == null && reason != null) existing.setReason(reason);

                    if (!imageUrls.isEmpty()) {
                        if (existing.getImageUrls() == null) {
                            existing.setImageUrls(new ArrayList<>());
                        }
                        existing.getImageUrls().addAll(imageUrls);
                    }

                    continue;
                }

                // === TẠO MỚI ===
                RequisitionMonthly req = new RequisitionMonthly();
                req.setGroupId(groupId);
                req.setCreatedDate(LocalDateTime.now());
                req.setUpdatedDate(LocalDateTime.now());

                if (isSapValid) req.setOldSAPCode(sapCode);
                req.setItemDescriptionVN(itemVN);
                req.setItemDescriptionEN(itemEN);
                req.setTotalRequestQty(requestQty);
                req.setOrderQty(buy);
                req.setDailyMedInventory(inhand);
                req.setUnit(unit);
                req.setReason(reason);
                req.setImageUrls(imageUrls.isEmpty() ? null : imageUrls);
                req.setHanaSAPCode(hanaCode);

                List<DepartmentRequisitionMonthly> deptList = new ArrayList<>();
                if (deptId != null) {
                    DepartmentRequisitionMonthly dr = new DepartmentRequisitionMonthly();
                    dr.setId(deptId); dr.setName(deptName);
                    dr.setQty(requestQty); dr.setBuy(buy);
                    deptList.add(dr);
                }
                req.setDepartmentRequisitions(deptList);

                requisitions.add(req);
                currentMap.put(groupKey, requisitions.size() - 1);
            }

            if (requisitions.isEmpty()) {
                return badRequest("No valid data found starting from row 6 (column B).");
            }

            List<RequisitionMonthly> saved = requisitionMonthlyRepository.saveAll(requisitions);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            return badRequest("Error processing file: " + e.getMessage());
        }
    }

    private MonthlyComparisonRequisitionDTO convertToComparisonDTO(RequisitionMonthly req, String currency, Boolean removeDuplicateSuppliers) {
        List<MonthlyComparisonRequisitionDTO.SupplierDTO> supplierDTOs = new ArrayList<>();

        String sapCode = req.getOldSAPCode() != null && !req.getOldSAPCode().isEmpty() ? req.getOldSAPCode() : null;
        String selectedSupplierId = req.getSupplierId();

        String unit = req.getUnit() != null ? req.getUnit() : "";
        String goodtype = "";
        BigDecimal price = null; // sẽ gán sau nếu có selected supplier

        // === CHỈ KHI CÓ supplierId MỚI ĐI TÌM SUPPLIER PRODUCT ===
        if (selectedSupplierId != null && !selectedSupplierId.isEmpty() && sapCode != null && !sapCode.isEmpty()) {
            List<SupplierProduct> suppliers = supplierProductRepository.findBySapCodeAndCurrencyIgnoreCase(sapCode, currency);

            // Xử lý loại bỏ duplicate nếu cần
            Map<String, List<SupplierProduct>> supplierGroups = suppliers.stream()
                    .collect(Collectors.groupingBy(
                            sp -> sp.getSupplierName() + "|" + sp.getSapCode() + "|" + sp.getCurrency(),
                            Collectors.toList()
                    ));

            List<SupplierProduct> filteredSuppliers = new ArrayList<>();
            if (Boolean.TRUE.equals(removeDuplicateSuppliers)) {
                for (List<SupplierProduct> group : supplierGroups.values()) {
                    if (group.size() > 1) {
                        Optional<SupplierProduct> selected = group.stream()
                                .filter(sp -> selectedSupplierId.equals(sp.getId()))
                                .findFirst();
                        if (selected.isPresent()) {
                            filteredSuppliers.add(selected.get());
                        } else {
                            group.stream()
                                    .filter(sp -> sp.getPrice() != null)
                                    .min(Comparator.comparing(SupplierProduct::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                                    .ifPresent(filteredSuppliers::add);
                        }
                    } else {
                        filteredSuppliers.addAll(group);
                    }
                }
            } else {
                filteredSuppliers.addAll(suppliers);
            }

            // Tính giá thấp nhất toàn cục (dùng để đánh dấu best price)
            BigDecimal globalMinPrice = filteredSuppliers.stream()
                    .map(SupplierProduct::getPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            supplierDTOs = filteredSuppliers.stream()
                    .map(sp -> {
                        boolean isBestPrice = !Boolean.TRUE.equals(removeDuplicateSuppliers)
                                && globalMinPrice != null
                                && sp.getPrice() != null
                                && sp.getPrice().compareTo(globalMinPrice) == 0;
                        return new MonthlyComparisonRequisitionDTO.SupplierDTO(
                                sp.getPrice(),
                                sp.getSupplierName(),
                                selectedSupplierId.equals(sp.getId()) ? 1 : 0,
                                sp.getUnit(),
                                isBestPrice
                        );
                    })
                    .sorted(Comparator.comparing(
                            MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice,
                            Comparator.nullsLast(BigDecimal::compareTo)))
                    .collect(Collectors.toList());

            // Lấy thông tin của selected supplier để cập nhật unit, currency, goodtype
            Optional<SupplierProduct> selectedSupplier = suppliers.stream()
                    .filter(sp -> selectedSupplierId.equals(sp.getId()))
                    .findFirst();

            if (selectedSupplier.isPresent()) {
                SupplierProduct sp = selectedSupplier.get();
                unit = sp.getUnit() != null ? sp.getUnit() : unit;
                currency = sp.getCurrency() != null ? sp.getCurrency() : currency;
                goodtype = sp.getGoodType() != null ? sp.getGoodType() : goodtype;
                price = sp.getPrice(); // giá của nhà cung cấp được chọn
            }
        }

        // === Nếu KHÔNG có supplierId → vẫn có thể lấy giá từ req (nếu có lưu trước đó) ===
        if (price == null && req.getPrice() != null) {
            price = req.getPrice(); // fallback nếu đã lưu giá trước đó
        }

        // === Các tính toán còn lại giữ nguyên ===
        BigDecimal highestPrice = supplierDTOs.stream()
                .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal orderQty = req.getOrderQty() != null ? req.getOrderQty() : BigDecimal.ZERO;
        BigDecimal amount = price != null ? price.multiply(orderQty) : BigDecimal.ZERO;
        BigDecimal highestAmount = highestPrice != null ? highestPrice.multiply(orderQty) : BigDecimal.ZERO;
        BigDecimal amtDifference = highestAmount != null ? amount.subtract(highestAmount) : BigDecimal.ZERO;

        BigDecimal percentage = BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) != 0 && highestAmount.compareTo(BigDecimal.ZERO) != 0) {
            percentage = amtDifference.divide(amount, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        BigDecimal minPrice = supplierDTOs.stream()
                .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        Boolean isBestPrice = false;
        String statusBestPrice = req.getStatusBestPrice();
        if (statusBestPrice == null || statusBestPrice.trim().isEmpty() || "EMPTY".equalsIgnoreCase(statusBestPrice.trim())) {
            isBestPrice = price != null && minPrice != null && price.compareTo(minPrice) == 0;
        } else {
            isBestPrice = "Yes".equalsIgnoreCase(statusBestPrice.trim());
        }

        // Department requests
        List<MonthlyComparisonRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequisitions() != null ?
                req.getDepartmentRequisitions().stream()
                        .filter(Objects::nonNull)
                        .map(dept -> new MonthlyComparisonRequisitionDTO.DepartmentRequestDTO(
                                dept.getId(),
                                dept.getName(),
                                dept.getQty() != null ? dept.getQty() : BigDecimal.ZERO,
                                dept.getBuy() != null ? dept.getBuy() : BigDecimal.ZERO
                        ))
                        .collect(Collectors.toList()) : Collections.emptyList();

        // Product type names (giữ nguyên)
        String type1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty() ?
                productType1Service.getById(req.getProductType1Id()).getName() : "";
        String type2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty() ?
                productType2Service.getById(req.getProductType2Id()).getName() : "";

        // Các field khác
        BigDecimal dailyMedInventory = req.getDailyMedInventory();
        BigDecimal totalRequestQty = req.getTotalRequestQty();
        BigDecimal safeStock = req.getSafeStock();
        BigDecimal useStockQty = req.getUseStockQty();

        return new MonthlyComparisonRequisitionDTO(
                req.getId(),
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
                isBestPrice,
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

    // === HELPER: Trả về lỗi nhanh ===
    private ResponseEntity<List<RequisitionMonthly>> badRequest(String message) {
        return ResponseEntity.badRequest().body(
                Collections.singletonList(new RequisitionMonthly() {{ setRemark(message); }})
        );
    }

    // Helper method to get cell value
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // SAP Code có thể là số → chuyển thành String
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue()); // Ép về long để tránh .0
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf((long) cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }

    // Helper method to parse Double value
    private Double parseDouble(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } else if (cell.getCellType() == CellType.FORMULA) {
                return cell.getNumericCellValue();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // Helper method to parse BigDecimal value
    private BigDecimal parseBigDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                return new BigDecimal(cell.getStringCellValue().trim());
            } else if (cell.getCellType() == CellType.FORMULA) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    // Helper method to get value from cell, handling merged cells
    private Cell getMergedCellValue(Sheet sheet, int rowIndex, int colIndex, List<CellRangeAddress> mergedRegions) {
        for (CellRangeAddress range : mergedRegions) {
            if (range.isInRange(rowIndex, colIndex)) {
                Row firstRow = sheet.getRow(range.getFirstRow());
                return firstRow.getCell(range.getFirstColumn(), Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
            }
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) return null;
        return row.getCell(colIndex, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
    }

    private String saveImage(byte[] imageBytes, String fileName, String mimeType) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) return null;

        Set<String> allowed = Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp");
        if (!allowed.contains(mimeType)) {
            System.err.println("Skipped unsupported format: " + mimeType);
            return null;
        }

        Path path = Paths.get(UPLOAD_DIR).resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, imageBytes);

        return "/uploads/" + fileName;
    }

    private String getImageExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    @Transactional
    public void autoSelectBestSupplierAndSave(String groupId, String currency) {

        List<UpdateRequisitionMonthlyDTO> dtoList = requisitionMonthlyRepository.findRequestByGroupId(groupId);
        if (dtoList == null || dtoList.isEmpty()) return;

        if (currency == null || currency.isBlank()) currency = "VND";

        Map<String, RequisitionMonthly> entityMap = dtoList.stream()
                .collect(Collectors.toMap(
                        UpdateRequisitionMonthlyDTO::getId,
                        dto -> requisitionMonthlyRepository.findById(dto.getId())
                                .orElseThrow(() -> new RuntimeException("Item not found: " + dto.getId()))
                ));

        for (UpdateRequisitionMonthlyDTO dto : dtoList) {
            String searchKey = null;
            boolean useSapCode = dto.getOldSAPCode() != null
                    && !dto.getOldSAPCode().trim().isEmpty()
                    && !"NEW".equalsIgnoreCase(dto.getOldSAPCode().trim());

            if (useSapCode) {
                searchKey = dto.getOldSAPCode().trim();
            } else if (dto.getItemDescriptionVN() != null && !dto.getItemDescriptionVN().trim().isEmpty()) {
                searchKey = dto.getItemDescriptionVN().trim();
            } else {
                dto.setSupplierComparisonList(Collections.emptyList());
                continue;
            }

            List<SupplierProduct> suppliers = new ArrayList<>();
            if (useSapCode) {
                suppliers = supplierProductRepository.findBySapCodeAndCurrencyIgnoreCase(searchKey, currency);
            }
            if (suppliers.isEmpty()) {
                suppliers = supplierProductRepository.findByItemNoContainingIgnoreCaseAndCurrency(searchKey, currency);
            }
            if (suppliers.isEmpty()) {
                dto.setSupplierComparisonList(Collections.emptyList());
                continue;
            }

            List<CompletedSupplierDTO> comparisonList = suppliers.stream()
                    .filter(sp -> sp.getPrice() != null)
                    .map(sp -> new CompletedSupplierDTO(
                            sp.getId(),
                            sp.getSupplierName(),
                            sp.getPrice(),
                            sp.getUnit(),
                            0,
                            false
                    ))
                    .sorted(Comparator.comparing(CompletedSupplierDTO::getPrice))
                    .collect(Collectors.toList());

            if (comparisonList.isEmpty()) {
                dto.setSupplierComparisonList(Collections.emptyList());
                continue;
            }

            String currentStatus = dto.getStatusBestPrice();
            if (currentStatus == null || currentStatus.trim().isEmpty()) {
                BigDecimal bestPrice = comparisonList.get(0).getPrice();
                boolean isCurrentlyBest = dto.getPrice() != null
                        && dto.getPrice().compareTo(bestPrice) == 0;
                entityMap.get(dto.getId()).setStatusBestPrice(isCurrentlyBest ? "Yes" : "No");
            }

            comparisonList.get(0).setIsSelected(1);
            comparisonList.get(0).setIsBestPrice(true);
            dto.setSupplierComparisonList(comparisonList);
            entityMap.get(dto.getId()).setSupplierComparisonList(comparisonList);
        }

        requisitionMonthlyRepository.saveAll(entityMap.values());
    }

    @GetMapping("/requisition-monthly/{id}/status")
    public ResponseEntity<?> getStatusBestPrice(@PathVariable String id) {
        try {
            Optional<RequisitionMonthly> optional = requisitionMonthlyRepository.findById(id);
            if (optional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Requisition not found for ID: " + id));
            }

            RequisitionMonthly req = optional.get();
            String currentStatus = req.getStatusBestPrice();

            if (currentStatus == null || currentStatus.trim().isEmpty() || "EMPTY".equalsIgnoreCase(currentStatus.trim())) {

                String sapCode = req.getOldSAPCode();
                String currency = req.getCurrency();
                String selectedSupplierId = req.getSupplierId();

                BigDecimal minPrice = null;
                BigDecimal currentPrice = req.getPrice();

                if (sapCode != null && !sapCode.isEmpty() && selectedSupplierId != null && !selectedSupplierId.isEmpty()) {
                    List<SupplierProduct> suppliers = supplierProductRepository
                            .findBySapCodeAndCurrencyIgnoreCase(sapCode, currency);

                    if (!suppliers.isEmpty()) {
                        minPrice = suppliers.stream()
                                .map(SupplierProduct::getPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(null);
                    }
                }

                boolean isBestPrice = currentPrice != null && minPrice != null && currentPrice.compareTo(minPrice) == 0;
                String newStatus = isBestPrice ? "Yes" : "No";

                Map<String, Object> response = new HashMap<>();
                response.put("statusBestPrice", newStatus);
                response.put("remarkComparison", req.getRemarkComparison() != null ? req.getRemarkComparison() : "");
                return ResponseEntity.ok(response);

            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("statusBestPrice", currentStatus);
                response.put("remarkComparison", req.getRemarkComparison() != null ? req.getRemarkComparison() : "");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch status: " + e.getMessage()));
        }
    }

    @GetMapping("/comparison-monthly-grouped")
    public ResponseEntity<GroupedByTypeComparisonResponseDTO> getComparisonMonthlyGrouped(
            @RequestParam String groupId,
            @RequestParam(defaultValue = "false") boolean removeDuplicateSuppliers) {

        // Lấy currency
        String currency = groupSummaryRequisitionService.getGroupSummaryRequisitionById(groupId)
                .map(GroupSummaryRequisition::getCurrency)
                .orElse("VND");

        // Lấy tất cả requisition
        List<RequisitionMonthly> requisitions = requisitionMonthlyRepository.findByGroupId(groupId);

        // Convert sang DTO cũ (tái sử dụng 100%)
        List<MonthlyComparisonRequisitionDTO> allDtos = requisitions.stream()
                .map(req -> convertToComparisonDTO(req, currency, removeDuplicateSuppliers))
                .toList();

        // === GROUP THEO TYPE1 TRƯỚC, SAU ĐÓ THEO TYPE2 TRONG TYPE1 ===
        Map<String, Map<String, List<MonthlyComparisonRequisitionDTO>>> groupedByType1ThenType2 = allDtos.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getType1() != null ? dto.getType1() : "___NULL_TYPE1___",
                        Collectors.groupingBy(
                                dto -> dto.getType2() != null ? dto.getType2() : "___NULL_TYPE2___"
                        )
                ));

        List<GroupedByTypeComparisonResponseDTO.Type1Group> type1Groups = new ArrayList<>();
        BigDecimal grandAmount = BigDecimal.ZERO;
        BigDecimal grandDiff = BigDecimal.ZERO;

        for (var type1Entry : groupedByType1ThenType2.entrySet()) {
            String type1Key = type1Entry.getKey();
            String type1 = "___NULL_TYPE1___".equals(type1Key) ? null : type1Key;
            String type1Name = type1 != null ? getType1Name(type1) : "";

            List<GroupedByTypeComparisonResponseDTO.Type2Subgroup> subgroups = new ArrayList<>();

            BigDecimal type1BuyQty = BigDecimal.ZERO;
            BigDecimal type1Amount = BigDecimal.ZERO;
            BigDecimal type1Diff = BigDecimal.ZERO;

            for (var type2Entry : type1Entry.getValue().entrySet()) {
                String type2Key = type2Entry.getKey();
                String type2 = "___NULL_TYPE2___".equals(type2Key) ? null : type2Key;
                String type2Name = type2 != null ? getType2Name(type2) : "";

                List<MonthlyComparisonRequisitionDTO> items = type2Entry.getValue();

                BigDecimal type2BuyQty = BigDecimal.ZERO;
                BigDecimal type2Amount = BigDecimal.ZERO;
                BigDecimal type2Diff = BigDecimal.ZERO;

                for (var dto : items) {
                    // Tính buy qty từ tất cả department
                    BigDecimal buyQty = dto.getDepartmentRequests().stream()
                            .map(d -> d.getBuy() != null ? d.getBuy() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    type2BuyQty = type2BuyQty.add(buyQty);
                    type2Amount = type2Amount.add(dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO);
                    type2Diff = type2Diff.add(dto.getAmtDifference() != null ? dto.getAmtDifference() : BigDecimal.ZERO);
                }

                BigDecimal type2Percent = type2Amount.compareTo(BigDecimal.ZERO) != 0
                        ? type2Diff.divide(type2Amount, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                var type2Total = new GroupedByTypeComparisonResponseDTO.GroupTotal(
                        type2BuyQty, type2Amount, type2Diff, type2Percent);

                subgroups.add(new GroupedByTypeComparisonResponseDTO.Type2Subgroup(
                        type2, type2Name, type2Total, items));

                // Cộng dồn vào Type1
                type1BuyQty = type1BuyQty.add(type2BuyQty);
                type1Amount = type1Amount.add(type2Amount);
                type1Diff = type1Diff.add(type2Diff);
            }

            BigDecimal type1Percent = type1Amount.compareTo(BigDecimal.ZERO) != 0
                    ? type1Diff.divide(type1Amount, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            var type1Total = new GroupedByTypeComparisonResponseDTO.GroupTotal(
                    type1BuyQty, type1Amount, type1Diff, type1Percent);

            type1Groups.add(new GroupedByTypeComparisonResponseDTO.Type1Group(
                    type1, type1Name, type1Total, subgroups));

            // Cộng vào Grand Total
            grandAmount = grandAmount.add(type1Amount);
            grandDiff = grandDiff.add(type1Diff);
        }

        BigDecimal grandPercent = grandAmount.compareTo(BigDecimal.ZERO) != 0
                ? grandDiff.divide(grandAmount, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        var grandTotal = new GroupedByTypeComparisonResponseDTO.GrandTotal(
                grandAmount, grandDiff, grandPercent);

        return ResponseEntity.ok(new GroupedByTypeComparisonResponseDTO(type1Groups, grandTotal));
    }

    // Helper lấy tên type (an toàn)
    private String getType1Name(String id) {
        try {
            return productType1Service.getById(id).getName();
        } catch (Exception e) {
            return "";
        }
    }

    private String getType2Name(String id) {
        try {
            return productType2Service.getById(id).getName();
        } catch (Exception e) {
            return "";
        }
    }

}