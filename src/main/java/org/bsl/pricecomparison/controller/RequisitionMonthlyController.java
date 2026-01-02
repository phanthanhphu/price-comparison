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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;
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

    // ✅ FULL API: filterRequisitions (keep old logic) + UPDATED lastPurchase:
// - lastPurchaseOrderQty: sum in previous-month window (same as old)
// - lastPurchasePrice/Date/SupplierName: latest completed record ALL TIME (not previous month)

    // ✅ UPDATED: filterRequisitions - chỉ đổi đoạn last purchase: gọi helper -> set 4 field
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
            @RequestParam(defaultValue = "false") boolean includeMonthlyLastPurchase,
            Pageable pageable) {

        GroupSummaryRequisition group = groupSummaryRequisitionService.getGroupSummaryRequisitionById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        LocalDateTime monthStart = null;
        LocalDateTime monthEndExclusive = null;

        if (includeMonthlyLastPurchase) {
            LocalDate createdDate = group.getCreatedDate().toLocalDate();
            LocalDate currentMonthStart = createdDate.withDayOfMonth(1);
            LocalDate previousMonthStart = currentMonthStart.minusMonths(1);

            monthStart = previousMonthStart.atStartOfDay();
            monthEndExclusive = currentMonthStart.atStartOfDay();
        }

        List<RequisitionMonthly> requisitions;

        if (hasFilter) {
            requisitions = requisitionMonthlyRepository.findByGroupId(groupId).stream()
                    .filter(req -> {
                        boolean matches = true;

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
                                            .filter(Objects::nonNull)
                                            .map(DepartmentRequisitionMonthly::getName)
                                            .filter(Objects::nonNull)
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
                        return date2.compareTo(date1);
                    })
                    .collect(Collectors.toList());
        } else {
            requisitions = requisitionMonthlyRepository.findByGroupId(groupId).stream()
                    .sorted((req1, req2) -> {
                        LocalDateTime date1 = req1.getUpdatedDate() != null ? req1.getUpdatedDate() :
                                req1.getCreatedDate() != null ? req1.getCreatedDate() : LocalDateTime.MIN;
                        LocalDateTime date2 = req2.getUpdatedDate() != null ? req2.getUpdatedDate() :
                                req2.getCreatedDate() != null ? req2.getCreatedDate() : LocalDateTime.MIN;
                        return date2.compareTo(date1);
                    })
                    .collect(Collectors.toList());
        }

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

        final LocalDateTime finalMonthStart = monthStart;
        final LocalDateTime finalMonthEndExclusive = monthEndExclusive;

        List<RequisitionMonthlyDTO> requisitionDTOs = requisitions.stream()
                .map(req -> {
                    RequisitionMonthlyDTO dto = new RequisitionMonthlyDTO();

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
                    dto.setCreatedByEmail(req.getCreatedByEmail());
                    dto.setUpdatedByEmail(req.getUpdatedByEmail());
                    dto.setCompletedByEmail(req.getCompletedByEmail());
                    dto.setUncompletedByEmail(req.getUncompletedByEmail());
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

                    // ✅ ONLY CHANGE: lấy info rồi set 4 field
                    if (includeMonthlyLastPurchase && finalMonthStart != null && finalMonthEndExclusive != null) {
                        LastPurchaseInfo info = getMonthlyLastPurchaseInfo(req, finalMonthStart, finalMonthEndExclusive);
                        if (info != null) {
                            dto.setLastPurchaseOrderQty(info.getOrderQty());
                            dto.setLastPurchasePrice(info.getPrice());
                            dto.setLastPurchaseDate(info.getDate());
                            dto.setLastPurchaseSupplierName(info.getSupplierName());
                        }
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        List<RequisitionMonthlyDTO> resultRequisitionDTOs;
        Page<RequisitionMonthlyDTO> pagedResult;

        if (disablePagination) {
            resultRequisitionDTOs = requisitionDTOs;
            pagedResult = new PageImpl<>(resultRequisitionDTOs,
                    PageRequest.of(0, Integer.MAX_VALUE),
                    requisitionDTOs.size());
        } else {
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), requisitionDTOs.size());
            resultRequisitionDTOs = requisitionDTOs.subList(start, end);
            pagedResult = new PageImpl<>(resultRequisitionDTOs, pageable, requisitionDTOs.size());
        }

        RequisitionMonthlyPagedResponse response = new RequisitionMonthlyPagedResponse(
                pagedResult,
                totalSumDailyMedInventory,
                totalSumSafeStock,
                totalSumRequestQty,
                totalSumUseStockQty,
                totalSumOrderQty,
                totalSumAmount,
                totalSumPrice
        );

        return ResponseEntity.ok(response);
    }

    // ✅ UPDATED: return về 4 giá trị thay vì set vào DTO
    private LastPurchaseInfo getMonthlyLastPurchaseInfo(
            RequisitionMonthly req,
            LocalDateTime monthStart,
            LocalDateTime monthEndExclusive
    ) {
        String currency = req.getCurrency();
        if (currency == null || currency.trim().isEmpty()) return null;

        BigDecimal lastMonthTotalQty = null;

        // ===== (A) SUM QTY theo tháng trước =====
        List<RequisitionMonthly> matchesMonth = null;

        if (isValidKey(req.getOldSAPCode())) {
            matchesMonth = requisitionMonthlyRepository.findLastPurchaseByOldSapCodeAndCurrency(
                    req.getOldSAPCode().trim(), currency, monthStart, monthEndExclusive);
        } else if (isValidKey(req.getHanaSAPCode())) {
            matchesMonth = requisitionMonthlyRepository.findLastPurchaseByHanaSapCodeAndCurrency(
                    req.getHanaSAPCode().trim(), currency, monthStart, monthEndExclusive);
        } else if (isValidKey(req.getItemDescriptionVN())) {
            matchesMonth = requisitionMonthlyRepository.findLastPurchaseByItemDescriptionVNAndCurrency(
                    req.getItemDescriptionVN().trim(), currency, monthStart, monthEndExclusive);
        } else if (isValidKey(req.getItemDescriptionEN())) {
            matchesMonth = requisitionMonthlyRepository.findLastPurchaseByItemDescriptionENAndCurrency(
                    req.getItemDescriptionEN().trim(), currency, monthStart, monthEndExclusive);
        }

        if (matchesMonth != null && !matchesMonth.isEmpty()) {
            lastMonthTotalQty = matchesMonth.stream()
                    .map(RequisitionMonthly::getOrderQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // ===== (B) latest completed ALL TIME =====
        Pageable top1 = PageRequest.of(0, 1);
        RequisitionMonthly latestAllTime = null;

        if (isValidKey(req.getOldSAPCode())) {
            List<RequisitionMonthly> list =
                    requisitionMonthlyRepository.findLatestPurchaseAllTimeByOldSapCodeAndCurrency(
                            req.getOldSAPCode().trim(), currency, top1);
            latestAllTime = (list != null && !list.isEmpty()) ? list.get(0) : null;

        } else if (isValidKey(req.getHanaSAPCode())) {
            List<RequisitionMonthly> list =
                    requisitionMonthlyRepository.findLatestPurchaseAllTimeByHanaSapCodeAndCurrency(
                            req.getHanaSAPCode().trim(), currency, top1);
            latestAllTime = (list != null && !list.isEmpty()) ? list.get(0) : null;

        } else if (isValidKey(req.getItemDescriptionVN())) {
            List<RequisitionMonthly> list =
                    requisitionMonthlyRepository.findLatestPurchaseAllTimeByItemDescriptionVNAndCurrency(
                            req.getItemDescriptionVN().trim(), currency, top1);
            latestAllTime = (list != null && !list.isEmpty()) ? list.get(0) : null;

        } else if (isValidKey(req.getItemDescriptionEN())) {
            List<RequisitionMonthly> list =
                    requisitionMonthlyRepository.findLatestPurchaseAllTimeByItemDescriptionENAndCurrency(
                            req.getItemDescriptionEN().trim(), currency, top1);
            latestAllTime = (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

        BigDecimal price = null;
        LocalDateTime date = null;
        String supplierName = null;

        if (latestAllTime != null) {
            price = latestAllTime.getPrice();
            date = latestAllTime.getCompletedDate() != null ? latestAllTime.getCompletedDate()
                    : (latestAllTime.getUpdatedDate() != null ? latestAllTime.getUpdatedDate()
                    : latestAllTime.getCreatedDate());
            supplierName = latestAllTime.getSupplierName();
        }

        // nếu cả 2 phần đều null thì return null cho gọn
        if (lastMonthTotalQty == null && price == null && date == null && supplierName == null) return null;

        return new LastPurchaseInfo(lastMonthTotalQty, price, date, supplierName);
    }

    private boolean isValidKey(String key) {
        return key != null && !key.trim().isEmpty() && !"NEW".equalsIgnoreCase(key.trim());
    }


    @PutMapping(value = "/requisition-monthly/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateRequisitionMonthly(
            @PathVariable String id,
            @RequestParam("email") String email,
            @ModelAttribute UpdateRequisitionMonthlyRequest request
    ) {
        try {
            // ✅ Validate email
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

            // =========================================================
            // ✅ NEW UNIQUE RULE (only if supplierId exists):
            // groupId + (oldSap|hana|vn|en by priority) + supplierId
            // ignore key when NEW/empty (case-insensitive)
            // =========================================================

            String nextGroupId = (request.getGroupId() != null && !request.getGroupId().isBlank())
                    ? request.getGroupId().trim()
                    : (requisition.getGroupId() != null ? requisition.getGroupId().trim() : null);

            String nextOldSap = pick(request.getOldSAPCode(), requisition.getOldSAPCode());
            String nextHana   = pick(request.getHanaSAPCode(), requisition.getHanaSAPCode());
            String nextVN     = pick(request.getItemDescriptionVN(), requisition.getItemDescriptionVN());
            String nextEN     = pick(request.getItemDescriptionEN(), requisition.getItemDescriptionEN());

            // supplierId logic:
            // - if client sends supplierId => use it (even blank means clearing)
            // - else keep existing supplierId
            String nextSupplierId = (request.getSupplierId() != null)
                    ? request.getSupplierId()
                    : requisition.getSupplierId();

            Optional<RequisitionMonthly> dup = findDuplicateByPriority(
                    nextGroupId,
                    nextOldSap,
                    nextHana,
                    nextVN,
                    nextEN,
                    nextSupplierId,
                    id
            );

            if (dup.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "Duplicate in same group with same supplier (priority: oldSap -> hana -> VN -> EN).",
                                "duplicateId", dup.get().getId()
                        ));
            }

            // 3) Update oldSAPCode
            if (request.getOldSAPCode() != null && !request.getOldSAPCode().isEmpty()) {
                requisition.setOldSAPCode(request.getOldSAPCode());
            }

            // 4) Supplier handling (unchanged from yours)
            if (request.getSupplierId() != null) {

                if (!request.getSupplierId().isBlank()) {
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

                    requisition.setProductType1Id(supplier.getProductType1Id());
                    requisition.setProductType2Id(supplier.getProductType2Id());

                } else {
                    // cleared supplier
                    requisition.setSupplierId(null);
                    requisition.setSupplierName(null);

                    requisition.setPrice(BigDecimal.ZERO);
                    requisition.setAmount(BigDecimal.ZERO);
                }
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

            // 6) Update department requisitions (null-safe)
            List<DepartmentRequisitionMonthly> deptRequisitions =
                    requisition.getDepartmentRequisitions() != null
                            ? requisition.getDepartmentRequisitions()
                            : new ArrayList<>();

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

            BigDecimal price = requisition.getPrice() != null ? requisition.getPrice() : BigDecimal.ZERO;
            BigDecimal amount = price.multiply(medConfirmedQty);
            requisition.setAmount(amount);

            // 8) Handle images deletion
            List<String> currentImageUrls = requisition.getImageUrls() != null
                    ? requisition.getImageUrls()
                    : new ArrayList<>();

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
                for (MultipartFile f : files) {
                    if (f != null && !f.isEmpty()) {
                        String imageUrl = saveImage(f);
                        if (imageUrl != null) currentImageUrls.add(imageUrl);
                    }
                }
            }

            if (currentImageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }
            requisition.setImageUrls(currentImageUrls);

            // 10) Save updatedByEmail
            requisition.setUpdatedByEmail(email);

            // 11) Update timestamp & save
            requisition.setUpdatedDate(LocalDateTime.now());
            RequisitionMonthly updatedRequisition = requisitionMonthlyRepository.save(requisition);

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

     Optional<RequisitionMonthly> findDuplicateByPriority(
            String groupId,
            String oldSap,
            String hana,
            String descVN,
            String descEN,
            String supplierId,
            String currentId // exclude chính nó khi update
    ) {
        if (groupId == null || groupId.isBlank()) return Optional.empty();

        // ✅ ONLY enforce when supplierId is provided
        if (supplierId == null || supplierId.isBlank()) return Optional.empty();

        String g = groupId.trim();
        String sId = supplierId.trim();

        // 1) groupId + oldSAP + supplierId
        if (isUsableKey(oldSap)) {
            Optional<RequisitionMonthly> x =
                    requisitionMonthlyRepository.findFirstByGroupIdAndOldSAPCodeIgnoreCaseAndSupplierId(
                            g, oldSap.trim(), sId
                    );
            if (x.isPresent() && !x.get().getId().equals(currentId)) return x;
            return Optional.empty();
        }

        // 2) groupId + hana + supplierId
        if (isUsableKey(hana)) {
            Optional<RequisitionMonthly> x =
                    requisitionMonthlyRepository.findFirstByGroupIdAndHanaSAPCodeIgnoreCaseAndSupplierId(
                            g, hana.trim(), sId
                    );
            if (x.isPresent() && !x.get().getId().equals(currentId)) return x;
            return Optional.empty();
        }

        // 3) groupId + descVN + supplierId
        if (isUsableKey(descVN)) {
            Optional<RequisitionMonthly> x =
                    requisitionMonthlyRepository.findFirstByGroupIdAndItemDescriptionVNIgnoreCaseAndSupplierId(
                            g, descVN.trim(), sId
                    );
            if (x.isPresent() && !x.get().getId().equals(currentId)) return x;
            return Optional.empty();
        }

        // 4) groupId + descEN + supplierId
        if (isUsableKey(descEN)) {
            Optional<RequisitionMonthly> x =
                    requisitionMonthlyRepository.findFirstByGroupIdAndItemDescriptionENIgnoreCaseAndSupplierId(
                            g, descEN.trim(), sId
                    );
            if (x.isPresent() && !x.get().getId().equals(currentId)) return x;
        }

        return Optional.empty();
    }


    /**
     * usable = not null, not blank, not "NEW" (case-insensitive)
     */
    private boolean isUsableKey(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        return !"new".equalsIgnoreCase(t);
    }


    /**
     * pick request if user sent it (even if blank), else fallback current value.
     * Nếu bạn muốn: "request có gửi nhưng blank" => coi như blank thật thì OK,
     * vì isUsableKey sẽ tự loại blank/NEW.
     */
    private String pick(String requestValue, String currentValue) {
        return requestValue != null ? requestValue : currentValue;
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

    // ✅ UPDATED: add includeMonthlyLastPurchase param
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
            @RequestParam(defaultValue = "false") Boolean removeDuplicateSuppliers,
            @RequestParam(defaultValue = "false") boolean includeMonthlyLastPurchase
    ) {
        // ✅ get group to obtain currency + createdDate (for previous month window)
        GroupSummaryRequisition group = groupSummaryRequisitionService
                .getGroupSummaryRequisitionById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        final String groupCurrency = (group.getCurrency() != null ? group.getCurrency() : "").trim();

        // ✅ build previous month window (same logic as filterRequisitions)
        LocalDateTime monthStart = null;
        LocalDateTime monthEndExclusive = null;

        if (includeMonthlyLastPurchase) {
            LocalDate createdDate = group.getCreatedDate().toLocalDate();
            LocalDate currentMonthStart = createdDate.withDayOfMonth(1);
            LocalDate previousMonthStart = currentMonthStart.minusMonths(1);

            monthStart = previousMonthStart.atStartOfDay();
            monthEndExclusive = currentMonthStart.atStartOfDay();
        }

        // ===== cache to reduce repeated queries =====
        final Map<String, List<SupplierProduct>> supplierListCache = new HashMap<>();

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

        // pass to converter
        final LocalDateTime finalMonthStart = monthStart;
        final LocalDateTime finalMonthEndExclusive = monthEndExclusive;

        for (RequisitionMonthly req : filteredRequisitions) {
            MonthlyComparisonRequisitionDTO dto = convertToComparisonDTO(
                    req,
                    groupCurrency,
                    removeDuplicateSuppliers,
                    supplierListCache,
                    includeMonthlyLastPurchase,
                    finalMonthStart,
                    finalMonthEndExclusive
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

    // ✅ UPDATED: chỉ đổi phần "last purchase" ở cuối fun (dùng chung getMonthlyLastPurchaseInfo)
    private MonthlyComparisonRequisitionDTO convertToComparisonDTO(
            RequisitionMonthly req,
            String groupCurrency,
            Boolean removeDuplicateSuppliers,
            Map<String, List<SupplierProduct>> supplierListCache,
            boolean includeMonthlyLastPurchase,
            LocalDateTime monthStart,
            LocalDateTime monthEndExclusive
    ) {
        List<MonthlyComparisonRequisitionDTO.SupplierDTO> supplierDTOs = new ArrayList<>();

        String codeKey = null;
        if (req.getOldSAPCode() != null && !req.getOldSAPCode().isBlank()) {
            codeKey = req.getOldSAPCode().trim();
        } else if (req.getHanaSAPCode() != null && !req.getHanaSAPCode().isBlank()) {
            codeKey = req.getHanaSAPCode().trim();
        }

        String selectedSupplierId = (req.getSupplierId() != null && !req.getSupplierId().isBlank())
                ? req.getSupplierId().trim()
                : null;

        String unit = req.getUnit() != null ? req.getUnit() : "";
        String goodtype = "";
        BigDecimal price = null;

        final String finalGroupCurrency = groupCurrency != null ? groupCurrency.trim() : "";

        if (selectedSupplierId != null && codeKey != null && !codeKey.isBlank() && !finalGroupCurrency.isBlank()) {
            final String supplierListKey = codeKey + "|" + finalGroupCurrency;

            String finalCodeKey = codeKey;
            List<SupplierProduct> suppliers = supplierListCache.computeIfAbsent(supplierListKey, k ->
                    supplierProductRepository.findBySapCodeAndCurrencyIgnoreCase(finalCodeKey, finalGroupCurrency)
            );

            if (suppliers != null && !suppliers.isEmpty()) {

                List<MonthlyComparisonRequisitionDTO.SupplierDTO> allSuppliers = suppliers.stream()
                        .map(sp -> new MonthlyComparisonRequisitionDTO.SupplierDTO(
                                sp.getPrice(),
                                sp.getSupplierName(),
                                selectedSupplierId.equals(sp.getId()) ? 1 : 0,
                                sp.getUnit(),
                                false
                        ))
                        .collect(Collectors.toList());

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

        Boolean isBestPrice;
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

        // ✅ ONLY CHANGE: dùng chung helper -> set 4 field
        if (includeMonthlyLastPurchase && monthStart != null && monthEndExclusive != null) {
            LastPurchaseInfo info = getMonthlyLastPurchaseInfo(req, monthStart, monthEndExclusive);
            if (info != null) {
                dto.setLastPurchaseOrderQty(info.getOrderQty());
                dto.setLastPurchasePrice(info.getPrice());
                dto.setLastPurchaseDate(info.getDate());
                dto.setLastPurchaseSupplierName(info.getSupplierName());
            }
        }

        return dto;
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

/* =========================================================
   Helpers for strict import
   ========================================================= */

    private static class PicturePayload {
        final byte[] bytes;
        final String mimeType;
        PicturePayload(byte[] bytes, String mimeType) {
            this.bytes = bytes;
            this.mimeType = mimeType;
        }
    }

    private boolean isValidMonthlyTemplateHeader(
            Sheet sheet,
            Row headerRow,
            List<String> errors,
            int headerRowIndex,
            Map<Integer, String> expectedContains
    ) {
        if (headerRow == null) {
            errors.add("Invalid template: header row (row " + (headerRowIndex + 1) + ") not found.");
            return false;
        }

        boolean ok = true;
        for (Map.Entry<Integer, String> e : expectedContains.entrySet()) {
            int col = e.getKey();
            String expected = e.getValue();

            Cell c = headerRow.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String v = normalizeHeader(getCellValue(c));

            if (v == null || !v.contains(expected)) {
                ok = false;
                errors.add("Invalid template: header at row " + (headerRowIndex + 1) +
                        ", col " + (col + 1) + " is wrong/missing. Expected contains '" + expected + "'.");
            }
        }

        if (!ok) {
            errors.add("Please use the correct Monthly Requisition import template (latest).");
        }
        return ok;
    }

    private String normalizeHeader(String s) {
        if (s == null) return null;
        return s.toLowerCase()
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeKey(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }


    private BigDecimal parseBigDecimalStrict(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.FORMULA) {
                // try numeric result first
                try {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                } catch (Exception ignore) {}
                // fallback to string
                String s = cell.getStringCellValue();
                return parseBigDecimalFromString(s);
            }
            if (cell.getCellType() == CellType.STRING) {
                return parseBigDecimalFromString(cell.getStringCellValue());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimalFromString(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // allow "1,234" or "1 234" style
        s = s.replace(",", "").replace(" ", "");
        // if someone puts "-" or text -> fail
        return new BigDecimal(s);
    }

    private boolean isAllowedImageMime(String mimeType) {
        if (mimeType == null) return false;
        Set<String> allowed = Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp");
        return allowed.contains(mimeType);
    }

    // === HELPER: Trả về lỗi nhanh (single) ===
    private ResponseEntity<List<RequisitionMonthly>> badRequest(String message) {
        return ResponseEntity.badRequest().body(
                Collections.singletonList(new RequisitionMonthly() {{ setRemark(message); }})
        );
    }

    // === HELPER: Trả về lỗi dạng list (multi) ===
    private ResponseEntity<List<RequisitionMonthly>> badRequestErrors(List<String> messages) {
        List<RequisitionMonthly> list = new ArrayList<>();
        for (String m : messages) {
            RequisitionMonthly r = new RequisitionMonthly();
            r.setRemark(m);
            list.add(r);
        }
        return ResponseEntity.badRequest().body(list);
    }

    // Helper method to get cell value (giữ logic cũ, nhưng trim an toàn)
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : null;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    String s = cell.getStringCellValue();
                    return s != null ? s.trim() : null;
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

    // Helper: merged cell
    private Cell getMergedCellValue(Sheet sheet, int rowIndex, int colIndex, List<CellRangeAddress> mergedRegions) {
        for (CellRangeAddress range : mergedRegions) {
            if (range.isInRange(rowIndex, colIndex)) {
                Row firstRow = sheet.getRow(range.getFirstRow());
                return firstRow == null ? null : firstRow.getCell(range.getFirstColumn(), Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
            }
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) return null;
        return row.getCell(colIndex, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
    }

    private String saveImage(byte[] imageBytes, String fileName, String mimeType) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) return null;

        if (!isAllowedImageMime(mimeType)) {
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

    @PostMapping(value = "/requisition-monthly/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importMonthlyExcel(
            @RequestParam("email") String email,
            @RequestParam("groupId") String groupId,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            // ===== Validate =====
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "email is required"));
            }
            if (groupId == null || groupId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "groupId is required"));
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "file is required"));
            }

            ImportMonthlyResult result = new ImportMonthlyResult();

            // Load department master
            DepartmentMaster master = loadDepartmentMaster(result);

            // Read the Excel file
            ReadExcelResult readResult = readRowsFromExcel(file, result, master);
            List<ImportRow> rawRows = readResult.rows;
            Map<String, DeptMeta> deptMetaById = readResult.deptMetaById;

            // Merge duplicates inside the file
            Map<String, ImportRow> mergedRows = new LinkedHashMap<>();
            for (ImportRow row : rawRows) {
                String key = buildMatchKey(row);
                if (key == null) {
                    result.warnings.add("Skip row " + row.debugRowNo + " because key fields are empty (old/hana/vn/en).");
                    continue;
                }
                ImportRow existing = mergedRows.get(key);
                if (existing == null) {
                    mergedRows.put(key, row);
                } else {
                    existing.mergeFrom(row);
                    result.mergedDuplicatesInFile++;
                }

                // Calculate total request quantity from departments
                BigDecimal totalRequestQty = row.sumDeptQty();
                // Compare daily MED quantity with total request quantity
                BigDecimal dailyMedInventory = row.dailyMedInventoryQty;
                if (dailyMedInventory.compareTo(totalRequestQty) > 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "The daily MED quantity is greater than the total request quantity."));
                }
            }

            // Load DB items
            List<RequisitionMonthly> dbItems = requisitionMonthlyRepository.findAllByGroupId(groupId);
            ExistingIndex index = new ExistingIndex(dbItems);

            // Upsert logic
            Set<String> matchedDbIds = new HashSet<>();

            for (ImportRow row : mergedRows.values()) {
                RequisitionMonthly target = findExisting(index, row);

                if (target == null) {
                    // Create new record
                    RequisitionMonthly created = new RequisitionMonthly();
                    applyImportToEntity(created, row, deptMetaById, groupId, email, true);
                    requisitionMonthlyRepository.save(created);
                    result.created++;
                } else {
                    // Update existing record
                    applyImportToEntity(target, row, deptMetaById, groupId, email, false);
                    requisitionMonthlyRepository.save(target);
                    matchedDbIds.add(target.getId());
                    result.updated++;
                }
            }

            // Delete DB items not present in the file
            for (RequisitionMonthly db : dbItems) {
                if (!matchedDbIds.contains(db.getId())) {
                    requisitionMonthlyRepository.delete(db);
                    result.deleted++;
                }
            }

            result.rowsRead = rawRows.size();
            result.rowsUsedAfterMerge = mergedRows.size();

            return ResponseEntity.ok(Map.of(
                    "message", "Import monthly requisitions successfully",
                    "summary", result
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error: " + e.getMessage()));
        }
    }

    // =========================================================
// MATCH: oldSap -> hana -> vn -> en
// =========================================================
    private RequisitionMonthly findExisting(ExistingIndex index, ImportRow row) {
        String oldSap = norm(row.oldSapCode);
        if (isUsableCode(oldSap)) {
            RequisitionMonthly x = index.byOldSap.get(oldSap);
            if (x != null) return x;
        }

        String hana = norm(row.hanaSapCode);
        if (isUsableCode(hana)) {
            RequisitionMonthly x = index.byHana.get(hana);
            if (x != null) return x;
        }

        String vn = norm(row.descriptionVN);
        if (vn != null && !vn.isBlank()) {
            RequisitionMonthly x = index.byDescVN.get(vn);
            if (x != null) return x;
        }

        String en = norm(row.descriptionEN);
        if (en != null && !en.isBlank()) {
            return index.byDescEN.get(en);
        }

        return null;
    }

    private String buildMatchKey(ImportRow row) {
        String oldSap = norm(row.oldSapCode);
        if (isUsableCode(oldSap)) return "OLD_SAP::" + oldSap;

        String hana = norm(row.hanaSapCode);
        if (isUsableCode(hana)) return "HANA::" + hana;

        String vn = norm(row.descriptionVN);
        if (vn != null && !vn.isBlank()) return "VN::" + vn;

        String en = norm(row.descriptionEN);
        if (en != null && !en.isBlank()) return "EN::" + en;

        return null;
    }

    private boolean isUsableCode(String code) {
        return code != null && !code.isBlank() && !"new".equalsIgnoreCase(code.trim());
    }

    private String norm(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String normDept(String s) {
        if (s == null) return null;
        return s.replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    // ✅ remove .0, avoid scientific notation
    private BigDecimal stripTrailingZerosSafe(BigDecimal x) {
        if (x == null) return BigDecimal.ZERO;
        BigDecimal z = x.stripTrailingZeros();
        // if becomes 5E+2, convert back to plain scale
        if (z.scale() < 0) z = z.setScale(0);
        return z;
    }

    private BigDecimal nvl(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    // =========================================================
// LOAD MASTER DEPARTMENTS (id + name)
// - warn duplicates only, deterministic pick when used.
// =========================================================
    private DepartmentMaster loadDepartmentMaster(ImportMonthlyResult result) {
        List<Department> list = departmentRepository.findAllIdAndNames();

        Map<String, List<DeptMeta>> byNorm = new HashMap<>();

        for (Department d : list) {
            if (d == null) continue;
            String id = d.getId();
            String name = d.getDepartmentName();
            if (id == null || id.isBlank() || name == null || name.isBlank()) continue;

            String key = normDept(name);
            byNorm.computeIfAbsent(key, k -> new ArrayList<>()).add(new DeptMeta(id, name.trim()));
        }

        for (Map.Entry<String, List<DeptMeta>> e : byNorm.entrySet()) {
            List<DeptMeta> metas = e.getValue();
            if (metas != null && metas.size() > 1) {
                result.warnings.add("Duplicate department name in master (case/space-insensitive): "
                        + metas.get(0).name + " (count=" + metas.size() + "). Will auto-pick deterministic ID if used in Excel header.");
            }
        }

        DepartmentMaster master = new DepartmentMaster();
        master.byNormName = byNorm;
        return master;
    }

    private DeptMeta pickDeptDeterministically(List<DeptMeta> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparing(a -> a.id == null ? "" : a.id));
        return candidates.get(0);
    }

    // =========================================================
// APPLY IMPORT -> ENTITY
// IMPORTANT FIX:
// ✅ price: keep + normalize (no .0)
// ✅ supplierName: keep if Excel blank
// ✅ amount = orderQty * price (orderQty = dailyMedInventory)
// =========================================================
    private void applyImportToEntity(
            RequisitionMonthly entity,
            ImportRow row,
            Map<String, DeptMeta> deptMetaById,
            String groupId,
            String email,
            boolean isCreate
    ) {
        entity.setGroupId(groupId);

        entity.setProductType1Name(row.productType1);
        entity.setProductType2Name(row.productType2);

        entity.setItemDescriptionEN(row.descriptionEN);
        entity.setItemDescriptionVN(row.descriptionVN);

        entity.setOldSAPCode(row.oldSapCode);
        entity.setHanaSAPCode(row.hanaSapCode);

        entity.setUnit(row.unit);

        // ✅ totalRequestQty = sum dept qty
        BigDecimal totalReqFromDept = stripTrailingZerosSafe(row.sumDeptQty());
        entity.setTotalRequestQty(totalReqFromDept);

        // ✅ dailyMedInventory = confirmed MED qty (Excel Q'TY)
        BigDecimal dailyMed = stripTrailingZerosSafe(row.dailyMedInventoryQty);
        entity.setDailyMedInventory(dailyMed);

        // ✅ orderQty = dailyMedInventory
        entity.setOrderQty(dailyMed);

        // ✅ allocate buy (also strip .0)
        entity.setDepartmentRequisitions(
                row.toDepartmentEntitiesWithBuyAllocationSmallFirst(dailyMed, deptMetaById, this::stripTrailingZerosSafe)
        );

        // ✅ price: Keep the existing price from database (don't update from file)
        if (entity.getPrice() == null) {
            entity.setPrice(BigDecimal.ZERO);  // If no price exists, set it to 0
        }

        // ✅ supplierName: ONLY overwrite if Excel has value (avoid wiping to "")
        if (row.supplierName != null && !row.supplierName.isBlank()) {
            entity.setSupplierName(row.supplierName.trim());
        } // else keep old supplierName

        entity.setRemark(row.remark);

        entity.setType(RequisitionType.MONTHLY);

        // ✅ amount = orderQty * price (NOT 0.00)
        BigDecimal amount = stripTrailingZerosSafe(dailyMed.multiply(entity.getPrice())); // Use updated price if changed
        entity.setAmount(amount);

        if (isCreate) {
            entity.setCreatedByEmail(email);
            entity.setCreatedDate(LocalDateTime.now());
        }
        entity.setUpdatedByEmail(email);
        entity.setUpdatedDate(LocalDateTime.now());
    }



    // =========================================================
// READ EXCEL
// Stop markers: "TOTAL" or "Request by" (case/space-insensitive)
// Also: numbers read via DataFormatter to avoid 500.0
// =========================================================
    private ReadExcelResult readRowsFromExcel(
            MultipartFile file,
            ImportMonthlyResult result,
            DepartmentMaster master
    ) throws Exception {

        try (InputStream is = file.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheetAt(0);

            Row headerRow2 = sheet.getRow(1); // Excel row 2
            Row headerRow3 = sheet.getRow(2); // Excel row 3

            int totalReqCol = findCol(headerRow2, "Total Request");
            int confirmedMedCol = findCol(headerRow2, "Confirmed MED");
            int priceCol = findCol(headerRow2, "Price");
            int supplierCol = findCol(headerRow2, "Suppliers");
            int remarkCol = findCol(headerRow2, "Remark");

            int productType1Col = findCol(headerRow2, "Product Type 1");
            int productType2Col = findCol(headerRow2, "Product Type 2");
            int oldSapCol = findCol(headerRow2, "Old SAP Code");
            int hanaCol = findCol(headerRow2, "Hana SAP Code");
            int unitCol = findCol(headerRow2, "Unit");

            int descENCol = findCol(headerRow3, "EN");
            int descVNCol = findCol(headerRow3, "VN");

            int medQtyCol = findCol(headerRow3, "Q'TY");
            if (medQtyCol < 0) medQtyCol = confirmedMedCol;

            int deptStartCol = findCol(headerRow2, "Departments");
            if (deptStartCol < 0) deptStartCol = findMergedHeaderStartCol(sheet, "Departments");

            if (deptStartCol < 0 || totalReqCol < 0) {
                throw new IllegalArgumentException("Excel format invalid: cannot find 'Departments' or 'Total Request'.");
            }

            // =========================================================
            // ✅ Detect EN/VN content swapped (optional, bạn đang dùng)
            // =========================================================
            boolean swapDescColumns = false;
            int firstDataRowIndex = 3; // r starts at 3 (Excel row 4)

            if (descENCol >= 0 && descVNCol >= 0) {
                Row firstDataRow = sheet.getRow(firstDataRowIndex);
                if (firstDataRow != null) {
                    String enSample = getString(firstDataRow.getCell(descENCol), wb);
                    String vnSample = getString(firstDataRow.getCell(descVNCol), wb);

                    // EN column contains Vietnamese diacritics but VN column doesn't => swapped content
                    if (hasVietnameseDiacritics(enSample) && !hasVietnameseDiacritics(vnSample)) {
                        swapDescColumns = true;
                        result.warnings.add("Detected Excel template has EN/VN content swapped. Auto-corrected by swapping read columns.");
                    }
                }
            }

            // =========================================================
            // Build department mapping from headerRow3
            // =========================================================
            List<DeptCol> deptCols = new ArrayList<>();
            List<String> unknown = new ArrayList<>();
            Map<String, DeptMeta> deptMetaById = new HashMap<>();

            for (int c = deptStartCol; c < totalReqCol; c++) {
                String deptName = getString(headerRow3.getCell(c), wb);
                if (deptName == null || deptName.isBlank()) continue;

                String normalized = normDept(deptName);
                List<DeptMeta> candidates = master.byNormName.get(normalized);

                if (candidates == null || candidates.isEmpty()) {
                    unknown.add(deptName.trim());
                    continue;
                }

                DeptMeta picked = pickDeptDeterministically(new ArrayList<>(candidates));
                if (picked == null) {
                    unknown.add(deptName.trim());
                    continue;
                }

                deptCols.add(new DeptCol(c, picked.id, picked.name));
                deptMetaById.put(picked.id, picked);
            }

            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException("Invalid departments in Excel header: " + String.join(", ", unknown));
            }

            // =========================================================
            // ✅ Read data rows
            // =========================================================
            List<ImportRow> rows = new ArrayList<>();

            for (int r = 3; r <= sheet.getLastRowNum(); r++) { // data starts row 4
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // ✅ STOP condition: if this row contains "Request by" anywhere -> stop import
                if (rowContainsText(row, wb, "request by")) {
                    break;
                }

                // ✅ SKIP subtotal/total rows like: "SUB TOTAL 1.1", "TOTAL 1", "TOTAL"
                if (isSubtotalOrTotalRow(row, wb)) {
                    continue;
                }

                // --- read codes first ---
                String oldSap = oldSapCol >= 0 ? getString(row.getCell(oldSapCol), wb) : null;
                String hana = hanaCol >= 0 ? getString(row.getCell(hanaCol), wb) : null;

                // --- read EN/VN (apply swap if needed) ---
                String en;
                String vn;
                if (swapDescColumns) {
                    en = descVNCol >= 0 ? getString(row.getCell(descVNCol), wb) : null;
                    vn = descENCol >= 0 ? getString(row.getCell(descENCol), wb) : null;
                } else {
                    en = descENCol >= 0 ? getString(row.getCell(descENCol), wb) : null;
                    vn = descVNCol >= 0 ? getString(row.getCell(descVNCol), wb) : null;
                }

                // ✅ IMPORTANT FIX:
                // Không break khi EN/VN trống nữa.
                // Nếu cả EN, VN, oldSap, hana đều trống => skip (continue)
                if (allBlank(en, vn, oldSap, hana)) {
                    continue;
                }

                ImportRow ir = new ImportRow();
                ir.debugRowNo = r + 1;

                ir.productType1 = productType1Col >= 0 ? getString(row.getCell(productType1Col), wb) : null;
                ir.productType2 = productType2Col >= 0 ? getString(row.getCell(productType2Col), wb) : null;

                ir.descriptionEN = en;
                ir.descriptionVN = vn;

                ir.oldSapCode = oldSap;
                ir.hanaSapCode = hana;

                ir.unit = unitCol >= 0 ? getString(row.getCell(unitCol), wb) : null;

                ir.totalRequestQtyFromExcel = totalReqCol >= 0
                        ? getDecimal(row.getCell(totalReqCol), wb)
                        : BigDecimal.ZERO;

                ir.dailyMedInventoryQty = medQtyCol >= 0
                        ? getDecimal(row.getCell(medQtyCol), wb)
                        : BigDecimal.ZERO;

                ir.supplierName = supplierCol >= 0 ? getString(row.getCell(supplierCol), wb) : null;
                ir.remark = remarkCol >= 0 ? getString(row.getCell(remarkCol), wb) : null;

                int orderIndex = 0;
                for (DeptCol dc : deptCols) {
                    BigDecimal qty = getDecimalNullable(row.getCell(dc.colIndex), wb);
                    if (qty == null) {
                        orderIndex++;
                        continue;
                    }
                    ir.deptQtyById.put(dc.deptId, qty);
                    ir.deptOrderById.put(dc.deptId, orderIndex);
                    orderIndex++;
                }

                rows.add(ir);
            }

            ReadExcelResult out = new ReadExcelResult();
            out.rows = rows;
            out.deptMetaById = deptMetaById;
            return out;
        }
    }

    /**
     * Row contains a target text in ANY cell (first ~30 columns is enough)
     */
    private boolean rowContainsText(Row row, Workbook wb, String targetLower) {
        if (row == null || targetLower == null) return false;
        String target = targetLower.trim().toLowerCase();

        int last = Math.min(row.getLastCellNum(), 30); // limit scan
        for (int c = 0; c < last; c++) {
            String v = getString(row.getCell(c), wb);
            if (v != null && v.trim().toLowerCase().contains(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Skip rows like: "SUB TOTAL ...", "TOTAL ..."
     * (in file bạn gửi, nó nằm ở cột A)
     */
    private boolean isSubtotalOrTotalRow(Row row, Workbook wb) {
        String first = getString(row.getCell(0), wb);
        if (first == null) return false;
        String x = first.replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();

        return x.startsWith("sub total") || x.equals("total") || x.startsWith("total ");
    }

    /**
     * ✅ Dùng để detect file đang bị đảo nội dung EN/VN
     */
    private boolean hasVietnameseDiacritics(String s) {
        if (s == null) return false;
        return s.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ].*");
    }


    private boolean isEndOfDataRow(String en, String vn) {
        String a = normalizeEndText(en);
        String b = normalizeEndText(vn);

        if ("total".equalsIgnoreCase(a) || "total".equalsIgnoreCase(b)) return true;
        if ("request by".equalsIgnoreCase(a) || "request by".equalsIgnoreCase(b)) return true;

        if (a == null || b == null || a.trim().isEmpty() || b.trim().isEmpty()) return true;

        return false;
    }


    private String normalizeEndText(String s) {
        if (s == null) return null;
        return s.replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    private int findMergedHeaderStartCol(Sheet sheet, String headerText) {
        if (sheet == null || headerText == null) return -1;

        String target = headerText.trim().toLowerCase();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region == null) continue;

            if (region.getFirstRow() != 1) continue;

            Row row = sheet.getRow(region.getFirstRow());
            if (row == null) continue;

            for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                Cell cc = row.getCell(c);
                String vv = getString(cc, sheet.getWorkbook());
                if (vv != null && vv.trim().toLowerCase().equals(target)) {
                    return region.getFirstColumn();
                }
            }
        }
        return -1;
    }

    private boolean allBlank(String... xs) {
        for (String x : xs) {
            if (x != null && !x.isBlank()) return false;
        }
        return true;
    }

    private int findCol(Row row, String text) {
        if (row == null) return -1;
        for (Cell cell : row) {
            String v = getString(cell, row.getSheet().getWorkbook());
            if (v != null && v.trim().equalsIgnoreCase(text.trim())) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    // =========================================================
// ✅ IMPORTANT: DataFormatter để tránh double => 500.0
// =========================================================
    private String getString(Cell cell, Workbook wb) {
        if (cell == null) return null;
        try {
            DataFormatter formatter = new DataFormatter();
            if (cell.getCellType() == CellType.FORMULA) {
                FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                return formatter.formatCellValue(cell, evaluator).trim();
            }
            return formatter.formatCellValue(cell).trim();
        } catch (Exception e) {
            try {
                return cell.toString().trim();
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    // blank => 0 (NO .0)
    private BigDecimal getDecimal(Cell cell, Workbook wb) {
        String s = getString(cell, wb);
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try {
            s = s.replace(",", "").trim();
            BigDecimal bd = new BigDecimal(s);
            return stripTrailingZerosSafe(bd);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // blank => null (NO .0), "0" => 0
    private BigDecimal getDecimalNullable(Cell cell, Workbook wb) {
        String s = getString(cell, wb);
        if (s == null || s.isBlank()) return null;
        try {
            s = s.replace(",", "").trim();
            BigDecimal bd = new BigDecimal(s);
            return stripTrailingZerosSafe(bd);
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================
// Helper classes
// =========================================================
    private static class DeptMeta {
        String id;
        String name;
        DeptMeta(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class DepartmentMaster {
        Map<String, List<DeptMeta>> byNormName = new HashMap<>();
    }

    private static class DeptCol {
        int colIndex;
        String deptId;
        String deptName;
        DeptCol(int colIndex, String deptId, String deptName) {
            this.colIndex = colIndex;
            this.deptId = deptId;
            this.deptName = deptName;
        }
    }

    private static class ReadExcelResult {
        List<ImportRow> rows = new ArrayList<>();
        Map<String, DeptMeta> deptMetaById = new HashMap<>();
    }

    // =========================================================
// Import Row + merge + buy allocation
// =========================================================
    private static class ImportRow {
        int debugRowNo;

        String productType1;
        String productType2;

        String descriptionEN;
        String descriptionVN;

        String oldSapCode;
        String hanaSapCode;

        String unit;

        BigDecimal totalRequestQtyFromExcel = BigDecimal.ZERO;
        BigDecimal dailyMedInventoryQty = BigDecimal.ZERO;

        BigDecimal price = BigDecimal.ZERO;
        String supplierName;
        String remark;

        LinkedHashMap<String, BigDecimal> deptQtyById = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> deptOrderById = new LinkedHashMap<>();

        void mergeFrom(ImportRow other) {
            this.totalRequestQtyFromExcel = nvl(this.totalRequestQtyFromExcel).add(nvl(other.totalRequestQtyFromExcel));
            this.dailyMedInventoryQty = nvl(this.dailyMedInventoryQty).add(nvl(other.dailyMedInventoryQty));

            for (Map.Entry<String, BigDecimal> e : other.deptQtyById.entrySet()) {
                String deptId = e.getKey();
                BigDecimal qty = nvl(e.getValue());
                this.deptQtyById.merge(deptId, qty, BigDecimal::add);

                if (!this.deptOrderById.containsKey(deptId) && other.deptOrderById.containsKey(deptId)) {
                    this.deptOrderById.put(deptId, other.deptOrderById.get(deptId));
                }
            }

            // ✅ price: prefer non-zero
            if (nvl(this.price).compareTo(BigDecimal.ZERO) == 0 && nvl(other.price).compareTo(BigDecimal.ZERO) != 0) {
                this.price = other.price;
            }

            // ✅ supplierName: prefer non-blank
            if ((this.supplierName == null || this.supplierName.isBlank()) && other.supplierName != null && !other.supplierName.isBlank()) {
                this.supplierName = other.supplierName;
            }

            if ((this.remark == null || this.remark.isBlank()) && other.remark != null) {
                this.remark = other.remark;
            }
        }

        BigDecimal sumDeptQty() {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal v : deptQtyById.values()) {
                if (v != null) sum = sum.add(v);
            }
            return sum;
        }

        List<DepartmentRequisitionMonthly> toDepartmentEntitiesWithBuyAllocationSmallFirst(
                BigDecimal dailyMedInventory,
                Map<String, DeptMeta> deptMetaById,
                java.util.function.Function<BigDecimal, BigDecimal> normalizer
        ) {
            BigDecimal totalReq = sumDeptQty();
            Map<String, BigDecimal> buyMap = allocateBuySmallFirst(
                    deptQtyById,
                    deptOrderById,
                    nvl(dailyMedInventory),
                    totalReq
            );

            List<DepartmentRequisitionMonthly> list = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> e : deptQtyById.entrySet()) {
                String deptId = e.getKey();
                BigDecimal qty = normalizer.apply(nvl(e.getValue()));
                BigDecimal buy = normalizer.apply(buyMap.getOrDefault(deptId, BigDecimal.ZERO));

                DeptMeta meta = deptMetaById != null ? deptMetaById.get(deptId) : null;
                String deptName = meta != null ? meta.name : null;

                list.add(new DepartmentRequisitionMonthly(deptId, deptName, qty, buy));
            }
            return list;
        }

        private static Map<String, BigDecimal> allocateBuySmallFirst(
                LinkedHashMap<String, BigDecimal> deptQtyById,
                LinkedHashMap<String, Integer> deptOrderById,
                BigDecimal dailyMedInventory,
                BigDecimal totalDeptRequest
        ) {
            Map<String, BigDecimal> allocated = new HashMap<>();
            for (String deptId : deptQtyById.keySet()) allocated.put(deptId, BigDecimal.ZERO);

            if (dailyMedInventory == null || dailyMedInventory.compareTo(BigDecimal.ZERO) <= 0) return allocated;
            if (totalDeptRequest == null || totalDeptRequest.compareTo(BigDecimal.ZERO) <= 0) return allocated;

            BigDecimal remaining = dailyMedInventory.min(totalDeptRequest);

            List<DeptAlloc> candidates = new ArrayList<>();
            int fallback = 0;
            for (Map.Entry<String, BigDecimal> e : deptQtyById.entrySet()) {
                String deptId = e.getKey();
                BigDecimal q = nvl(e.getValue());

                if (q.compareTo(BigDecimal.ZERO) > 0) {
                    int idx = deptOrderById.getOrDefault(deptId, fallback);
                    candidates.add(new DeptAlloc(deptId, q, idx));
                }
                fallback++;
            }

            candidates.sort((a, b) -> {
                int c = a.qty.compareTo(b.qty);
                if (c != 0) return c;
                return Integer.compare(a.originalIndex, b.originalIndex);
            });

            for (DeptAlloc d : candidates) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal give = d.qty.min(remaining);
                allocated.put(d.deptId, give);
                remaining = remaining.subtract(give);
            }

            return allocated;
        }

        private static class DeptAlloc {
            String deptId;
            BigDecimal qty;
            int originalIndex;

            DeptAlloc(String deptId, BigDecimal qty, int originalIndex) {
                this.deptId = deptId;
                this.qty = qty;
                this.originalIndex = originalIndex;
            }
        }

        private static BigDecimal nvl(BigDecimal x) {
            return x == null ? BigDecimal.ZERO : x;
        }
    }

    // =========================================================
// DB Index (within groupId)
// =========================================================
    private static class ExistingIndex {
        Map<String, RequisitionMonthly> byOldSap = new HashMap<>();
        Map<String, RequisitionMonthly> byHana = new HashMap<>();
        Map<String, RequisitionMonthly> byDescVN = new HashMap<>();
        Map<String, RequisitionMonthly> byDescEN = new HashMap<>();

        ExistingIndex(List<RequisitionMonthly> items) {
            for (RequisitionMonthly x : items) {
                if (x.getOldSAPCode() != null && !x.getOldSAPCode().isBlank()) {
                    byOldSap.put(norm(x.getOldSAPCode()), x);
                }
                if (x.getHanaSAPCode() != null && !x.getHanaSAPCode().isBlank()) {
                    byHana.put(norm(x.getHanaSAPCode()), x);
                }
                if (x.getItemDescriptionVN() != null && !x.getItemDescriptionVN().isBlank()) {
                    byDescVN.put(norm(x.getItemDescriptionVN()), x);
                }
                if (x.getItemDescriptionEN() != null && !x.getItemDescriptionEN().isBlank()) {
                    byDescEN.put(norm(x.getItemDescriptionEN()), x);
                }
            }
        }

        private static String norm(String s) {
            if (s == null) return null;
            return s.trim().replaceAll("\\s+", " ").toLowerCase();
        }
    }

    // =========================================================
// Result DTO
// =========================================================
    public static class ImportMonthlyResult {
        public int rowsRead;
        public int rowsUsedAfterMerge;
        public int mergedDuplicatesInFile;

        public int created;
        public int updated;
        public int deleted;

        public List<String> warnings = new ArrayList<>();
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

        // ✅ gom lỗi validate — chỉ khi không có lỗi mới saveAll
        List<String> validationErrors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            return badRequest("No file uploaded.");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
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
                Cell itemENCell   = getMergedCellValue(sheet, i, 2, mergedRegions);   // Cột C
                Cell sapCell      = getMergedCellValue(sheet, i, 3, mergedRegions);   // Cột D
                Cell hanaCell     = getMergedCellValue(sheet, i, 4, mergedRegions);   // Cột E
                Cell requestCell  = getMergedCellValue(sheet, i, 8, mergedRegions);   // Cột I
                Cell unitCell     = getMergedCellValue(sheet, i, 9, mergedRegions);   // Cột J
                Cell deptCell     = getMergedCellValue(sheet, i, 10, mergedRegions);  // Cột K
                Cell reasonCell   = getMergedCellValue(sheet, i, 14, mergedRegions);  // Cột O (Remark)

                String itemEN   = getCellValue(itemENCell);
                String sapCode  = getCellValue(sapCell);      // sapCode là String, dù Excel lưu số
                String hanaCode = getCellValue(hanaCell);

                BigDecimal requestQty = parseBigDecimal(requestCell);
                if (requestQty == null) requestQty = BigDecimal.ZERO;

                // Inhand và Buy không còn trong file → mặc định 0
                BigDecimal inhand = BigDecimal.ZERO;
                BigDecimal buy = BigDecimal.ZERO;

                String unit = getCellValue(unitCell);
                String deptName = getCellValue(deptCell);
                String reason = getCellValue(reasonCell);

                // === KIỂM TRA TRÙNG SAP CODE TRONG DB (GOM LỖI, KHÔNG RETURN NGAY) ===
                boolean isSapValid = sapCode != null && !sapCode.trim().isEmpty() && !"NEW".equalsIgnoreCase(sapCode.trim());
                if (isSapValid) {
                    String normalizedSapCode = sapCode.trim().toUpperCase();
                    if (existingSapCodes.contains(normalizedSapCode)) {
                        validationErrors.add("Row " + (i + 1) + ": SAP Code '" + sapCode + "' already exists in the system for this group.");
                        // không continue bắt buộc, nhưng nên continue để tránh tạo request “rác”
                        continue;
                    }
                }

                // === XỬ LÝ HÌNH ẢNH (CỘT N - index 13) ===
                // Lưu ý: hiện tại ảnh vẫn được lưu ngay khi đọc (nếu fail validate các dòng sau, ảnh vẫn nằm trên disk)
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

                // === XỬ LÝ DEPARTMENT (CHỈ CHECK TỒN TẠI + LẤY INFO TỪ DB) ===
                String deptId = null;
                String deptNameFromDb = null;

                String deptNameTrim = (deptName == null) ? null : deptName.trim();
                if (deptNameTrim != null && !deptNameTrim.isEmpty()) {
                    Department dept = departmentRepository.findByDepartmentName(deptNameTrim);

                    if (dept == null) {
                        validationErrors.add("Row " + (i + 1) + ": Department '" + deptNameTrim + "' does not exist.");
                        continue; // không tạo request cho dòng này
                    }

                    deptId = dept.getId();
                    deptNameFromDb = dept.getDepartmentName(); // ✅ dùng tên chuẩn từ DB
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
                        ndr.setId(deptId);
                        ndr.setName(deptNameFromDb); // ✅ lấy từ DB
                        ndr.setQty(requestQty);
                        ndr.setBuy(buy);
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
                    dr.setId(deptId);
                    dr.setName(deptNameFromDb); // ✅ lấy từ DB
                    dr.setQty(requestQty);
                    dr.setBuy(buy);
                    deptList.add(dr);
                }
                req.setDepartmentRequisitions(deptList);

                requisitions.add(req);
                currentMap.put(groupKey, requisitions.size() - 1);
            }

            // ✅ Nếu có bất kỳ lỗi validate nào -> KHÔNG LƯU GÌ
            if (!validationErrors.isEmpty()) {
                return badRequest("Validation failed:\n" + String.join("\n", validationErrors));
            }

            if (requisitions.isEmpty()) {
                return badRequest("No valid data found starting from row 9 (column B).");
            }

            List<RequisitionMonthly> saved = requisitionMonthlyRepository.saveAll(requisitions);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            return badRequest("Error processing file: " + e.getMessage());
        }
    }

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
}