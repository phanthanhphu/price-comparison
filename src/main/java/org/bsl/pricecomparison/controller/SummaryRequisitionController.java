package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.bsl.pricecomparison.dto.*;
import org.bsl.pricecomparison.enums.RequisitionType;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.*;
import org.bsl.pricecomparison.request.*;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
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
    public ResponseEntity<?> create(@ModelAttribute UpdateSummaryRequisitionRequest request) {
        try {
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

            if (requisitionMonthlyRepository.findByGroupIdAndOldSAPCode(request.getGroupId(), request.getOldSapCode().trim()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Duplicate entry: groupId and oldSapCode already exist in monthly."));
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
    @Operation(summary = "Update requisition", description = "Uses same logic as create: orderQty = sum(buy), amount = orderQty * price.")
    public ResponseEntity<?> update(@PathVariable String id, @ModelAttribute UpdateSummaryRequisitionRequest request) {
        try {
            Optional<RequisitionMonthly> opt = requisitionMonthlyRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Requisition not found."));
            }

            RequisitionMonthly current = opt.get();

            // === CHECK DUPLICATE (exclude self) ===
            if (request.getGroupId() != null && request.getOldSapCode() != null) {
                Optional<RequisitionMonthly> dup = requisitionMonthlyRepository
                        .findByGroupIdAndOldSAPCode(request.getGroupId(), request.getOldSapCode());
                if (dup.isPresent() && !dup.get().getId().equals(id)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "Duplicate groupId and oldSapCode."));
                }
            }

            // === IMAGE HANDLING ===
            List<String> imageUrls = current.getImageUrls() != null ? new ArrayList<>(current.getImageUrls()) : new ArrayList<>();
            List<MultipartFile> files = request.getFiles() != null ? request.getFiles() : new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    if (!file.getContentType().startsWith("image/")) {
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
                    List<String> toDelete = objectMapper.readValue(request.getImagesToDelete(), new TypeReference<List<String>>() {});
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

            // === DEPARTMENT REQUISITIONS (nếu không gửi → giữ nguyên cũ) ===
            List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptList = new ArrayList<>();
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

            // === TẠO TEMP SUMMARY (chỉ cập nhật field có gửi) ===
            SummaryRequisition tempSummary = new SummaryRequisition();
            tempSummary.setGroupId(request.getGroupId() != null ? request.getGroupId() : current.getGroupId());
            tempSummary.setEnglishName(request.getEnglishName() != null ? request.getEnglishName() : current.getItemDescriptionEN());
            tempSummary.setVietnameseName(request.getVietnameseName() != null ? request.getVietnameseName() : current.getItemDescriptionVN());
            tempSummary.setOldSapCode(request.getOldSapCode() != null ? request.getOldSapCode() : current.getOldSAPCode());
            tempSummary.setHanaSapCode(request.getHanaSapCode() != null ? request.getHanaSapCode() : current.getHanaSAPCode());
            tempSummary.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : current.getFullDescription());
            tempSummary.setReason(request.getReason() != null ? request.getReason() : current.getReason());
            tempSummary.setRemark(request.getRemark() != null ? request.getRemark() : current.getRemark());
            tempSummary.setRemarkComparison(request.getRemarkComparison() != null ? request.getRemarkComparison() : current.getRemarkComparison());
            tempSummary.setProductType1Id(request.getProductType1Id() != null ? request.getProductType1Id() : current.getProductType1Id());
            tempSummary.setProductType2Id(request.getProductType2Id() != null ? request.getProductType2Id() : current.getProductType2Id());
            tempSummary.setStock(request.getStock() != null && request.getStock().compareTo(BigDecimal.ZERO) != 0 ? request.getStock() : current.getStock());
            tempSummary.setType(RequisitionType.WEEKLY);

            // === DÙNG CHÍNH XÁC buildRequisitionMonthly (GIỮ NGUYÊN CÔNG THỨC GỐC) ===
            RequisitionMonthly updated = buildRequisitionMonthly(tempSummary, request, deptList, imageUrls);

            // === GHI ĐÈ ID + CREATED DATE ===
            updated.setId(current.getId());
            updated.setCreatedDate(current.getCreatedDate());
            updated.setUpdatedDate(LocalDateTime.now());

            // === STOCK (nếu có) → chỉ lưu, KHÔNG ảnh hưởng orderQty ===
            if (request.getStock() != null) {
                updated.setDailyMedInventory(request.getStock());
            } else {
                updated.setDailyMedInventory(current.getDailyMedInventory());
            }

            // === SAVE ===
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing request."));
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

        // === LỌC & SẮP XẾP TRÊN MEMORY (giống filter monthly) ===
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

//    @PostMapping(value = "/upload-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(
//            summary = "Upload Requisition File",
//            description = "Upload an Excel .xlsx file with data starting from row 4. Columns: No (A), Item (B) [VN/EN with /], SAP code (C), Last times (D) [Price], Sup (E), Q'ty Request (F), Inhand (G), Buy (H), U/Price (I), Sup (J), Amount (K), Dept request (L), Reason (M), Picture (N)"
//    )
//    public ResponseEntity<List<RequisitionMonthly>> uploadRequisitionFile(
//            @Parameter(description = "Excel .xlsx file containing requisition data")
//            @RequestPart("file") MultipartFile file,
//            @Parameter(description = "Group ID", required = true)
//            @RequestParam(value = "groupId", required = true) String groupId) {
//
//        List<RequisitionMonthly> requisitions = new ArrayList<>();
//        Map<String, Integer> sapCodeMap = new HashMap<>(); // <sapCode, index>
//        Map<String, Integer> materialMap = new HashMap<>(); // <itemVN, index> cho SAP = NEW
//
//        if (file == null || file.isEmpty()) {
//            return ResponseEntity.badRequest().body(Collections.singletonList(new RequisitionMonthly() {{
//                setRemark("No file uploaded.");
//            }}));
//        }
//        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
//            return ResponseEntity.badRequest().body(Collections.singletonList(new RequisitionMonthly() {{
//                setRemark("Only .xlsx files are allowed.");
//            }}));
//        }
//
//        try (InputStream is = file.getInputStream()) {
//            XSSFWorkbook workbook = new XSSFWorkbook(is);
//            Sheet sheet = workbook.getSheetAt(0);
//            if (sheet == null) {
//                return ResponseEntity.badRequest().body(Collections.singletonList(new RequisitionMonthly() {{
//                    setRemark("Sheet not found in the uploaded file.");
//                }}));
//            }
//
//            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
//            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
//
//            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
//                Row row = sheet.getRow(i);
//                if (row == null) continue;
//
//                Cell noCell = getMergedCellValue(sheet, i, 0, mergedRegions);
//                if (noCell == null || !isValidNo(noCell)) break;
//
//                Cell sapCell = getMergedCellValue(sheet, i, 2, mergedRegions);
//                String sapCode = sapCell != null ? getCellValue(sapCell) : null;
//
//                Cell descriptionCell = getMergedCellValue(sheet, i, 1, mergedRegions);
//                String description = descriptionCell != null ? getCellValue(descriptionCell) : null;
//                String vietnameseName = description != null && description.contains("/") ?
//                        description.split("/", 2)[0].trim() : description != null ? description.trim() : null;
//
//                Cell deptCell = getMergedCellValue(sheet, i, 11, mergedRegions);
//                String deptName = deptCell != null ? getCellValue(deptCell) : null;
//
//                String deptId = null;
//                if (deptName != null && !deptName.trim().isEmpty()) {
//                    Department dept = departmentRepository.findByDepartmentName(deptName.trim());
//                    if (dept != null) {
//                        deptId = dept.getId();
//                    } else {
//                        Department newDept = new Department();
//                        newDept.setDepartmentName(deptName.trim());
//                        departmentRepository.save(newDept);
//                        deptId = newDept.getId();
//                    }
//                }
//
//                Cell requestQtyCell = getMergedCellValue(sheet, i, 5, mergedRegions);
//                BigDecimal requestQty = parseBigDecimal(requestQtyCell); // BigDecimal
//                if (requestQty == null) requestQty = BigDecimal.ZERO;
//
//                Cell inhandCell = getMergedCellValue(sheet, i, 6, mergedRegions);
//                BigDecimal inhand = parseBigDecimal(inhandCell);
//                if (inhand == null) inhand = BigDecimal.ZERO;
//
//                Cell buyCell = getMergedCellValue(sheet, i, 7, mergedRegions);
//                BigDecimal buy = parseBigDecimal(buyCell);
//                if (buy == null) buy = requestQty.subtract(inhand).max(BigDecimal.ZERO);
//
//                boolean isSapNewOrEmpty = (sapCode == null || sapCode.trim().isEmpty() || sapCode.equalsIgnoreCase("NEW"));
//                String groupKey = isSapNewOrEmpty ? vietnameseName.toLowerCase() : sapCode.trim().toLowerCase();
//                Map<String, Integer> currentMap = isSapNewOrEmpty ? materialMap : sapCodeMap;
//
//                if (currentMap.containsKey(groupKey)) {
//                    int existingIndex = currentMap.get(groupKey);
//                    RequisitionMonthly existingReq = requisitions.get(existingIndex);
//
//                    boolean deptExists = false;
//                    for (DepartmentRequisitionMonthly deptReq : existingReq.getDepartmentRequisitions()) {
//                        if (deptReq.getId().equals(deptId)) {
//                            deptExists = true;
//                            deptReq.setQty(deptReq.getQty().add(requestQty));
//                            deptReq.setBuy(deptReq.getBuy().add(buy));
//                            break;
//                        }
//                    }
//
//                    if (!deptExists && deptId != null) {
//                        DepartmentRequisitionMonthly newDeptReq = new DepartmentRequisitionMonthly();
//                        newDeptReq.setId(deptId);
//                        newDeptReq.setName(deptName);
//                        newDeptReq.setQty(requestQty);
//                        newDeptReq.setBuy(buy);
//                        existingReq.getDepartmentRequisitions().add(newDeptReq);
//                    }
//
//                    existingReq.setTotalRequestQty(existingReq.getTotalRequestQty().add(requestQty));
//                    existingReq.setOrderQty(existingReq.getOrderQty().add(buy));
//                    continue;
//                }
//
//                RequisitionMonthly req = new RequisitionMonthly();
//                req.setGroupId(groupId);
//                req.setCreatedDate(LocalDateTime.now());
//                req.setUpdatedDate(LocalDateTime.now());
//                if (!isSapNewOrEmpty) req.setOldSAPCode(sapCode);
//                req.setItemDescriptionVN(vietnameseName);
//                req.setTotalRequestQty(requestQty);
//                req.setOrderQty(buy);
//                req.setDailyMedInventory(inhand);
//
//                List<DepartmentRequisitionMonthly> deptList = new ArrayList<>();
//                if (deptId != null) {
//                    DepartmentRequisitionMonthly deptReq = new DepartmentRequisitionMonthly();
//                    deptReq.setId(deptId);
//                    deptReq.setName(deptName);
//                    deptReq.setQty(requestQty);
//                    deptReq.setBuy(buy);
//                    deptList.add(deptReq);
//                }
//                req.setDepartmentRequisitions(deptList);
//
//                List<String> imageUrls = new ArrayList<>();
//                for (XSSFShape shape : drawing.getShapes()) {
//                    if (shape instanceof XSSFPicture) {
//                        XSSFPicture pic = (XSSFPicture) shape;
//                        XSSFClientAnchor anchor = pic.getClientAnchor();
//                        if (anchor.getCol1() == 13 && anchor.getRow1() <= i && anchor.getRow2() >= i) {
//                            byte[] imgBytes = pic.getPictureData().getData();
//                            String imgPath = saveImage(imgBytes, "image_" + i + "_" + System.currentTimeMillis() + ".png");
//                            if (imgPath != null) imageUrls.add(imgPath);
//                        }
//                    }
//                }
//                req.setImageUrls(imageUrls.isEmpty() ? null : imageUrls);
//
//                requisitions.add(req);
//                currentMap.put(groupKey, requisitions.size() - 1);
//            }
//
//            if (requisitions.isEmpty()) {
//                return ResponseEntity.badRequest().body(Collections.singletonList(new RequisitionMonthly() {{
//                    setRemark("No valid data found in the file starting from row 4.");
//                }}));
//            }
//
//            List<RequisitionMonthly> saved = requisitionMonthlyRepository.saveAll(requisitions);
//            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonList(new RequisitionMonthly() {{
//                setRemark("Error processing file: " + e.getMessage());
//            }}));
//        }
//    }

    @PostMapping(value = "/upload-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload Requisition File",
            description = "Upload Excel .xlsx file. Data starts from **row 6**, column B (Item VN). " +
                    "Columns: B=Item VN, C=Item EN, D=Hana SAP, G=Request, H=Inhand, I=Buy, J=Unit, M=Dept, N=Reason, O=Picture"
    )
    public ResponseEntity<List<RequisitionMonthly>> uploadRequisitionFile(
            @Parameter(description = "Excel .xlsx file") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Group ID", required = true) @RequestParam("groupId") String groupId) {

        // === LẤY DỮ LIỆU ĐÃ TỒN TẠI TRONG DB ===
        List<RequisitionMonthly> allMonthly = requisitionMonthlyRepository.findByGroupId(groupId);
        Set<String> existingSapCodes = allMonthly.stream()
                .map(RequisitionMonthly::getOldSAPCode)
                .filter(code -> code != null && !code.trim().isEmpty() && !"NEW".equalsIgnoreCase(code.trim()))
                .map(code -> code.trim().toUpperCase())
                .collect(Collectors.toSet());

        List<RequisitionMonthly> requisitions = new ArrayList<>();
        Map<String, Integer> sapCodeMap = new HashMap<>();     // <sapCode, index>
        Map<String, Integer> materialMap = new HashMap<>();    // <itemVN, index> for SAP = NEW

        // === VALIDATE FILE ===
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

            // === BẮT ĐẦU TỪ DÒNG 6 (index = 5) ===
            for (int i = 5; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // === CỘT B (Item VN) ===
                Cell itemVNCell = getMergedCellValue(sheet, i, 1, mergedRegions);
                if (itemVNCell == null) {
                    itemVNCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                }
                String itemVN = getCellValue(itemVNCell);

                if (itemVN == null || itemVN.trim().isEmpty()) {
                    continue;
                }

                // === ĐỌC CÁC CỘT KHÁC ===
                Cell sapCell = getMergedCellValue(sheet, i, 3, mergedRegions); // D
                String sapCode = getCellValue(sapCell);

                // === KIỂM TRA TRÙNG SAP CODE TRONG DB (chỉ với SAP hợp lệ) ===
                boolean isSapValid = sapCode != null && !sapCode.trim().isEmpty() && !"NEW".equalsIgnoreCase(sapCode.trim());
                if (isSapValid) {
                    String normalizedSapCode = sapCode.trim().toUpperCase();
                    if (existingSapCodes.contains(normalizedSapCode)) {
                        return badRequest("SAP Code '" + sapCode + "' already exists in the system for this group.");
                    }
                }

                Cell itemENCell   = getMergedCellValue(sheet, i, 2, mergedRegions);
                Cell requestCell  = getMergedCellValue(sheet, i, 6, mergedRegions);
                Cell inhandCell   = getMergedCellValue(sheet, i, 7, mergedRegions);
                Cell buyCell      = getMergedCellValue(sheet, i, 8, mergedRegions);
                Cell unitCell     = getMergedCellValue(sheet, i, 9, mergedRegions);
                Cell deptCell     = getMergedCellValue(sheet, i, 12, mergedRegions);
                Cell reasonCell   = getMergedCellValue(sheet, i, 13, mergedRegions);

                String itemEN   = getCellValue(itemENCell);

                BigDecimal requestQty = parseBigDecimal(requestCell);
                if (requestQty == null) requestQty = BigDecimal.ZERO;

                BigDecimal inhand = parseBigDecimal(inhandCell);
                if (inhand == null) inhand = BigDecimal.ZERO;

                BigDecimal buy = parseBigDecimal(buyCell);
                if (buy == null || buy.compareTo(BigDecimal.ZERO) < 0) {
                    buy = requestQty.subtract(inhand).max(BigDecimal.ZERO);
                }

                String unit     = getCellValue(unitCell);
                String deptName = getCellValue(deptCell);
                String reason   = getCellValue(reasonCell);

                // === XỬ LÝ HÌNH ẢNH (CỘT N - index 13) - ĐÃ SỬA CHÍNH XÁC ===
//                List<String> imageUrls = new ArrayList<>();
//                for (XSSFShape shape : drawing.getShapes()) {
//                    if (shape instanceof XSSFPicture) {
//                        XSSFPicture pic = (XSSFPicture) shape;
//                        XSSFClientAnchor anchor = pic.getClientAnchor();
//                        if (anchor.getCol1() == 14 && anchor.getRow1() <= i && anchor.getRow2() >= i) {
//                            byte[] imgBytes = pic.getPictureData().getData();
//                            String imgPath = saveImage(imgBytes, "img_" + i + "_" + System.currentTimeMillis() + ".png");
//                            if (imgPath != null) {
//                                imageUrls.add(imgPath);
//                            }
//                        }
//                    }
//                }

                List<String> imageUrls = new ArrayList<>();

                for (XSSFShape shape : drawing.getShapes()) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture pic = (XSSFPicture) shape;
                        XSSFClientAnchor anchor = pic.getClientAnchor();

                        int col1 = anchor.getCol1();
                        int col2 = anchor.getCol2();
                        int row1 = anchor.getRow1();
                        int row2 = anchor.getRow2();

                        // Cho phép ảnh nằm ở cột O hoặc lệch sang P (Excel hay bị)
                        boolean colMatches = (col1 == 14 || col1 == 15 || (col1 <= 14 && col2 >= 14));
                        boolean rowMatches = (row1 <= i && i <= row2);

                        if (colMatches && rowMatches) {
                            byte[] imgBytes = pic.getPictureData().getData();
                            String imgPath = saveImage(imgBytes, "img_" + i + "_" + System.currentTimeMillis() + ".png");
                            if (imgPath != null) {
                                imageUrls.add(imgPath);
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

                // === GỘP DỮ LIỆU TRONG FILE (giữ nguyên logic cũ) ===
                if (currentMap.containsKey(groupKey)) {
                    int idx = currentMap.get(groupKey);
                    RequisitionMonthly existing = requisitions.get(idx);

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
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
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
            description = "Retrieve a paginated or full list of summary requisitions from RequisitionMonthly for a given group ID with optional filters for product types, names, Old SAP code, Hana SAP code, supplier name, and department name, sorted by updatedDate or createdDate in descending order. Includes sumBuy (total buy quantity across departments) and financial totals for each requisition. Optionally removes duplicate suppliers based on supplierName, keeping the one with the lowest price."
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
            Pageable pageable) {

        // Fetch GroupSummaryRequisition for currency
        Optional<GroupSummaryRequisition> group = groupSummaryRequisitionService.getGroupSummaryRequisitionById(groupId);
        String currency = group.isPresent() && group.get().getCurrency() != null ? group.get().getCurrency() : "";

        List<ComparisonRequisitionDTO> dtoList;
        long totalElements;

        if (disablePagination) {
            // No pagination: fetch all records and apply filters
            List<RequisitionMonthly> allMonthly = requisitionMonthlyRepository.findByGroupId(groupId);
            totalElements = allMonthly.size();

            dtoList = allMonthly.stream()
                    .map(this::mapToSummaryRequisition)
                    .filter(req -> {
                        boolean matches = true;

                        String reqProductType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                                ? (productType1Service.getById(req.getProductType1Id()) != null
                                ? productType1Service.getById(req.getProductType1Id()).getName() : "")
                                : "";
                        String reqProductType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                                ? (productType2Service.getById(req.getProductType2Id()) != null
                                ? productType2Service.getById(req.getProductType2Id()).getName() : "")
                                : "";

                        List<String> deptNames = req.getDepartmentRequestQty() != null ?
                                req.getDepartmentRequestQty().keySet().stream()
                                        .filter(deptId -> deptId != null && !deptId.isEmpty())
                                        .map(deptId -> departmentRepository.findById(deptId)
                                                .map(Department::getDepartmentName)
                                                .orElse("Unknown"))
                                        .collect(Collectors.toList()) :
                                Collections.emptyList();

                        if (hasFilter) {
                            if (productType1Name != null && !productType1Name.isEmpty()) {
                                matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                            }
                            if (productType2Name != null && !productType2Name.isEmpty()) {
                                matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                            }
                            if (englishName != null && !englishName.isEmpty()) {
                                matches = matches && req.getVietnameseName() != null && req.getEnglishName().toLowerCase().contains(englishName.toLowerCase());
                            }
                            if (vietnameseName != null && !vietnameseName.isEmpty()) {
                                matches = matches && req.getVietnameseName() != null && req.getVietnameseName().toLowerCase().contains(vietnameseName.toLowerCase());
                            }
                            if (oldSapCode != null && !oldSapCode.isEmpty()) {
                                matches = matches && req.getOldSapCode() != null && req.getOldSapCode().toLowerCase().contains(oldSapCode.toLowerCase());
                            }
                            if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                                matches = matches && req.getHanaSapCode() != null && req.getHanaSapCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                            }
                            if (supplierName != null && !supplierName.isEmpty()) {
                                SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;
                                String reqSupplierName = supplierProduct != null ? supplierProduct.getSupplierName() : "";
                                matches = matches && reqSupplierName.toLowerCase().contains(supplierName.toLowerCase());
                            }
                            if (departmentName != null && !departmentName.isEmpty()) {
                                matches = matches && deptNames.stream().anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                            }
                        }

                        return matches;
                    })
                    .sorted((req1, req2) -> {
                        LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                        return date2.compareTo(date1);
                    })
                    .map(req -> convertToDtos(req, currency, removeDuplicateSuppliers))
                    .collect(Collectors.toList());

            BigDecimal totalAmtVnd = BigDecimal.ZERO;
            BigDecimal totalAmtDifference = BigDecimal.ZERO;
            BigDecimal totalDifferencePercentage = BigDecimal.ZERO;
            for (ComparisonRequisitionDTO dto : dtoList) {
                if (dto.getAmtVnd() != null) {
                    totalAmtVnd = totalAmtVnd.add(dto.getAmtVnd());
                }
                if (dto.getAmtDifference() != null) {
                    totalAmtDifference = totalAmtDifference.add(dto.getAmtDifference());
                }
                if (dto.getPercentage() != null) {
                    totalDifferencePercentage = totalDifferencePercentage.add(dto.getPercentage());
                }
            }

            Page<ComparisonRequisitionDTO> page = new PageImpl<>(dtoList, PageRequest.of(0, Math.max(dtoList.size(), 1)), totalElements);

            ComparisonRequisitionResponseDTO response = new ComparisonRequisitionResponseDTO(
                    page,
                    totalAmtVnd,
                    totalAmtDifference,
                    totalDifferencePercentage
            );

            return ResponseEntity.ok(response);
        } else {
            // Fetch all records and paginate in-memory
            List<RequisitionMonthly> allMonthly = requisitionMonthlyRepository.findByGroupId(groupId);
            totalElements = allMonthly.size();

            List<SummaryRequisition> filteredRequisitions = allMonthly.stream()
                    .map(this::mapToSummaryRequisition)
                    .filter(req -> {
                        boolean matches = true;

                        String reqProductType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                                ? (productType1Service.getById(req.getProductType1Id()) != null
                                ? productType1Service.getById(req.getProductType1Id()).getName() : "")
                                : "";
                        String reqProductType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                                ? (productType2Service.getById(req.getProductType2Id()) != null
                                ? productType2Service.getById(req.getProductType2Id()).getName() : "")
                                : "";

                        List<String> deptNames = req.getDepartmentRequestQty() != null ?
                                req.getDepartmentRequestQty().keySet().stream()
                                        .filter(deptId -> deptId != null && !deptId.isEmpty())
                                        .map(deptId -> departmentRepository.findById(deptId)
                                                .map(Department::getDepartmentName)
                                                .orElse("Unknown"))
                                        .collect(Collectors.toList()) :
                                Collections.emptyList();

                        if (hasFilter) {
                            if (productType1Name != null && !productType1Name.isEmpty()) {
                                matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                            }
                            if (productType2Name != null && !productType2Name.isEmpty()) {
                                matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                            }
                            if (englishName != null && !englishName.isEmpty()) {
                                matches = matches && req.getEnglishName() != null && req.getEnglishName().toLowerCase().contains(englishName.toLowerCase());
                            }
                            if (vietnameseName != null && !vietnameseName.isEmpty()) {
                                matches = matches && req.getVietnameseName() != null && req.getVietnameseName().toLowerCase().contains(vietnameseName.toLowerCase());
                            }
                            if (oldSapCode != null && !oldSapCode.isEmpty()) {
                                matches = matches && req.getOldSapCode() != null && req.getOldSapCode().toLowerCase().contains(oldSapCode.toLowerCase());
                            }
                            if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                                matches = matches && req.getHanaSapCode() != null && req.getHanaSapCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                            }
                            if (supplierName != null && !supplierName.isEmpty()) {
                                SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;
                                String reqSupplierName = supplierProduct != null ? supplierProduct.getSupplierName() : "";
                                matches = matches && reqSupplierName.toLowerCase().contains(supplierName.toLowerCase());
                            }
                            if (departmentName != null && !departmentName.isEmpty()) {
                                matches = matches && deptNames.stream().anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                            }
                        }

                        return matches;
                    })
                    .sorted((req1, req2) -> {
                        LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                        return date2.compareTo(date1);
                    })
                    .collect(Collectors.toList());

            // Apply in-memory pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filteredRequisitions.size());
            List<SummaryRequisition> pagedRequisitions = filteredRequisitions.subList(start, end);

            dtoList = pagedRequisitions.stream()
                    .map(req -> convertToDtos(req, currency, removeDuplicateSuppliers))
                    .collect(Collectors.toList());

            BigDecimal totalAmt = BigDecimal.ZERO;
            BigDecimal totalAmtDifference = BigDecimal.ZERO;
            BigDecimal totalDifferencePercentage = BigDecimal.ZERO;
            for (ComparisonRequisitionDTO dto : dtoList) {
                if (dto.getAmtVnd() != null) {
                    totalAmt = totalAmt.add(dto.getAmtVnd());
                }
                if (dto.getAmtDifference() != null) {
                    totalAmtDifference = totalAmtDifference.add(dto.getAmtDifference());
                }
                if (dto.getPercentage() != null) {
                    totalDifferencePercentage = totalDifferencePercentage.add(dto.getPercentage());
                }
            }

            Page<ComparisonRequisitionDTO> page = new PageImpl<>(dtoList, pageable, totalElements);

            ComparisonRequisitionResponseDTO response = new ComparisonRequisitionResponseDTO(
                    page,
                    totalAmt,
                    totalAmtDifference,
                    totalDifferencePercentage
            );

            return ResponseEntity.ok(response);
        }
    }

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
        summary.setStock(monthly.getStock()); // Use stock, not dailyMedInventory or safeStock
        summary.setUnit(monthly.getUnit());

        // Map departmentRequisitions to departmentRequestQty
        Map<String, DepartmentQty> deptMap = new HashMap<>();
        if (monthly.getDepartmentRequisitions() != null) {
            for (DepartmentRequisitionMonthly d : monthly.getDepartmentRequisitions()) {
                DepartmentQty deptQty = new DepartmentQty();
                deptQty.setQty(d.getQty() != null ? d.getQty() : BigDecimal.ZERO);
                deptQty.setBuy(d.getBuy() != null ? d.getBuy() : BigDecimal.ZERO);
                deptMap.put(d.getId(), deptQty);
            }
        }
        summary.setDepartmentRequestQty(deptMap);

        return summary;
    }

    private ComparisonRequisitionDTO convertToDtos(SummaryRequisition req, String currency, boolean removeDuplicateSuppliers) {
        List<ComparisonRequisitionDTO.SupplierDTO> suppliers;

        String sapCode = req.getOldSapCode() != null && !req.getOldSapCode().isEmpty() ? req.getOldSapCode() : null;
        String selectedSupplierId = req.getSupplierId();

        String unit = req.getUnit();
        String goodType = null;

        if (sapCode != null && sapCode.length() >= 3) {
            List<SupplierProduct> supplierProducts = (currency != null && !currency.isEmpty()) ?
                    supplierProductRepository.findBySapCodeAndCurrency(sapCode, currency) :
                    supplierProductRepository.findBySapCode(sapCode);

            suppliers = supplierProducts.stream()
                    .map(sp -> new ComparisonRequisitionDTO.SupplierDTO(
                            sp.getPrice(),
                            sp.getSupplierName(),
                            selectedSupplierId != null && !selectedSupplierId.isEmpty() && selectedSupplierId.equals(sp.getId()) ? 1 : 0,
                            sp.getUnit()))
                    .collect(Collectors.toList());

            // Remove duplicate suppliers based on supplierName, keeping the one with the lowest price
            if (removeDuplicateSuppliers) {
                Map<String, ComparisonRequisitionDTO.SupplierDTO> supplierMap = new LinkedHashMap<>();
                for (ComparisonRequisitionDTO.SupplierDTO supplier : suppliers) {
                    String supplierName = supplier.getSupplierName();
                    if (!supplierMap.containsKey(supplierName)) {
                        supplierMap.put(supplierName, supplier);
                    } else {
                        ComparisonRequisitionDTO.SupplierDTO existing = supplierMap.get(supplierName);
                        if (supplier.getPrice() != null && (existing.getPrice() == null || supplier.getPrice().compareTo(existing.getPrice()) < 0)) {
                            supplierMap.put(supplierName, supplier);
                        }
                    }
                }
                suppliers = new ArrayList<>(supplierMap.values());
            }

            // Sort suppliers by price (ascending, nulls last)
            suppliers = suppliers.stream()
                    .sorted(Comparator.comparing(ComparisonRequisitionDTO.SupplierDTO::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                    .collect(Collectors.toList());

            if (selectedSupplierId != null && !selectedSupplierId.isEmpty()) {
                SupplierProduct selectedSupplier = supplierProducts.stream()
                        .filter(sp -> sp.getId().equals(selectedSupplierId))
                        .findFirst()
                        .orElse(null);
                if (selectedSupplier != null) {
                    unit = selectedSupplier != null && selectedSupplier.getUnit() != null
                            ? selectedSupplier.getUnit()
                            : (req.getUnit() != null ? req.getUnit() : "");
                    currency = selectedSupplier.getCurrency() != null ? selectedSupplier.getCurrency() : currency;
                    goodType = selectedSupplier.getGoodType() != null ? selectedSupplier.getGoodType() : null;

                    // If duplicates were removed, ensure selected supplier is in the list
                    if (removeDuplicateSuppliers) {
                        boolean isSelectedInList = suppliers.stream()
                                .anyMatch(s -> s.getIsSelected() == 1);
                        if (!isSelectedInList && selectedSupplier != null) {
                            ComparisonRequisitionDTO.SupplierDTO selectedSupplierDTO = suppliers.stream()
                                    .filter(s -> s.getSupplierName().equals(selectedSupplier.getSupplierName()))
                                    .findFirst()
                                    .orElse(null);
                            if (selectedSupplierDTO != null) {
                                selectedSupplierDTO.setIsSelected(1);
                            }
                        }
                    }
                }
            }
        } else {
            suppliers = Collections.emptyList();
        }

        int requestQty = 0;
        if (req.getDepartmentRequestQty() != null) {
            for (Object value : req.getDepartmentRequestQty().values()) {
                if (value instanceof DepartmentQty deptQty) {
                    requestQty += deptQty.getQty() != null ? deptQty.getQty().intValue() : 0;
                } else if (value instanceof Double qty) {
                    requestQty += qty.intValue();
                }
            }
        }

        BigDecimal orderQty = req.getOrderQty() != null && req.getOrderQty().compareTo(BigDecimal.ZERO) != 0 ? req.getOrderQty() : BigDecimal.ZERO;

        BigDecimal selectedPrice = null;
        BigDecimal highestPrice = null;
        BigDecimal lowestPrice = null;
        boolean isBestPrice = false;

        if (!suppliers.isEmpty()) {
            selectedPrice = suppliers.stream()
                    .filter(dto -> dto.getIsSelected() == 1)
                    .map(ComparisonRequisitionDTO.SupplierDTO::getPrice)
                    .filter(price -> price != null)
                    .findFirst()
                    .orElse(null);

            highestPrice = suppliers.stream()
                    .map(ComparisonRequisitionDTO.SupplierDTO::getPrice)
                    .filter(price -> price != null)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            lowestPrice = suppliers.stream()
                    .map(ComparisonRequisitionDTO.SupplierDTO::getPrice)
                    .filter(price -> price != null)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            if (selectedPrice != null && lowestPrice != null) {
                isBestPrice = selectedPrice.compareTo(lowestPrice) == 0;
            }
        }

        BigDecimal amtVnd = selectedPrice != null && orderQty != null ? selectedPrice.multiply(orderQty) : BigDecimal.ZERO;

        BigDecimal amtDifference = (amtVnd != null && highestPrice != null && orderQty != null && orderQty.compareTo(BigDecimal.ZERO) != 0)
                ? amtVnd.subtract(highestPrice.multiply(orderQty))
                : BigDecimal.ZERO;

        BigDecimal percentage = (amtVnd != null && amtDifference != null && amtVnd.compareTo(BigDecimal.ZERO) != 0)
                ? amtDifference.divide(amtVnd, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        List<ComparisonRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequestQty() != null ?
                req.getDepartmentRequestQty().entrySet().stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .map(entry -> {
                            String deptId = entry.getKey();
                            Object value = entry.getValue();
                            Integer qty = 0;
                            Integer buy = 0;

                            if (value instanceof DepartmentQty deptQty) {
                                qty = deptQty.getQty() != null ? deptQty.getQty().intValue() : 0;
                                buy = deptQty.getBuy() != null ? deptQty.getBuy().intValue() : 0;
                            } else if (value instanceof Double doubleQty) {
                                qty = doubleQty.intValue();
                                buy = qty;
                            }

                            Department dept = departmentRepository.findById(deptId).orElse(null);
                            String deptName = dept != null ? dept.getDepartmentName() : "Unknown";
                            return new ComparisonRequisitionDTO.DepartmentRequestDTO(deptId, deptName, qty, buy);
                        })
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        String type1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                ? (productType1Service.getById(req.getProductType1Id()) != null
                ? productType1Service.getById(req.getProductType1Id()).getName() : "")
                : "";
        String type2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                ? (productType2Service.getById(req.getProductType2Id()) != null
                ? productType2Service.getById(req.getProductType2Id()).getName() : "")
                : "";

        return new ComparisonRequisitionDTO(
                req.getEnglishName(),
                req.getVietnameseName(),
                req.getOldSapCode(),
                req.getHanaSapCode(),
                suppliers,
                req.getRemarkComparison(),
                departmentRequests,
                selectedPrice,
                amtVnd,
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
                currency,
                goodType,
                isBestPrice
        );
    }
}