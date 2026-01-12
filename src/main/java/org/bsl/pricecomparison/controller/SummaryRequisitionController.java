package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.bsl.pricecomparison.common.CommonRequisitionUtils;
import org.bsl.pricecomparison.dto.*;
import org.bsl.pricecomparison.enums.RequisitionType;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.*;
import org.bsl.pricecomparison.request.*;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.bsl.pricecomparison.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.springframework.data.domain.*;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestPart;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RestController
@RequestMapping("/api/summary-requisitions")
public class SummaryRequisitionController {

    private final SummaryRequisitionRepository requisitionRepository;
    private final SupplierProductRepository supplierProductRepository;

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private RequisitionMonthlyRepository requisitionMonthlyRepository;

    @Autowired
    private SupplierProductRepository supplierRepository;

    @Autowired
    private UserService userService;

    @Autowired
    public SummaryRequisitionController(SummaryRequisitionRepository requisitionRepository,
                                        SupplierProductRepository supplierProductRepository) {
        this.requisitionRepository = requisitionRepository;
        this.supplierProductRepository = supplierProductRepository;
    }

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private ProductType2Service productType2Service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductType1Repository productType1Repository;

    @Autowired
    private ProductType2Repository productType2Repository;

    @Autowired
    private GroupSummaryRequisitionService groupSummaryRequisitionService;

    @Autowired
    private RequisitionMonthlyController requisitionMonthlyController;

    @Autowired
    private CommonRequisitionUtils commonRequisitionUtils;

    @GetMapping
    public List<SummaryRequisition> getAll() {
        return requisitionRepository.findAll();
    }

    @GetMapping("/{id}")
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

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new summary requisition with multiple image uploads",
            description = "Create a summary requisition and upload multiple images using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UpdateSummaryRequisitionRequest.class)
                    )
            )
    )
    @Transactional
    public ResponseEntity<?> create(
            @ModelAttribute UpdateSummaryRequisitionRequest request,
            @RequestParam("email") String email // ✅ ADD (không sửa logic)
    ) {
        try {
            // ✅ email required (giống bulk completion)
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "email is required and cannot be empty."));
            }
            final String emailNorm = email.trim().toLowerCase();

            if (request.getVietnameseName() == null || request.getVietnameseName().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Vietnamese name is required."));
            }
            if (request.getOldSapCode() == null || request.getOldSapCode().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Old SAP Code is required."));
            }
            if (request.getGroupId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Group ID is required."));
            }

            ObjectMapper mapper = new ObjectMapper();
            List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptList;
            try {
                if (request.getDepartmentRequisitions() == null || request.getDepartmentRequisitions().trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Department requisitions cannot be empty."));
                }
                deptList = mapper.readValue(
                        request.getDepartmentRequisitions(),
                        new TypeReference<List<DepartmentRequisitionMonthly.DepartmentRequestDTO>>() {}
                );
                if (deptList.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "At least one department requisition is required."));
                }
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid JSON format for departmentRequisitions: " + e.getMessage()));
            }

            // Validate BigDecimal qty and buy
            for (DepartmentRequisitionMonthly.DepartmentRequestDTO dept : deptList) {
                if (dept.getBuy() != null && dept.getBuy().compareTo(BigDecimal.ZERO) < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Buy value cannot be negative for department: " + dept.getId()));
                }
                if (dept.getQty() != null && dept.getQty().compareTo(BigDecimal.ZERO) < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Qty value cannot be negative for department: " + dept.getId()));
                }
            }

            // ✅ unit required (rule mới: groupId + unit + key)
            if (request.getUnit() == null || request.getUnit().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "unit is required."));
            }

            String groupId = request.getGroupId().trim();
            String unit = request.getUnit().trim();

            // ✅ DUPLICATE CHECK (NEW RULE): groupId + unit + (SAP/HANA/VN/EN by priority)
            Optional<RequisitionMonthly> dup = commonRequisitionUtils.checkExistsInGroupWithUnitByPriority(
                    groupId,
                    unit,
                    request.getOldSapCode(),        // SAP
                    request.getHanaSapCode(),       // HANA
                    request.getVietnameseName(),    // Des VN
                    request.getEnglishName(),       // Des EN
                    null                            // create => no currentId
            );

            if (dup.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "Duplicate in same group with same unit (priority: SAP -> HANA -> VN -> EN).",
                                "duplicateId", dup.get().getId()
                        ));
            }

            List<String> imageUrls = new ArrayList<>();
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        if (!file.getContentType().startsWith("image/")) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("message", "Only image files are allowed: " + file.getOriginalFilename()));
                        }
                        if (file.getSize() > 5 * 1024 * 1024) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("message", "Image size exceeds 5MB: " + file.getOriginalFilename()));
                        }
                        String imageUrl = saveImage(file);
                        imageUrls.add(imageUrl);
                    }
                }
            }
            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }

            SummaryRequisition summary = new SummaryRequisition();
            summary.setEnglishName(request.getEnglishName());
            summary.setVietnameseName(request.getVietnameseName().trim());
            summary.setOldSapCode(request.getOldSapCode().trim());
            summary.setHanaSapCode(request.getHanaSapCode() != null ? request.getHanaSapCode().trim() : null);
            summary.setReason(request.getReason());
            summary.setRemark(request.getRemark());
            summary.setRemarkComparison(request.getRemarkComparison());
            summary.setSupplierId(request.getSupplierId());
            summary.setGroupId(request.getGroupId());
            summary.setProductType1Id(request.getProductType1Id());
            summary.setProductType2Id(request.getProductType2Id());
            summary.setFullDescription(request.getFullDescription());
            summary.setStock(request.getStock());
            summary.setImageUrls(imageUrls);
            summary.setType(RequisitionType.WEEKLY);
            summary.setCreatedAt(LocalDateTime.now());
            summary.setUpdatedAt(LocalDateTime.now());

            try {
                RequisitionMonthly monthly = buildRequisitionMonthly(summary, request, deptList, imageUrls);

                // ✅ APPLY AUDIT EMAIL (không sửa logic tính toán)
                monthly.setCreatedByEmail(emailNorm);
                monthly.setUpdatedByEmail(emailNorm);

                RequisitionMonthly savedMonthly = requisitionMonthlyRepository.save(monthly);

                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of(
                                "message", "Requisition created successfully!",
                                "data", savedMonthly
                        ));

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to save requisition: " + e.getMessage()));
            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error processing file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error: " + e.getMessage()));
        }
    }

    private RequisitionMonthly buildRequisitionMonthly(
            SummaryRequisition summary,
            UpdateSummaryRequisitionRequest request,
            List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptList,
            List<String> imageUrls) {

        RequisitionMonthly monthly = new RequisitionMonthly();

        monthly.setGroupId(summary.getGroupId());
        monthly.setItemDescriptionEN(summary.getEnglishName());
        monthly.setItemDescriptionVN(summary.getVietnameseName());
        monthly.setOldSAPCode(summary.getOldSapCode());
        monthly.setHanaSAPCode(summary.getHanaSapCode());
        monthly.setFullDescription(summary.getFullDescription());
        monthly.setReason(summary.getReason());
        monthly.setRemark(summary.getRemark());
        monthly.setRemarkComparison(summary.getRemarkComparison());
        monthly.setProductType1Id(summary.getProductType1Id());
        monthly.setProductType2Id(summary.getProductType2Id());
        monthly.setStock(summary.getStock());
        monthly.setType(RequisitionType.WEEKLY);
        monthly.setCreatedDate(LocalDateTime.now());
        monthly.setUpdatedDate(LocalDateTime.now());

        // === SUPPLIER ===
        if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
            SupplierProduct supplier = supplierRepository.findById(request.getSupplierId()).orElse(null);
            if (supplier != null) {
                monthly.setSupplierId(supplier.getId());
                monthly.setSupplierName(supplier.getSupplierName());
                monthly.setUnit(supplier.getUnit());
                monthly.setPrice(supplier.getPrice() != null ? supplier.getPrice() : BigDecimal.ZERO);
                monthly.setGoodType(supplier.getGoodType() != null ? supplier.getGoodType() : "");
                monthly.setCurrency(supplier.getCurrency() != null ? supplier.getCurrency() : "VND");
            }
        }

        // === DEPARTMENT REQUISITIONS ===
        List<DepartmentRequisitionMonthly> requisitions = deptList.stream()
                .map(dto -> new DepartmentRequisitionMonthly(
                        dto.getId(),
                        dto.getName(),
                        dto.getQty() != null ? dto.getQty() : BigDecimal.ZERO,
                        dto.getBuy() != null ? dto.getBuy() : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
        monthly.setDepartmentRequisitions(requisitions);

        // === TÍNH TOÁN MỚI ===
        // 1. totalRequestQty = sum(qty)
        BigDecimal totalRequestQty = requisitions.stream()
                .map(DepartmentRequisitionMonthly::getQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        monthly.setTotalRequestQty(totalRequestQty);

        // 2. orderQty = sum(buy)
        BigDecimal orderQty = requisitions.stream()
                .map(DepartmentRequisitionMonthly::getBuy)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        monthly.setOrderQty(orderQty);

        // 3. amount = orderQty * price
        BigDecimal price = monthly.getPrice() != null ? monthly.getPrice() : BigDecimal.ZERO;
        BigDecimal amount = orderQty.multiply(price);
        monthly.setAmount(amount);

        monthly.setImageUrls(new ArrayList<>(imageUrls));

        return monthly;
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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update requisition",
            description = "Uses same logic as create: orderQty = sum(buy), amount = orderQty * price. Unit is updated from request (NOT supplier). If supplierId missing/blank -> clear supplier fields, price=0, amount=0."
    )
    public ResponseEntity<?> update(
            @PathVariable String id,
            @ModelAttribute UpdateSummaryRequisitionRequest request,
            @RequestParam("email") String email
    ) {
        try {
            // ✅ email required
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "email is required and cannot be empty."));
            }
            final String emailNorm = email.trim().toLowerCase();

            Optional<RequisitionMonthly> opt = requisitionMonthlyRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Requisition not found."));
            }
            RequisitionMonthly current = opt.get();

            // =====================================================
            // ✅ DUPLICATE CHECK (NEW RULE):
            // only if supplierId exists, and priority oldSap -> hana -> VN -> EN
            // ignore empty/"NEW" (case-insensitive)
            // check by "after update" values
            // =====================================================
            String nextGroupId = (request.getGroupId() != null && !request.getGroupId().isBlank())
                    ? request.getGroupId().trim()
                    : (current.getGroupId() != null ? current.getGroupId().trim() : null);

            String nextOldSap = (request.getOldSapCode() != null)
                    ? request.getOldSapCode() // giữ đúng logic pick: client gửi null mới fallback
                    : current.getOldSAPCode();

            String nextHana = (request.getHanaSapCode() != null)
                    ? request.getHanaSapCode()
                    : current.getHanaSAPCode();

            String nextEN = (request.getEnglishName() != null)
                    ? request.getEnglishName()
                    : current.getItemDescriptionEN();

            String nextVN = (request.getVietnameseName() != null)
                    ? request.getVietnameseName()
                    : current.getItemDescriptionVN();

// ✅ unit AFTER update: request có => dùng, không => giữ unit hiện tại
            String nextUnit = (request.getUnit() != null)
                    ? request.getUnit().trim()
                    : (current.getUnit() != null ? current.getUnit().trim() : null);

// ✅ groupId required
            if (nextGroupId == null || nextGroupId.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "groupId is required"));
            }

// ✅ unit required (vì rule check tồn tại có unit)
            if (nextUnit == null || nextUnit.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "unit is required"));
            }

// ✅ dùng CommonRequisitionUtils (fun chung) để check duplicate
            Optional<RequisitionMonthly> dup = commonRequisitionUtils.checkExistsInGroupWithUnitByPriority(
                    nextGroupId,
                    nextUnit,
                    nextOldSap,
                    nextHana,
                    nextVN,
                    nextEN,
                    id // exclude chính nó
            );

            if (dup.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "message", "Duplicate in same group with same unit (priority: SAP -> HANA -> VN -> EN).",
                                "duplicateId", dup.get().getId()
                        ));
            }

            // =====================================================
            // ✅ IMAGE HANDLING
            // =====================================================
            List<String> imageUrls = current.getImageUrls() != null
                    ? new ArrayList<>(current.getImageUrls())
                    : new ArrayList<>();

            List<MultipartFile> files = request.getFiles() != null ? request.getFiles() : new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Only image files allowed: " + file.getOriginalFilename()));
                    }
                    if (file.getSize() > 5 * 1024 * 1024) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Image size > 5MB: " + file.getOriginalFilename()));
                    }
                    String url = saveImage(file);
                    if (url != null) imageUrls.add(url);
                }
            }

            if (request.getImagesToDelete() != null && !request.getImagesToDelete().isBlank()) {
                try {
                    List<String> toDelete = objectMapper.readValue(
                            request.getImagesToDelete(),
                            new TypeReference<List<String>>() {}
                    );

                    for (String url : toDelete) {
                        if (url != null && url.startsWith("/uploads/") && imageUrls.contains(url)) {
                            imageUrls.remove(url);
                            deleteImage(url);
                        }
                    }
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid imagesToDelete JSON."));
                }
            }

            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }

            // =====================================================
            // ✅ DEPARTMENT REQUISITIONS
            // =====================================================
            List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptList;

            if (request.getDepartmentRequisitions() != null && !request.getDepartmentRequisitions().isBlank()) {
                try {
                    deptList = objectMapper.readValue(
                            request.getDepartmentRequisitions(),
                            new TypeReference<List<DepartmentRequisitionMonthly.DepartmentRequestDTO>>() {}
                    );

                    for (var d : deptList) {
                        if (d.getBuy() != null && d.getBuy().compareTo(BigDecimal.ZERO) < 0) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("message", "Buy value cannot be negative."));
                        }
                        if (d.getQty() != null && d.getQty().compareTo(BigDecimal.ZERO) < 0) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("message", "Qty value cannot be negative."));
                        }
                    }
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid departmentRequisitions JSON."));
                }
            } else {
                deptList = current.getDepartmentRequisitions().stream()
                        .map(d -> new DepartmentRequisitionMonthly.DepartmentRequestDTO(
                                d.getId(),
                                d.getName(),
                                d.getQty() != null ? d.getQty() : BigDecimal.ZERO,
                                d.getBuy() != null ? d.getBuy() : BigDecimal.ZERO
                        ))
                        .collect(Collectors.toList());
            }

            // =====================================================
            // ✅ TEMP SUMMARY
            // =====================================================
            SummaryRequisition tempSummary = new SummaryRequisition();
            tempSummary.setGroupId(nextGroupId);
            tempSummary.setEnglishName(nextEN);
            tempSummary.setVietnameseName(nextVN);
            tempSummary.setOldSapCode(nextOldSap);
            tempSummary.setHanaSapCode(nextHana);
            tempSummary.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : current.getFullDescription());
            tempSummary.setReason(request.getReason() != null ? request.getReason() : current.getReason());
            tempSummary.setRemark(request.getRemark() != null ? request.getRemark() : current.getRemark());
            tempSummary.setRemarkComparison(request.getRemarkComparison() != null ? request.getRemarkComparison() : current.getRemarkComparison());
            tempSummary.setProductType1Id(request.getProductType1Id() != null ? request.getProductType1Id() : current.getProductType1Id());
            tempSummary.setProductType2Id(request.getProductType2Id() != null ? request.getProductType2Id() : current.getProductType2Id());
            tempSummary.setStock(
                    request.getStock() != null && request.getStock().compareTo(BigDecimal.ZERO) != 0
                            ? request.getStock()
                            : current.getStock()
            );
            tempSummary.setType(RequisitionType.WEEKLY);

            // =====================================================
            // ✅ BUILD theo logic create
            // =====================================================
            RequisitionMonthly updated = buildRequisitionMonthly(tempSummary, request, deptList, imageUrls);

            // =====================================================
            // ✅ GHI ĐÈ ID + createdDate
            // =====================================================
            updated.setId(current.getId());
            updated.setCreatedDate(current.getCreatedDate());
            updated.setUpdatedDate(LocalDateTime.now());

            // ✅ audit
            updated.setUpdatedByEmail(emailNorm);
            updated.setCreatedByEmail(
                    (current.getCreatedByEmail() != null && !current.getCreatedByEmail().isBlank())
                            ? current.getCreatedByEmail()
                            : emailNorm
            );

            // ✅ STOCK mapping
            if (request.getStock() != null) {
                updated.setDailyMedInventory(request.getStock());
            } else {
                updated.setDailyMedInventory(current.getDailyMedInventory());
            }

            // =====================================================
            // ✅ UNIT UPDATE từ REQUEST (GIỐNG UI EDIT)
            // - request có unit => set unit mới
            // - request không gửi unit => giữ unit hiện tại
            // =====================================================
            if (request.getUnit() != null) {
                updated.setUnit(request.getUnit()); // ✅ dùng unit từ client
            } else {
                updated.setUnit(current.getUnit()); // ✅ fallback giữ unit cũ
            }

            // =====================================================
            // ✅ SUPPLIER RULES (không set unit từ supplier)
            // =====================================================
            boolean noSupplierIdProvided = (request.getSupplierId() == null || request.getSupplierId().isBlank());

            if (noSupplierIdProvided) {
                updated.setSupplierId(null);
                updated.setSupplierName(null);
                updated.setGoodType("");

                updated.setCurrency(current.getCurrency());
                updated.setPrice(BigDecimal.ZERO);

                BigDecimal oq = updated.getOrderQty() != null ? updated.getOrderQty() : BigDecimal.ZERO;
                updated.setAmount(oq.multiply(BigDecimal.ZERO));
            } else {
                SupplierProduct supplier = supplierRepository.findById(request.getSupplierId()).orElse(null);
                if (supplier == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Supplier not found for supplierId: " + request.getSupplierId()));
                }

                updated.setSupplierId(supplier.getId());
                updated.setSupplierName(supplier.getSupplierName());
                updated.setGoodType(supplier.getGoodType() != null ? supplier.getGoodType() : "");
                updated.setCurrency(supplier.getCurrency() != null ? supplier.getCurrency() : "VND");

                BigDecimal price = supplier.getPrice() != null ? supplier.getPrice() : BigDecimal.ZERO;
                updated.setPrice(price);

                BigDecimal oq = updated.getOrderQty() != null ? updated.getOrderQty() : BigDecimal.ZERO;
                updated.setAmount(oq.multiply(price));
            }

            // =====================================================
            // ✅ SAVE
            // =====================================================
            RequisitionMonthly saved = requisitionMonthlyRepository.save(updated);

            UpdateRequisitionMonthlyDTO dto = new UpdateRequisitionMonthlyDTO(
                    saved.getId(),
                    saved.getGroupId(),
                    saved.getProductType1Id(),
                    saved.getProductType2Id(),
                    saved.getItemDescriptionEN(),
                    saved.getItemDescriptionVN(),
                    saved.getOldSAPCode(),
                    saved.getHanaSAPCode(),
                    saved.getUnit(),
                    saved.getDepartmentRequisitions(),
                    saved.getDailyMedInventory(),
                    saved.getTotalRequestQty(),
                    saved.getOrderQty(),
                    saved.getAmount(),
                    saved.getPrice(),
                    saved.getSupplierName(),
                    saved.getCreatedDate(),
                    saved.getUpdatedDate(),
                    saved.getFullDescription(),
                    saved.getReason(),
                    saved.getRemark(),
                    saved.getRemarkComparison(),
                    saved.getImageUrls(),
                    null,
                    null,
                    null
            );

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing request.", "error", e.getMessage()));
        }
    }



    private void deleteImage(String imageUrl) {
        try {
            String fileName = imageUrl.replace("/uploads/", "");
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete image: " + imageUrl + ", error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            Optional<RequisitionMonthly> requisitionOptional = requisitionMonthlyRepository.findById(id);
            if (requisitionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "ID not found: " + id));
            }

            RequisitionMonthly requisition = requisitionOptional.get();
            String name = requisition.getItemDescriptionEN(); // Map itemDescriptionEN to englishName

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
            return ResponseEntity.ok(Collections.singletonMap("message", "Name '" + name + "' has been deleted"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("message", "Invalid ID: " + id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Deletion error: " + e.getMessage()));
        }
    }

    @GetMapping("/with-suppliers")
    public List<SummaryRequisitionWithSupplierDTO> getAllWithSupplierInfo() {
        List<SummaryRequisition> requisitions = requisitionRepository.findAll();

        return requisitions.stream()
                .map(req -> {
                    // Tra cứu SupplierProduct
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository
                                .findById(req.getSupplierId())
                                .orElse(null);
                    }

                    // Chuyển đổi departmentRequestQty thành List<DepartmentQtyDTO>
                    List<DepartmentQtyDTO> departmentQtyDTOs = req.getDepartmentRequestQty().entrySet().stream()
                            .map(entry -> {
                                String departmentId = entry.getKey();
                                DepartmentQty deptQty = entry.getValue();
                                // Tra cứu departmentName từ DepartmentRepository
                                String departmentName = departmentRepository.findById(departmentId)
                                        .map(Department::getDepartmentName)
                                        .orElse("Unknown Department");
                                return new DepartmentQtyDTO(departmentId, departmentName, deptQty.getQty(), deptQty.getBuy());
                            })
                            .collect(Collectors.toList());

                    // Tạo DTO với departmentRequestQuantities
                    return new SummaryRequisitionWithSupplierDTO(req, supplierProduct, departmentQtyDTOs);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search requisitions by group ID and optional filters",
            description = "Lấy từ bảng RequisitionMonthly, convert về SummaryRequisitionDTO. Hỗ trợ filter, pagination, disablePagination."
    )
    public ResponseEntity<Page<SummaryRequisitionDTO>> searchComparison(
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

        List<RequisitionMonthly> allMonthly = requisitionMonthlyRepository.findByGroupId(groupId);

        List<RequisitionMonthly> filtered = allMonthly.stream()
                .filter(req -> {
                    if (!hasFilter) return true;

                    boolean matches = true;

                    // Product Type Names
                    String p1Name = Optional.ofNullable(req.getProductType1Id())
                            .filter(id -> !id.isEmpty())
                            .map(id -> Optional.ofNullable(productType1Service.getById(id))
                                    .map(ProductType1::getName)
                                    .orElse(null))
                            .orElse(null);
                    String p2Name = Optional.ofNullable(req.getProductType2Id())
                            .filter(id -> !id.isEmpty())
                            .map(id -> Optional.ofNullable(productType2Service.getById(id))
                                    .map(ProductType2::getName)
                                    .orElse(null))
                            .orElse(null);

                    // Department Names
                    List<String> deptNames = req.getDepartmentRequisitions().stream()
                            .map(DepartmentRequisitionMonthly::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    if (productType1Name != null && !productType1Name.isEmpty()) {
                        matches &= p1Name != null && p1Name.toLowerCase().contains(productType1Name.toLowerCase());
                    }
                    if (productType2Name != null && !productType2Name.isEmpty()) {
                        matches &= p2Name != null && p2Name.toLowerCase().contains(productType2Name.toLowerCase());
                    }
                    if (englishName != null && !englishName.isEmpty()) {
                        matches &= req.getItemDescriptionEN() != null && req.getItemDescriptionEN().toLowerCase().contains(englishName.toLowerCase());
                    }
                    if (vietnameseName != null && !vietnameseName.isEmpty()) {
                        matches &= req.getItemDescriptionVN() != null && req.getItemDescriptionVN().toLowerCase().contains(vietnameseName.toLowerCase());
                    }
                    if (oldSapCode != null && !oldSapCode.isEmpty()) {
                        matches &= req.getOldSAPCode() != null && req.getOldSAPCode().toLowerCase().contains(oldSapCode.toLowerCase());
                    }
                    if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                        matches &= req.getHanaSAPCode() != null && req.getHanaSAPCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                    }
                    if (supplierName != null && !supplierName.isEmpty()) {
                        matches &= req.getSupplierName() != null && req.getSupplierName().toLowerCase().contains(supplierName.toLowerCase());
                    }
                    if (departmentName != null && !departmentName.isEmpty()) {
                        matches &= deptNames.stream().anyMatch(d -> d.toLowerCase().contains(departmentName.toLowerCase()));
                    }

                    return matches;
                })
                .sorted((a, b) -> {
                    LocalDateTime d1 = a.getUpdatedDate() != null ? a.getUpdatedDate() : a.getCreatedDate() != null ? a.getCreatedDate() : LocalDateTime.MIN;
                    LocalDateTime d2 = b.getUpdatedDate() != null ? b.getUpdatedDate() : b.getCreatedDate() != null ? b.getCreatedDate() : LocalDateTime.MIN;
                    return d2.compareTo(d1); // Mới nhất trước
                })
                .collect(Collectors.toList());

        // === CONVERT TỪ RequisitionMonthly → SummaryRequisitionDTO ===
        List<SummaryRequisitionDTO> dtoList = filtered.stream()
                .map(this::convertMonthlyToSummaryDTO)
                .collect(Collectors.toList());

        // === PAGINATION ===
        Page<SummaryRequisitionDTO> resultPage;
        if (disablePagination) {
            resultPage = new PageImpl<>(dtoList, PageRequest.of(0, dtoList.size()), dtoList.size());
        } else {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), dtoList.size());
            List<SummaryRequisitionDTO> pagedList = dtoList.subList(start, end);
            resultPage = new PageImpl<>(pagedList, pageable, dtoList.size());
        }

        return ResponseEntity.ok(resultPage);
    }

    private SummaryRequisitionDTO convertMonthlyToSummaryDTO(RequisitionMonthly monthly) {
        // 1. Tạo SummaryRequisition giả từ Monthly
        SummaryRequisition fakeSummary = new SummaryRequisition();
        fakeSummary.setId(monthly.getId());
        fakeSummary.setGroupId(monthly.getGroupId());
        fakeSummary.setEnglishName(monthly.getItemDescriptionEN());
        fakeSummary.setVietnameseName(monthly.getItemDescriptionVN());
        fakeSummary.setOldSapCode(monthly.getOldSAPCode());
        fakeSummary.setHanaSapCode(monthly.getHanaSAPCode());
        fakeSummary.setProductType1Id(monthly.getProductType1Id());
        fakeSummary.setProductType2Id(monthly.getProductType2Id());
        fakeSummary.setSupplierId(monthly.getSupplierId());
        fakeSummary.setOrderQty(monthly.getOrderQty());
        fakeSummary.setCreatedAt(monthly.getCreatedDate());
        fakeSummary.setUpdatedAt(monthly.getUpdatedDate());
        fakeSummary.setRemark(monthly.getRemark());
        fakeSummary.setRemarkComparison(monthly.getRemarkComparison());
        fakeSummary.setReason(monthly.getReason());
        fakeSummary.setImageUrls(monthly.getImageUrls());
        fakeSummary.setFullDescription(monthly.getFullDescription());
        fakeSummary.setStock(monthly.getStock()); // Correct: Uses stock instead of safeStock
        fakeSummary.setCompletedDate(monthly.getCompletedDate());
        fakeSummary.setIsCompleted(monthly.getIsCompleted());

        // 2. Tạo map departmentRequestQty (id -> DepartmentQty)
        Map<String, DepartmentQty> deptMap = new HashMap<>();
        for (DepartmentRequisitionMonthly d : monthly.getDepartmentRequisitions()) {
            DepartmentQty deptQty = new DepartmentQty();
            deptQty.setQty(d.getQty() != null ? d.getQty() : BigDecimal.ZERO);
            deptQty.setBuy(d.getBuy() != null ? d.getBuy() : BigDecimal.ZERO);
            deptMap.put(d.getId(), deptQty);
        }
        fakeSummary.setDepartmentRequestQty(deptMap);

        // 3. Lấy SupplierProduct
        SupplierProduct supplier = monthly.getSupplierId() != null
                ? supplierProductRepository.findById(monthly.getSupplierId()).orElse(null)
                : null;

        // 4. Tính tổng buy
        BigDecimal sumBuy = monthly.getDepartmentRequisitions().stream()
                .map(d -> d.getBuy() != null ? d.getBuy() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Tính tổng qty
        BigDecimal sumQty = monthly.getDepartmentRequisitions().stream()
                .map(d -> d.getQty() != null ? d.getQty() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Tính totalPrice = orderQty * price
        BigDecimal totalPrice = (monthly.getPrice() != null && monthly.getOrderQty() != null)
                ? monthly.getPrice().multiply(monthly.getOrderQty())
                : BigDecimal.ZERO;

        // 7. Tạo departmentRequests DTO (có id, name, qty, buy)
        List<SummaryRequisitionDTO.DepartmentRequestDTO> deptDTOs = monthly.getDepartmentRequisitions().stream()
                .map(d -> {
                    Department dept = departmentRepository.findById(d.getId()).orElse(null);
                    String deptName = dept != null ? dept.getDepartmentName() : d.getName();
                    return new SummaryRequisitionDTO.DepartmentRequestDTO(
                            d.getId(),
                            deptName,
                            d.getQty(),
                            d.getBuy()
                    );
                })
                .collect(Collectors.toList());

        // 8. Lấy tên ProductType
        String p1Name = Optional.ofNullable(monthly.getProductType1Id())
                .filter(id -> !id.isEmpty())
                .map(id -> Optional.ofNullable(productType1Service.getById(id))
                        .map(ProductType1::getName)
                        .orElse(null))
                .orElse(null);
        String p2Name = Optional.ofNullable(monthly.getProductType2Id())
                .filter(id -> !id.isEmpty())
                .map(id -> Optional.ofNullable(productType2Service.getById(id))
                        .map(ProductType2::getName)
                        .orElse(null))
                .orElse(null);

        // 9. Tạo DTO
        return new SummaryRequisitionDTO(
                fakeSummary,
                supplier,
                deptDTOs,
                p1Name,
                p2Name,
                sumQty,
                sumBuy,
                monthly.getStock(),
                monthly.getOrderQty(),
                totalPrice,
                monthly.getPrice(),
                supplier != null ? supplier.getCurrency() : monthly.getCurrency(),
                supplier != null ? supplier.getGoodType() : monthly.getGoodType(),
                monthly.getSupplierId(),
                supplier != null ? supplier.getSupplierName() : monthly.getSupplierName(),
                monthly.getProductType1Id(),
                monthly.getProductType2Id(),
                monthly.getImageUrls(),
                monthly.getFullDescription(),
                monthly.getReason(),
                monthly.getRemarkComparison(),
                monthly.getCreatedDate() != null ? monthly.getCreatedDate().toString() : null,
                monthly.getUpdatedDate() != null ? monthly.getUpdatedDate().toString() : null,
                monthly.getUnit()
        );
    }

    @PostMapping(value = "/upload-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload Requisition File (Weekly)",
            description = "Upload Excel .xlsx file. Data starts from row 6, column B (Item VN). " +
                    "Columns: B=Item VN, C=Item EN, D=Old SAP, E=Hana Code (if any), " +
                    "H=Request, I=Inhand, J=Buy, K=Unit, N=Dept request, O=Reason, P=Picture"
    )
    public ResponseEntity<List<RequisitionMonthly>> uploadRequisitionFile(
            @Parameter(description = "Excel .xlsx file") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Group ID", required = true) @RequestParam("groupId") String groupId
    ) {

        // ✅ gom lỗi validate — chỉ khi không có lỗi mới saveAll
        List<String> validationErrors = new ArrayList<>();

        // ✅ validate input
        if (groupId == null || groupId.isBlank()) {
            return badRequest("groupId is required.");
        }
        if (file == null || file.isEmpty()) {
            return badRequest("No file uploaded.");
        }
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            return badRequest("Only .xlsx files are allowed.");
        }

        // ✅ 1) LẤY DỮ LIỆU ĐÃ TỒN TẠI TRONG DB (chỉ trong 1 groupId)
        List<RequisitionMonthly> allInDb = requisitionMonthlyRepository.findByGroupId(groupId);

        // ✅ Build set key đã tồn tại theo rule CHUNG: UNIT + (SAP -> HANA -> VN -> EN)
        Set<String> existingKeys = allInDb.stream()
                .map(CommonRequisitionUtils::buildExistingKeyFromDb)
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.toSet());

        List<RequisitionMonthly> requisitions = new ArrayList<>();
        Map<String, Integer> fileKeyMap = new HashMap<>(); // <rowKey, index>

        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return badRequest("Sheet not found.");
            }

            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

            // === BẮT ĐẦU TỪ DÒNG 6 (index = 5) ===
            for (int i = 5; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // B (1) Item VN
                Cell itemVNCell = getMergedCellValue(sheet, i, 1, mergedRegions);
                String itemVN = getCellValue(itemVNCell);

                // C (2) Item EN
                Cell itemENCell = getMergedCellValue(sheet, i, 2, mergedRegions);
                String itemEN = getCellValue(itemENCell);

                // D (3) Old SAP
                Cell oldSapCell = getMergedCellValue(sheet, i, 3, mergedRegions);
                String oldSapRaw = getCellValue(oldSapCell);

                // E (4) Hana Code
                Cell hanaCodeCell = getMergedCellValue(sheet, i, 4, mergedRegions);
                String hanaCodeRaw = getCellValue(hanaCodeCell);

                // Normalize NEW (new/New/NEW -> NEW)
                String oldSap = normalizeNew(oldSapRaw);     // null / "NEW" / value
                String hanaCode = normalizeNew(hanaCodeRaw); // null / "NEW" / value

                // Nếu dòng không có dữ liệu nhận diện nào -> skip
                boolean hasAnyId =
                        isUsableKey(oldSap) || isUsableKey(hanaCode) ||
                                (itemVN != null && !itemVN.trim().isEmpty()) ||
                                (itemEN != null && !itemEN.trim().isEmpty());
                if (!hasAnyId) continue;

                // H (7) Request, I (8) Inhand, J (9) Buy, K (10) Unit
                Cell requestCell = getMergedCellValue(sheet, i, 7, mergedRegions);
                Cell inhandCell = getMergedCellValue(sheet, i, 8, mergedRegions);
                Cell unitCell = getMergedCellValue(sheet, i, 10, mergedRegions); // Column K

                // N (13) Dept request, O (14) Reason
                Cell deptCell = getMergedCellValue(sheet, i, 13, mergedRegions);
                Cell reasonCell = getMergedCellValue(sheet, i, 14, mergedRegions);

                BigDecimal requestQty = parseBigDecimal(requestCell);
                if (requestQty == null || requestQty.compareTo(BigDecimal.ZERO) < 0) requestQty = BigDecimal.ZERO;

                BigDecimal inhand = parseBigDecimal(inhandCell);
                if (inhand == null || inhand.compareTo(BigDecimal.ZERO) < 0) inhand = BigDecimal.ZERO;

                // Buy = request - inhand
                BigDecimal buy = requestQty.subtract(inhand).max(BigDecimal.ZERO);

                String unit = getCellValue(unitCell);
                String deptName = getCellValue(deptCell);
                String reason = getCellValue(reasonCell);

                // ✅ unit required (theo rule mới)
                if (unit == null || unit.trim().isEmpty()) {
                    validationErrors.add("Row " + (i + 1) + ": Unit is required (column K).");
                    continue;
                }

                // ✅ KEY CHUNG: UNIT + (SAP -> HANA -> VN -> EN)
                String key = CommonRequisitionUtils.buildRowKeyWithUnitByPriority(
                        unit,
                        oldSap,
                        hanaCode,
                        itemVN,
                        itemEN
                );

                if (key == null) {
                    validationErrors.add("Row " + (i + 1) + ": Cannot build merge key (UNIT + SAP/HANA/VN/EN all empty).");
                    continue;
                }

                // ✅ Check trùng DB (trong groupId)
                if (existingKeys.contains(key)) {
                    validationErrors.add("Row " + (i + 1) + ": Duplicate record exists in this group with key = " + key);
                    continue;
                }

                // ✅ Check trùng trong file (weekly hiện đang merge, bạn muốn giống monthly hay merge?)
                // Ở đây giữ đúng style weekly hiện tại: merge theo key
                // Nếu bạn muốn "trùng trong file cũng là lỗi" thì đổi thành validationErrors.add + continue
                // hoặc return badRequest (fail-fast)
                boolean duplicateInFile = fileKeyMap.containsKey(key);

                // === XỬ LÝ DEPARTMENT (check tồn tại -> lấy name chuẩn từ DB) ===
                String deptId = null;
                String deptNameFromDb = null;

                String deptNameTrim = (deptName == null) ? null : deptName.trim();
                if (deptNameTrim != null && !deptNameTrim.isEmpty()) {
                    Department dept = departmentRepository.findByDepartmentName(deptNameTrim);
                    if (dept == null) {
                        validationErrors.add("Row " + (i + 1) + ": Department '" + deptNameTrim + "' does not exist.");
                        continue;
                    }
                    deptId = dept.getId();
                    deptNameFromDb = dept.getDepartmentName();
                }

                // === XỬ LÝ HÌNH ẢNH (cột P index 15) ===
                List<String> imageUrls = new ArrayList<>();
                for (XSSFShape shape : drawing.getShapes()) {
                    if (shape instanceof XSSFPicture pic) {
                        XSSFClientAnchor anchor = pic.getClientAnchor();

                        int col1 = anchor.getCol1();
                        int col2 = anchor.getCol2();
                        int row1 = anchor.getRow1();
                        int row2 = anchor.getRow2();

                        boolean colMatches = (col1 == 14 || col1 == 15 || (col1 <= 14 && col2 >= 14));
                        boolean rowMatches = (row1 <= i && i <= row2);

                        if (colMatches && rowMatches) {
                            try {
                                byte[] imgBytes = pic.getPictureData().getData();
                                String mimeType = pic.getPictureData().getMimeType();
                                String ext = requisitionMonthlyController.getImageExtension(mimeType);
                                String fileName = "req_weekly_" + groupId + "_" + (i + 1) + "_" + System.currentTimeMillis() + ext;

                                String imgPath = requisitionMonthlyController.saveImage(imgBytes, fileName, mimeType);
                                if (imgPath != null) imageUrls.add(imgPath);
                            } catch (Exception e) {
                                System.err.println("Image save failed at row " + (i + 1) + ": " + e.getMessage());
                            }
                        }
                    }
                }

                // === MERGE TRONG FILE (giữ đúng weekly hiện tại) ===
                if (duplicateInFile) {
                    int idx = fileKeyMap.get(key);
                    RequisitionMonthly existing = requisitions.get(idx);

                    if (existing.getDepartmentRequisitions() == null) {
                        existing.setDepartmentRequisitions(new ArrayList<>());
                    }

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
                        ndr.setName(deptNameFromDb);
                        ndr.setQty(requestQty);
                        ndr.setBuy(buy);
                        existing.getDepartmentRequisitions().add(ndr);
                    }

                    existing.setTotalRequestQty(existing.getTotalRequestQty().add(requestQty));
                    existing.setOrderQty(existing.getOrderQty().add(buy));

                    if (existing.getUnit() == null && unit != null) existing.setUnit(unit);
                    if (existing.getReason() == null && reason != null) existing.setReason(reason);

                    if (existing.getOldSAPCode() == null && oldSap != null) existing.setOldSAPCode(oldSap);
                    if (existing.getHanaSAPCode() == null && hanaCode != null) existing.setHanaSAPCode(hanaCode);

                    if (!imageUrls.isEmpty()) {
                        if (existing.getImageUrls() == null) existing.setImageUrls(new ArrayList<>());
                        existing.getImageUrls().addAll(imageUrls);
                    }

                    continue;
                }

                // === TẠO MỚI ===
                RequisitionMonthly req = new RequisitionMonthly();
                req.setGroupId(groupId);
                req.setCreatedDate(LocalDateTime.now());
                req.setUpdatedDate(LocalDateTime.now());

                req.setOldSAPCode(oldSap);
                req.setHanaSAPCode(hanaCode);

                req.setItemDescriptionVN(itemVN);
                req.setItemDescriptionEN(itemEN);

                req.setTotalRequestQty(requestQty);
                req.setOrderQty(buy);
                req.setDailyMedInventory(inhand);

                req.setUnit(unit);
                req.setReason(reason);

                req.setImageUrls(imageUrls.isEmpty() ? null : imageUrls);

                List<DepartmentRequisitionMonthly> deptList = new ArrayList<>();
                if (deptId != null) {
                    DepartmentRequisitionMonthly dr = new DepartmentRequisitionMonthly();
                    dr.setId(deptId);
                    dr.setName(deptNameFromDb);
                    dr.setQty(requestQty);
                    dr.setBuy(buy);
                    deptList.add(dr);
                }
                req.setDepartmentRequisitions(deptList);

                requisitions.add(req);
                fileKeyMap.put(key, requisitions.size() - 1);
            }

            if (!validationErrors.isEmpty()) {
                return badRequest("Validation failed:\n" + String.join("\n", validationErrors));
            }

            if (requisitions.isEmpty()) {
                return badRequest("No valid data found starting from row 6.");
            }

            List<RequisitionMonthly> saved = requisitionMonthlyRepository.saveAll(requisitions);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            return badRequest("Error processing file: " + e.getMessage());
        }
    }

    /* ===================== HELPERS (giống monthly + cascade) ===================== */

    private String normalizeNew(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        return "NEW".equalsIgnoreCase(t) ? "NEW" : t;
    }

    private boolean isUsableKey(String v) {
        return v != null && !v.trim().isEmpty() && !"NEW".equalsIgnoreCase(v.trim());
    }

    private String safeTrimLower(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t.toLowerCase();
    }

    /**
     * Build key theo rule:
     * - Nếu oldSap usable -> "SAP|<oldSapLower>"
     * - else nếu hana usable -> "HANA|<hanaLower>"
     * - else nếu vn usable -> "VN|<vnLower>"
     * - else nếu en usable -> "EN|<enLower>"
     */
    private String buildCascadeKey(String oldSap, String hana, String vnLower, String enLower) {
        if (isUsableKey(oldSap)) return "SAP|" + oldSap.trim().toLowerCase();
        if (isUsableKey(hana)) return "HANA|" + hana.trim().toLowerCase();
        if (vnLower != null && !vnLower.isEmpty()) return "VN|" + vnLower;
        if (enLower != null && !enLower.isEmpty()) return "EN|" + enLower;
        return null;
    }

    private ResponseEntity<List<RequisitionMonthly>> badRequest(String message) {
        return ResponseEntity.badRequest().body(
                Collections.singletonList(new RequisitionMonthly() {{ setRemark(message); }})
        );
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
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

    private String saveImage(byte[] imageBytes, String originalFileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        String contentType = "image/png"; // Mặc định là PNG, có thể điều chỉnh
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IOException("Only JPEG, PNG, and GIF files are allowed");
        }

        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());
        Files.write(path, imageBytes); // Ghi byte[] trực tiếp vào file

        return "/uploads/" + fileName;
    }


    @GetMapping("/search/comparison")
    @Operation(
            summary = "Search requisitions for comparison by group ID and optional filters",
            description = "Retrieve requisitions by groupId with optional filters. Supports removeDuplicateSuppliers. " +
                    "Returns last purchase info (supplierName, lastPurchaseDate, lastPurchasePrice, lastPurchaseOrderQty) " +
                    "from RequisitionMonthly where isCompleted=true, matched by (SAP/HANA/DES) + currency, excluding current item."
    )
    public ResponseEntity<ComparisonRequisitionResponseDTO> searchRequisitions(
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
            @RequestParam(defaultValue = "false") Boolean removeDuplicateSuppliers,
            Pageable pageable
    ) {
        // ✅ currency lấy từ group
        final String groupCurrency = groupSummaryRequisitionService
                .getGroupSummaryRequisitionById(groupId)
                .map(GroupSummaryRequisition::getCurrency)
                .orElse("")
                .trim();

        final boolean finalRemoveDup = Boolean.TRUE.equals(removeDuplicateSuppliers);

        // Fetch all by groupId
        List<RequisitionMonthly> allMonthly = requisitionMonthlyRepository.findByGroupId(groupId);

        // Map -> Summary + Filter + Sort desc
        List<SummaryRequisition> filtered = allMonthly.stream()
                .map(this::mapToSummaryRequisition)
                .filter(req -> {
                    if (!hasFilter) return true;

                    boolean matches = true;

                    String reqType1Name = getType1Name(req.getProductType1Id());
                    String reqType2Name = getType2Name(req.getProductType2Id());

                    List<String> deptNames = (req.getDepartmentRequestQty() != null)
                            ? req.getDepartmentRequestQty().keySet().stream()
                            .filter(id -> id != null && !id.isBlank())
                            .map(this::getDeptName)
                            .collect(Collectors.toList())
                            : Collections.emptyList();

                    if (productType1Name != null && !productType1Name.isBlank()) {
                        matches = matches && reqType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                    }
                    if (productType2Name != null && !productType2Name.isBlank()) {
                        matches = matches && reqType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                    }
                    if (englishName != null && !englishName.isBlank()) {
                        matches = matches && req.getEnglishName() != null
                                && req.getEnglishName().toLowerCase().contains(englishName.toLowerCase());
                    }
                    if (vietnameseName != null && !vietnameseName.isBlank()) {
                        matches = matches && req.getVietnameseName() != null
                                && req.getVietnameseName().toLowerCase().contains(vietnameseName.toLowerCase());
                    }
                    if (oldSapCode != null && !oldSapCode.isBlank()) {
                        matches = matches && req.getOldSapCode() != null
                                && req.getOldSapCode().toLowerCase().contains(oldSapCode.toLowerCase());
                    }
                    if (hanaSapCode != null && !hanaSapCode.isBlank()) {
                        matches = matches && req.getHanaSapCode() != null
                                && req.getHanaSapCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                    }

                    // supplierName filter: lấy supplierName từ supplierId (NO CACHE)
                    if (supplierName != null && !supplierName.isBlank()) {
                        String sid = normText(req.getSupplierId());
                        SupplierProduct sp = (sid != null)
                                ? supplierProductRepository.findById(sid).orElse(null)
                                : null;

                        String reqSupplierName = (sp != null && sp.getSupplierName() != null) ? sp.getSupplierName() : "";
                        matches = matches && reqSupplierName.toLowerCase().contains(supplierName.toLowerCase());
                    }

                    if (departmentName != null && !departmentName.isBlank()) {
                        matches = matches && deptNames.stream()
                                .anyMatch(d -> d != null && d.toLowerCase().contains(departmentName.toLowerCase()));
                    }

                    return matches;
                })
                .sorted((a, b) -> {
                    LocalDateTime d1 = (a.getUpdatedAt() != null) ? a.getUpdatedAt()
                            : (a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN);
                    LocalDateTime d2 = (b.getUpdatedAt() != null) ? b.getUpdatedAt()
                            : (b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN);
                    return d2.compareTo(d1);
                })
                .collect(Collectors.toList());

        long totalElements = filtered.size();

        List<SummaryRequisition> pageItems;
        if (disablePagination) {
            pageItems = filtered;
        } else {
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            if (start > end) start = end;
            pageItems = filtered.subList(start, end);
        }

        List<ComparisonRequisitionDTO> dtoList = pageItems.stream()
                .map(req -> convertToDtos_NoCache(req, groupCurrency, finalRemoveDup))
                .collect(Collectors.toList());

        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalAmtDifference = BigDecimal.ZERO;
        BigDecimal totalDifferencePercentage = BigDecimal.ZERO;

        for (ComparisonRequisitionDTO dto : dtoList) {
            if (dto.getAmtVnd() != null) totalAmt = totalAmt.add(dto.getAmtVnd());
            if (dto.getAmtDifference() != null) totalAmtDifference = totalAmtDifference.add(dto.getAmtDifference());
            if (dto.getPercentage() != null) totalDifferencePercentage = totalDifferencePercentage.add(dto.getPercentage());
        }

        Page<ComparisonRequisitionDTO> page = disablePagination
                ? new PageImpl<>(dtoList, PageRequest.of(0, Math.max(dtoList.size(), 1)), totalElements)
                : new PageImpl<>(dtoList, pageable, totalElements);

        return ResponseEntity.ok(new ComparisonRequisitionResponseDTO(
                page,
                totalAmt,
                totalAmtDifference,
                totalDifferencePercentage
        ));
    }

// =========================================================
// MAPPING
// =========================================================

    private SummaryRequisition mapToSummaryRequisition(RequisitionMonthly monthly) {
        SummaryRequisition summary = new SummaryRequisition();
        summary.setId(monthly.getId());
        summary.setGroupId(monthly.getGroupId());
        summary.setEnglishName(monthly.getItemDescriptionEN());
        summary.setVietnameseName(monthly.getItemDescriptionVN());
        summary.setOldSapCode(monthly.getOldSAPCode());
        summary.setHanaSapCode(monthly.getHanaSAPCode());
        summary.setProductType1Id(monthly.getProductType1Id());
        summary.setProductType2Id(monthly.getProductType2Id());
        summary.setSupplierId(monthly.getSupplierId());
        summary.setOrderQty(monthly.getOrderQty());
        summary.setCreatedAt(monthly.getCreatedDate());
        summary.setUpdatedAt(monthly.getUpdatedDate());
        summary.setRemark(monthly.getRemark());
        summary.setRemarkComparison(monthly.getRemarkComparison());
        summary.setReason(monthly.getReason());
        summary.setImageUrls(monthly.getImageUrls());
        summary.setFullDescription(monthly.getFullDescription());
        summary.setStock(monthly.getStock());
        summary.setUnit(monthly.getUnit());
        summary.setStatusBestPrice(monthly.getStatusBestPrice());

        Map<String, DepartmentQty> deptMap = new HashMap<>();
        if (monthly.getDepartmentRequisitions() != null) {
            for (DepartmentRequisitionMonthly d : monthly.getDepartmentRequisitions()) {
                DepartmentQty dq = new DepartmentQty();
                dq.setQty(d.getQty() != null ? d.getQty() : BigDecimal.ZERO);
                dq.setBuy(d.getBuy() != null ? d.getBuy() : BigDecimal.ZERO);
                deptMap.put(d.getId(), dq);
            }
        }
        summary.setDepartmentRequestQty(deptMap);
        return summary;
    }

// =========================================================
// DTO BUILD (NO CACHE) ✅ Suppliers giống comparison-monthly
// =========================================================

    private ComparisonRequisitionDTO convertToDtos_NoCache(
            SummaryRequisition req,
            String groupCurrency,
            boolean removeDuplicateSuppliers
    ) {
        List<ComparisonRequisitionDTO.SupplierDTO> suppliers = Collections.emptyList();

        String selectedSupplierId = normText(req.getSupplierId());
        String unit = req.getUnit() != null ? req.getUnit() : "";
        String goodType = null;

        BigDecimal selectedPrice = null;
        BigDecimal highestPrice = null;
        BigDecimal lowestPrice = null;
        boolean isBestPrice = false;

        final String cur = (groupCurrency != null ? groupCurrency.trim() : "");

        // ✅ NEW: SEARCH SUPPLIER FOLLOW 4 RULES (SAP -> HANA -> VN -> EN) + unit + currency
        if (selectedSupplierId != null && !cur.isBlank()) {

            // ✅ unit is REQUIRED for search (same as auto-supplier)
            String reqUnit = normText(unit);
            if (reqUnit != null) {

                // fields
                String sapCode  = normCode(req.getOldSapCode());   // null if empty/new
                String hanaCode = normCode(req.getHanaSapCode());  // null if empty/new
                String desVn    = normText(req.getVietnameseName());
                String desEn    = normText(req.getEnglishName());

                // pick keyword
                String keyword = null;
                int searchMode = 0; // 1=sap, 2=hana, 3=vn, 4=en

                if (sapCode != null) {
                    keyword = sapCode;
                    searchMode = 1;
                } else if (hanaCode != null) {
                    keyword = hanaCode;
                    searchMode = 2;
                } else if (desVn != null) {
                    keyword = desVn;
                    searchMode = 3;
                } else if (desEn != null) {
                    keyword = desEn;
                    searchMode = 4;
                }

                if (keyword != null) {
                    List<SupplierProduct> supplierProducts;

                    // ✅ EXACT SEARCH FUNCTIONS (4 cases)
                    if (searchMode == 1) {
                        supplierProducts = supplierProductRepository
                                .findBySapCodeIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(keyword, reqUnit, cur);
                    } else if (searchMode == 2) {
                        supplierProducts = supplierProductRepository
                                .findByHanaSapCodeIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(keyword, reqUnit, cur);
                    } else if (searchMode == 3) {
                        supplierProducts = supplierProductRepository
                                .findByItemDescriptionVNContainingIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(keyword, reqUnit, cur);
                    } else {
                        supplierProducts = supplierProductRepository
                                .findByItemDescriptionENContainingIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(keyword, reqUnit, cur);
                    }

                    if (supplierProducts != null && !supplierProducts.isEmpty()) {

                        List<ComparisonRequisitionDTO.SupplierDTO> allSuppliers = supplierProducts.stream()
                                .map(sp -> new ComparisonRequisitionDTO.SupplierDTO(
                                        sp.getPrice(),
                                        sp.getSupplierName(),
                                        selectedSupplierId.equals(sp.getId()) ? 1 : 0, // ✅ mark selected
                                        sp.getUnit()
                                ))
                                .collect(Collectors.toList());

                        // ✅ remove duplicate supplier logic giữ nguyên
                        if (removeDuplicateSuppliers) {
                            Map<String, ComparisonRequisitionDTO.SupplierDTO> unique = new LinkedHashMap<>();
                            for (ComparisonRequisitionDTO.SupplierDTO s : allSuppliers) {
                                String key = s.getSupplierName();
                                ComparisonRequisitionDTO.SupplierDTO exist = unique.get(key);

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
                            suppliers = new ArrayList<>(unique.values());
                        } else {
                            suppliers = allSuppliers;
                        }

                        // sort price asc
                        suppliers = suppliers.stream()
                                .sorted(Comparator.comparing(
                                        ComparisonRequisitionDTO.SupplierDTO::getPrice,
                                        Comparator.nullsLast(BigDecimal::compareTo)))
                                .collect(Collectors.toList());

                        // ✅ find selected supplier in list
                        SupplierProduct selectedSupplier = supplierProducts.stream()
                                .filter(sp -> selectedSupplierId.equals(sp.getId()))
                                .findFirst()
                                .orElse(null);

                        if (selectedSupplier != null) {
                            unit = selectedSupplier.getUnit() != null ? selectedSupplier.getUnit() : unit;
                            goodType = selectedSupplier.getGoodType();
                            selectedPrice = selectedSupplier.getPrice();
                        }

                        highestPrice = suppliers.stream()
                                .map(ComparisonRequisitionDTO.SupplierDTO::getPrice)
                                .filter(Objects::nonNull)
                                .max(BigDecimal::compareTo)
                                .orElse(null);

                        lowestPrice = suppliers.stream()
                                .map(ComparisonRequisitionDTO.SupplierDTO::getPrice)
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(null);
                    }
                }
            }
        }

        // ✅ ======= THE REST: KEEP ORIGINAL LOGIC (no change) =======

        int requestQty = 0;
        if (req.getDepartmentRequestQty() != null) {
            for (Object value : req.getDepartmentRequestQty().values()) {
                if (value instanceof DepartmentQty dq) {
                    requestQty += dq.getBuy() != null ? dq.getBuy().intValue() : 0;
                } else if (value instanceof Number n) {
                    requestQty += n.intValue();
                }
            }
        }

        BigDecimal orderQty = (req.getOrderQty() != null && req.getOrderQty().compareTo(BigDecimal.ZERO) != 0)
                ? req.getOrderQty()
                : BigDecimal.ZERO;

        BigDecimal amt = (selectedPrice != null) ? selectedPrice.multiply(orderQty) : BigDecimal.ZERO;
        BigDecimal highestAmount = (highestPrice != null) ? highestPrice.multiply(orderQty) : BigDecimal.ZERO;
        BigDecimal amtDifference = amt.subtract(highestAmount);

        BigDecimal percentage = BigDecimal.ZERO;
        if (amt.compareTo(BigDecimal.ZERO) != 0) {
            percentage = amtDifference.divide(amt, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }

        String statusBestPrice = req.getStatusBestPrice();
        if (statusBestPrice == null || statusBestPrice.trim().isEmpty() || "EMPTY".equalsIgnoreCase(statusBestPrice.trim())) {
            isBestPrice = selectedPrice != null && lowestPrice != null && selectedPrice.compareTo(lowestPrice) == 0;
        } else {
            isBestPrice = "Yes".equalsIgnoreCase(statusBestPrice.trim());
        }

        List<ComparisonRequisitionDTO.DepartmentRequestDTO> departmentRequests =
                (req.getDepartmentRequestQty() != null)
                        ? req.getDepartmentRequestQty().entrySet().stream()
                        .filter(e -> e.getKey() != null && !e.getKey().isBlank())
                        .map(e -> {
                            String deptId = e.getKey();
                            Object val = e.getValue();

                            int qty = 0, buy = 0;
                            if (val instanceof DepartmentQty dq) {
                                qty = dq.getQty() != null ? dq.getQty().intValue() : 0;
                                buy = dq.getBuy() != null ? dq.getBuy().intValue() : 0;
                            } else if (val instanceof Number n) {
                                qty = buy = n.intValue();
                            }

                            String deptName = getDeptName(deptId);
                            return new ComparisonRequisitionDTO.DepartmentRequestDTO(deptId, deptName, qty, buy);
                        })
                        .collect(Collectors.toList())
                        : Collections.emptyList();

        String type1Name = getType1Name(req.getProductType1Id());
        String type2Name = getType2Name(req.getProductType2Id());

        ComparisonRequisitionDTO dto = new ComparisonRequisitionDTO(
                req.getId(),
                req.getEnglishName(),
                req.getVietnameseName(),
                req.getOldSapCode(),
                req.getHanaSapCode(),
                suppliers,
                req.getRemarkComparison(),
                departmentRequests,
                selectedPrice,
                amt,
                amtDifference,
                percentage,
                highestPrice,
                req.getProductType1Id(),
                req.getProductType2Id(),
                type1Name,
                type2Name,
                unit,
                requestQty,
                orderQty,
                cur,
                goodType,
                isBestPrice
        );

        applyLastPurchaseLatest_NoDateRange(
                dto,
                req.getId(),
                cur,
                req.getOldSapCode(),
                req.getHanaSapCode(),
                req.getVietnameseName(),
                req.getEnglishName()
        );

        return dto;
    }

// =========================================================
// LAST PURCHASE ✅ latest completedDate (NO start/end) + only 1 record
// Priority: oldSAP > hana > VN > EN
// =========================================================

    private void applyLastPurchaseLatest_NoDateRange(
            ComparisonRequisitionDTO dto,
            String currentReqId,
            String currency,
            String oldSapCode,
            String hanaSapCode,
            String desVN,
            String desEN
    ) {
        if (dto == null) return;

        String cur = normText(currency);
        if (cur == null) return;

        String idNot = normText(currentReqId);
        if (idNot == null) idNot = "__NONE__";

        String sap = normCode(oldSapCode);
        String hana = normCode(hanaSapCode);
        String vn = normText(desVN);
        String en = normText(desEN);

        Optional<RequisitionMonthly> opt = Optional.empty();

        Pageable top1 = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "completedDate"));

        // ✅ ưu tiên: oldSapCode > hanaSapCode > itemVN > itemEN
        if (sap != null) {
            List<RequisitionMonthly> list = requisitionMonthlyRepository
                    .findLatestPurchaseByOldSapCodeAndCurrency(sap, cur, idNot, top1);
            opt = list.stream().findFirst();
        } else if (hana != null) {
            List<RequisitionMonthly> list = requisitionMonthlyRepository
                    .findLatestPurchaseByHanaSapCodeAndCurrency(hana, cur, idNot, top1);
            opt = list.stream().findFirst();
        } else if (vn != null) {
            List<RequisitionMonthly> list = requisitionMonthlyRepository
                    .findLatestPurchaseByItemDescriptionVNAndCurrency(vn, cur, idNot, top1);
            opt = list.stream().findFirst();
        } else if (en != null) {
            List<RequisitionMonthly> list = requisitionMonthlyRepository
                    .findLatestPurchaseByItemDescriptionENAndCurrency(en, cur, idNot, top1);
            opt = list.stream().findFirst();
        } else {
            return;
        }

        if (opt.isEmpty()) return;

        RequisitionMonthly latest = opt.get();

        // ✅ bắt buộc completedDate mới set
        if (latest.getCompletedDate() == null) return;

        dto.setLastPurchaseSupplierName(latest.getSupplierName());
        dto.setLastPurchaseDate(latest.getCompletedDate());
        dto.setLastPurchasePrice(latest.getPrice());
        dto.setLastPurchaseOrderQty(latest.getOrderQty() != null ? latest.getOrderQty() : BigDecimal.ZERO);
    }

// =========================================================
// HELPERS
// =========================================================

    private static String normText(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normCode(String s) {
        String t = normText(s);
        if (t == null) return null;
        return "new".equalsIgnoreCase(t) ? null : t;
    }

    private static String pickCodeKey_SapFirst(String oldSap, String hana) {
        String sap = normCode(oldSap);
        if (sap != null) return sap.trim();
        String h = normCode(hana);
        return (h != null) ? h.trim() : null;
    }

    private String getType1Name(String type1Id) {
        if (type1Id == null || type1Id.isBlank()) return "";
        ProductType1 t1 = productType1Service.getById(type1Id.trim());
        return (t1 != null && t1.getName() != null) ? t1.getName() : "";
    }

    private String getType2Name(String type2Id) {
        if (type2Id == null || type2Id.isBlank()) return "";
        ProductType2 t2 = productType2Service.getById(type2Id.trim());
        return (t2 != null && t2.getName() != null) ? t2.getName() : "";
    }

    private String getDeptName(String deptId) {
        if (deptId == null || deptId.isBlank()) return "Unknown";
        return departmentRepository.findById(deptId.trim())
                .map(Department::getDepartmentName)
                .orElse("Unknown");
    }

    @PatchMapping("/mark-completed")
    @Operation(
            summary = "Mark multiple requisitions as completed",
            description = "Updates isCompleted=true, completedDate=now, updatedDate=now + set completedByEmail/updatedByEmail"
    )
    public ResponseEntity<?> markAsCompleted(
            @RequestBody MarkCompletedRequest request,
            @RequestParam("email") String email
    ) {
        try {
            if (request.getRequisitionIds() == null || request.getRequisitionIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "requisitionIds list is required and cannot be empty"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "email is required and cannot be empty"));
            }

            String emailNorm = email.trim().toLowerCase();
            LocalDateTime now = LocalDateTime.now();

            List<RequisitionMonthly> requisitions = requisitionMonthlyRepository.findAllById(request.getRequisitionIds());

            if (requisitions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No requisitions found with provided IDs",
                        "updatedCount", 0
                ));
            }

            requisitions.forEach(req -> {
                req.setIsCompleted(true);
                req.setCompletedDate(now);
                req.setUpdatedDate(now);

                // ✅ audit email-only
                req.setCompletedByEmail(emailNorm);
                req.setUpdatedByEmail(emailNorm);

                // optional: clear uncomplete audit cho sạch
                req.setUncompletedByEmail(null);
            });

            List<RequisitionMonthly> saved = requisitionMonthlyRepository.saveAll(requisitions);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully marked as completed",
                    "updatedCount", saved.size(),
                    "updatedIds", saved.stream().map(RequisitionMonthly::getId).toList(),
                    "completedByEmail", emailNorm
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update requisitions: " + e.getMessage()));
        }
    }

    @PatchMapping("/mark-uncompleted")
    @Operation(
            summary = "Remove completed status from multiple requisitions",
            description = "Updates isCompleted=false, completedDate=null, updatedDate=now + set uncompletedByEmail/updatedByEmail (and clear completedByEmail)"
    )
    public ResponseEntity<?> markAsUncompleted(
            @RequestBody MarkCompletedRequest request,
            @RequestParam("email") String email
    ) {
        try {
            if (request.getRequisitionIds() == null || request.getRequisitionIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "requisitionIds list is required"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "email is required and cannot be empty"));
            }

            String emailNorm = email.trim().toLowerCase();
            LocalDateTime now = LocalDateTime.now();

            List<RequisitionMonthly> requisitions = requisitionMonthlyRepository.findAllById(request.getRequisitionIds());

            if (requisitions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No requisitions found with provided IDs",
                        "updatedCount", 0
                ));
            }

            requisitions.forEach(req -> {
                req.setIsCompleted(false);
                req.setCompletedDate(null);
                req.setUpdatedDate(now);

                // ✅ audit email-only
                req.setUncompletedByEmail(emailNorm);
                req.setUpdatedByEmail(emailNorm);

                // ✅ clear completed email trước đó
                req.setCompletedByEmail(null);
            });

            List<RequisitionMonthly> saved = requisitionMonthlyRepository.saveAll(requisitions);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully unmarked as completed",
                    "updatedCount", saved.size(),
                    "uncompletedByEmail", emailNorm
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update: " + e.getMessage()));
        }
    }

}