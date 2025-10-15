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
    public ResponseEntity<SummaryRequisitionWithSupplierDTO> getById(@PathVariable String id) {
        return requisitionRepository.findById(id)
                .map(req -> {
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository.findById(req.getSupplierId()).orElse(null);
                    }

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

                    SummaryRequisitionWithSupplierDTO dto = new SummaryRequisitionWithSupplierDTO(req, supplierProduct, departmentQtyDTOs);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new summary requisition with multiple image uploads",
            description = "Create a summary requisition and upload multiple images using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = CreateSummaryRequisitionRequest.class)
                    )
            )
    )
    public ResponseEntity<?> create(@ModelAttribute CreateSummaryRequisitionRequest request) {
        try {
            // Validate required fields
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

            // Log the received departmentRequestQty for debugging
            System.out.println("Received departmentRequestQty: " + request.getDepartmentRequestQty());

            // Handle department quantities
            ObjectMapper mapper = new ObjectMapper();
            DepartmentRequestQtyDTO deptQtyDTO;
            try {
                if (request.getDepartmentRequestQty() != null && !request.getDepartmentRequestQty().isEmpty()) {
                    deptQtyDTO = mapper.readValue(request.getDepartmentRequestQty(), DepartmentRequestQtyDTO.class);
                    if (deptQtyDTO.getQuantities() == null || deptQtyDTO.getQuantities().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "At least one department quantity is required."));
                    }
                } else {
                    deptQtyDTO = new DepartmentRequestQtyDTO();
                    deptQtyDTO.setQuantities(new HashMap<>());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "DepartmentRequestQty cannot be empty."));
                }
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid JSON format for departmentRequestQty: " + e.getMessage()));
            }

            // Extract and validate quantities from DepartmentRequestQtyDTO
            Map<String, DepartmentQty> deptQty = deptQtyDTO.getQuantities();
            if (deptQty.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "No department quantities provided."));
            }
            for (Map.Entry<String, DepartmentQty> entry : deptQty.entrySet()) {
                if (entry.getValue().getBuy() != null && entry.getValue().getBuy() < 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Buy value cannot be negative for department: " + entry.getKey()));
                }
            }

            // Check for duplicate entry
            Optional<SummaryRequisition> existing = requisitionRepository.findByGroupIdAndOldSapCode(
                    request.getGroupId(), request.getOldSapCode()
            );

            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Duplicate entry: productType1Id, productType2Id, and oldSapCode must be unique together."));
            }

            // Handle image uploads
            List<String> imageUrls = new ArrayList<>();
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        if (!file.getContentType().startsWith("image/")) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("message", "Only image files are allowed: " + file.getOriginalFilename()));
                        }
                        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("message", "Image size exceeds 5MB: " + file.getOriginalFilename()));
                        }
                        String imageUrl = saveImage(file);
                        imageUrls.add(imageUrl);
                    }
                }
            }

            // Validate image count
            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }

            // Create and populate SummaryRequisition
            SummaryRequisition summary = new SummaryRequisition();
            summary.setEnglishName(request.getEnglishName());
            summary.setVietnameseName(request.getVietnameseName().trim());
            summary.setOldSapCode(request.getOldSapCode().trim());
            summary.setHanaSapCode(request.getHanaSapCode() != null ? request.getHanaSapCode().trim() : null);
            summary.setDepartmentRequestQty(deptQty);
            summary.setStock(request.getStock() != null ? request.getStock() : BigDecimal.ZERO);
            summary.setOrderQty(request.getOrderQty() != null ? request.getOrderQty() : BigDecimal.ZERO);
            summary.setReason(request.getReason());
            summary.setRemark(request.getRemark());
            summary.setRemarkComparison(request.getRemarkComparison());
            summary.setSupplierId(request.getSupplierId());
            summary.setGroupId(request.getGroupId());
            summary.setProductType1Id(request.getProductType1Id());
            summary.setProductType2Id(request.getProductType2Id());
            summary.setFullDescription(request.getFullDescription());
            summary.setImageUrls(imageUrls);
            summary.setCreatedAt(LocalDateTime.now());
            summary.setUpdatedAt(LocalDateTime.now());

            // Save to repository
            SummaryRequisition saved = requisitionRepository.save(summary);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Summary requisition created successfully!", "data", saved));

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

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update an existing summary requisition",
            description = "Update a summary requisition with updated fields and manage image files. " +
                    "The provided files list contains new image files to upload. Images specified in imagesToDelete (as a JSON string of URLs) are removed. Supports file uploads for new images.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UpdateSummaryRequisitionRequest.class)
                    )
            )
    )
    public ResponseEntity<?> update(@PathVariable String id, @ModelAttribute UpdateSummaryRequisitionRequest request) {
        try {
            Optional<SummaryRequisition> existingRequisition = requisitionRepository.findById(id);
            if (!existingRequisition.isPresent()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Requisition with ID " + id + " not found."));
            }

            SummaryRequisition current = existingRequisition.get();

            // New: Check for duplicate groupId and oldSapCode, excluding the current requisition
            if (request.getGroupId() != null && request.getOldSapCode() != null) {
                Optional<SummaryRequisition> existing = requisitionRepository.findByGroupIdAndOldSapCode(
                        request.getGroupId(), request.getOldSapCode()
                );
                if (existing.isPresent() && !existing.get().getId().equals(id)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "Duplicate entry: groupId and oldSapCode must be unique together."));
                }
            }

            // Handle department quantities
            Map<String, DepartmentQty> deptQty = current.getDepartmentRequestQty() != null ? new HashMap<>(current.getDepartmentRequestQty()) : new HashMap<>();
            if (request.getDepartmentRequestQty() != null && !request.getDepartmentRequestQty().isEmpty()) {
                try {
                    DepartmentRequestQtyDTO deptQtyDTO = objectMapper.readValue(request.getDepartmentRequestQty(), DepartmentRequestQtyDTO.class);
                    deptQty.clear();
                    if (deptQtyDTO.getQuantities() != null) {
                        deptQty.putAll(deptQtyDTO.getQuantities());
                    }
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid JSON format for departmentRequestQty: " + e.getMessage()));
                }
            }

            // Handle image uploads
            List<String> newImageUrls = current.getImageUrls() != null ? new ArrayList<>(current.getImageUrls()) : new ArrayList<>();
            List<MultipartFile> files = request.getFiles() != null ? request.getFiles() : new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String imageUrl = saveImage(file);
                    if (imageUrl != null) {
                        newImageUrls.add(imageUrl);
                    }
                }
            }

            // Handle image deletions
            List<String> imagesToDelete = new ArrayList<>();
            String imagesToDeleteJson = request.getImagesToDelete();
            if (imagesToDeleteJson != null && !imagesToDeleteJson.isEmpty()) {
                try {
                    imagesToDelete = objectMapper.readValue(imagesToDeleteJson, new TypeReference<List<String>>() {});
                    if (imagesToDelete.size() > 10) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Cannot delete more than 10 images at once."));
                    }
                    List<String> imagesToDeleteCopy = new ArrayList<>(imagesToDelete);
                    for (String url : imagesToDeleteCopy) {
                        if (url != null && url.startsWith("/uploads/") && !url.contains("..")) {
                            String normalizedUrl = url.split("\\?")[0].trim();
                            if (newImageUrls.contains(normalizedUrl)) {
                                newImageUrls.remove(normalizedUrl);
                                deleteImage(normalizedUrl);
                                System.out.println("Successfully processed deletion of: " + normalizedUrl);
                            } else {
                                System.out.println("Image URL not found in newImageUrls: " + normalizedUrl);
                            }
                        } else {
                            System.out.println("Invalid image URL skipped: " + url);
                        }
                    }
                } catch (JsonProcessingException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("message", "Invalid JSON format for imagesToDelete: " + e.getMessage()));
                }
            }

            // Validate image count
            if (newImageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum 10 images allowed."));
            }

            // Update fields only if provided
            current.setEnglishName(request.getEnglishName() != null ? request.getEnglishName() : current.getEnglishName());
            current.setVietnameseName(request.getVietnameseName() != null ? request.getVietnameseName() : current.getVietnameseName());
            current.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : current.getFullDescription());
            current.setOldSapCode(request.getOldSapCode() != null ? request.getOldSapCode() : current.getOldSapCode());
            current.setHanaSapCode(request.getHanaSapCode() != null ? request.getHanaSapCode() : current.getHanaSapCode());
            current.setDepartmentRequestQty(deptQty);
            current.setStock(request.getStock() != null ? request.getStock() : current.getStock());
            current.setOrderQty(request.getOrderQty() != null ? request.getOrderQty() : current.getOrderQty());
            current.setReason(request.getReason() != null ? request.getReason() : current.getReason());
            current.setRemark(request.getRemark() != null ? request.getRemark() : current.getRemark());
            current.setRemarkComparison(request.getRemarkComparison() != null ? request.getRemarkComparison() : current.getRemarkComparison());
            current.setSupplierId(request.getSupplierId() != null && !request.getSupplierId().isEmpty()
                    ? request.getSupplierId()
                    : current.getSupplierId());
            current.setGroupId(request.getGroupId() != null ? request.getGroupId() : current.getGroupId());
            current.setProductType1Id(request.getProductType1Id() != null ? request.getProductType1Id() : current.getProductType1Id());
            current.setProductType2Id(request.getProductType2Id() != null ? request.getProductType2Id() : current.getProductType2Id());
            current.setImageUrls(newImageUrls);
            current.setUpdatedAt(LocalDateTime.now());

            // Save to repository
            SummaryRequisition updated = requisitionRepository.save(current);
            System.out.println("Updated imageUrls in database: " + updated.getImageUrls());
            return ResponseEntity.ok(Map.of("message", "Requisition updated successfully", "data", updated));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing request: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while updating the requisition: " + e.getMessage()));
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
            Optional<SummaryRequisition> requisitionOptional = requisitionRepository.findById(id);
            if (requisitionOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", "ID not found: " + id));
            }

            SummaryRequisition requisition = requisitionOptional.get();
            String name = requisition.getEnglishName();

            List<String> imageUrls = requisition.getImageUrls();
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (String imageUrl : imageUrls) {
                    deleteImage(imageUrl);
                }
            }

            requisitionRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Name '" + name + "' has been deleted"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Invalid ID: " + id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "Deletion error: " + e.getMessage()));
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
            description = "Retrieve a paginated or full list of summary requisitions for a given group ID with optional filters for product types, names, SAP codes, supplier, and department, sorted by updatedAt or createdAt in descending order."
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

        List<SummaryRequisitionDTO> dtoList;

        if (disablePagination) {
            // No pagination: fetch all records and apply filters
            dtoList = requisitionRepository.findByGroupId(groupId).stream()
                    .filter(req -> {
                        boolean matches = true;

                        // Fetch ProductType1 and ProductType2 names
                        String reqProductType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                                ? (productType1Service.getById(req.getProductType1Id()) != null
                                ? productType1Service.getById(req.getProductType1Id()).getName() : "Unknown")
                                : "Unknown";
                        String reqProductType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                                ? (productType2Service.getById(req.getProductType2Id()) != null
                                ? productType2Service.getById(req.getProductType2Id()).getName() : "Unknown")
                                : "Unknown";

                        // Fetch department names
                        List<String> deptNames = req.getDepartmentRequestQty().entrySet().stream()
                                .map(entry -> departmentRepository.findById(entry.getKey())
                                        .map(Department::getDepartmentName)
                                        .orElse("Unknown"))
                                .collect(Collectors.toList());

                        // Apply filters only if hasFilter is true
                        if (hasFilter) {
                            if (productType1Name != null && !productType1Name.isEmpty()) {
                                matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                            }
                            if (productType2Name != null && !productType2Name.isEmpty()) {
                                matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                            }
                            if (englishName != null && !englishName.isEmpty()) {
                                matches = matches && req.getEnglishName() != null
                                        && req.getEnglishName().toLowerCase().contains(englishName.toLowerCase());
                            }
                            if (vietnameseName != null && !vietnameseName.isEmpty()) {
                                matches = matches && req.getVietnameseName() != null
                                        && req.getVietnameseName().toLowerCase().contains(vietnameseName.toLowerCase());
                            }
                            if (oldSapCode != null && !oldSapCode.isEmpty()) {
                                matches = matches && req.getOldSapCode() != null
                                        && req.getOldSapCode().toLowerCase().contains(oldSapCode.toLowerCase());
                            }
                            if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                                matches = matches && req.getHanaSapCode() != null
                                        && req.getHanaSapCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                            }
                            if (supplierName != null && !supplierName.isEmpty()) {
                                SupplierProduct supplierProduct = req.getSupplierId() != null
                                        ? supplierProductRepository.findById(req.getSupplierId()).orElse(null)
                                        : null;
                                String reqSupplierName = supplierProduct != null ? supplierProduct.getSupplierName() : "";
                                matches = matches && reqSupplierName.toLowerCase().contains(supplierName.toLowerCase());
                            }
                            if (departmentName != null && !departmentName.isEmpty()) {
                                matches = matches && deptNames.stream()
                                        .anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                            }
                        }

                        return matches;
                    })
                    .sorted((req1, req2) -> {
                        LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt()
                                : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                        LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt()
                                : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                        return date2.compareTo(date1); // Sort newest first
                    })
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // Return as a Page with total count
            return ResponseEntity.ok(new PageImpl<>(dtoList, PageRequest.of(0, dtoList.size()), dtoList.size()));
        } else {
            // Apply pagination
            Page<SummaryRequisition> requisitionPage = requisitionRepository.findByGroupId(groupId, pageable);

            dtoList = requisitionPage.getContent().stream()
                    .filter(req -> {
                        boolean matches = true;

                        // Fetch ProductType1 and ProductType2 names
                        String reqProductType1Name = req.getProductType1Id() != null
                                ? (productType1Service.getById(req.getProductType1Id()) != null
                                ? productType1Service.getById(req.getProductType1Id()).getName() : "Unknown")
                                : "Unknown";
                        String reqProductType2Name = req.getProductType2Id() != null
                                ? (productType2Service.getById(req.getProductType2Id()) != null
                                ? productType2Service.getById(req.getProductType2Id()).getName() : "Unknown")
                                : "Unknown";

                        // Fetch department names
                        List<String> deptNames = req.getDepartmentRequestQty().entrySet().stream()
                                .map(entry -> departmentRepository.findById(entry.getKey())
                                        .map(Department::getDepartmentName)
                                        .orElse("Unknown"))
                                .collect(Collectors.toList());

                        // Apply filters only if hasFilter is true
                        if (hasFilter) {
                            if (productType1Name != null && !productType1Name.isEmpty()) {
                                matches = matches && reqProductType1Name.toLowerCase().contains(productType1Name.toLowerCase());
                            }
                            if (productType2Name != null && !productType2Name.isEmpty()) {
                                matches = matches && reqProductType2Name.toLowerCase().contains(productType2Name.toLowerCase());
                            }
                            if (englishName != null && !englishName.isEmpty()) {
                                matches = matches && req.getEnglishName() != null
                                        && req.getEnglishName().toLowerCase().contains(englishName.toLowerCase());
                            }
                            if (vietnameseName != null && !vietnameseName.isEmpty()) {
                                matches = matches && req.getVietnameseName() != null
                                        && req.getVietnameseName().toLowerCase().contains(vietnameseName.toLowerCase());
                            }
                            if (oldSapCode != null && !oldSapCode.isEmpty()) {
                                matches = matches && req.getOldSapCode() != null
                                        && req.getOldSapCode().toLowerCase().contains(oldSapCode.toLowerCase());
                            }
                            if (hanaSapCode != null && !hanaSapCode.isEmpty()) {
                                matches = matches && req.getHanaSapCode() != null
                                        && req.getHanaSapCode().toLowerCase().contains(hanaSapCode.toLowerCase());
                            }
                            if (supplierName != null && !supplierName.isEmpty()) {
                                SupplierProduct supplierProduct = req.getSupplierId() != null
                                        ? supplierProductRepository.findById(req.getSupplierId()).orElse(null)
                                        : null;
                                String reqSupplierName = supplierProduct != null ? supplierProduct.getSupplierName() : "";
                                matches = matches && reqSupplierName.toLowerCase().contains(supplierName.toLowerCase());
                            }
                            if (departmentName != null && !departmentName.isEmpty()) {
                                matches = matches && deptNames.stream()
                                        .anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                            }
                        }

                        return matches;
                    })
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            // Return paginated result
            return ResponseEntity.ok(new PageImpl<>(dtoList, pageable, requisitionPage.getTotalElements()));
        }
    }

    private SummaryRequisitionDTO convertToDto(SummaryRequisition req) {
        // Fetch SupplierProduct
        SupplierProduct supplierProduct = req.getSupplierId() != null
                ? supplierProductRepository.findById(req.getSupplierId()).orElse(null)
                : null;

        // Calculate sumBuy (for DTO, not used in totalPrice)
        int sumBuy = 0;
        if (req.getDepartmentRequestQty() != null) {
            for (Object value : req.getDepartmentRequestQty().values()) {
                if (value instanceof DepartmentQty deptQty) {
                    sumBuy += deptQty.getBuy() != null ? deptQty.getBuy().intValue() : 0;
                } else if (value instanceof Double qty) {
                    sumBuy += qty.intValue(); // Old data: buy = qty
                }
            }
        }

        // Calculate totalPrice (orderQty * price)
        BigDecimal totalPrice = (req.getSupplierId() != null && supplierProduct != null
                && supplierProduct.getPrice() != null && req.getOrderQty() != null
                && req.getOrderQty().compareTo(BigDecimal.ZERO) != 0)
                ? supplierProduct.getPrice().multiply(req.getOrderQty())
                : BigDecimal.ZERO;

        // Fetch DepartmentRequestQty
        List<SummaryRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequestQty() != null
                ? req.getDepartmentRequestQty().entrySet().stream()
                .map(entry -> {
                    String departmentId = entry.getKey();
                    Object value = entry.getValue();
                    Integer qty = 0;
                    Integer buy = 0;

                    if (value instanceof DepartmentQty deptQty) {
                        qty = deptQty.getQty() != null ? deptQty.getQty().intValue() : 0;
                        buy = deptQty.getBuy() != null ? deptQty.getBuy().intValue() : 0;
                    } else if (value instanceof Double doubleQty) {
                        qty = doubleQty.intValue();
                        buy = qty; // Old data: buy = qty
                    }

                    Department department = departmentRepository.findById(departmentId).orElse(null);
                    String departmentName = department != null ? department.getDepartmentName() : "Unknown";
                    return new SummaryRequisitionDTO.DepartmentRequestDTO(departmentId, departmentName, qty, buy);
                })
                .collect(Collectors.toList())
                : new ArrayList<>();

        // Fetch ProductType1 and ProductType2 names
        String productType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                ? (productType1Service.getById(req.getProductType1Id()) != null
                ? productType1Service.getById(req.getProductType1Id()).getName() : "Unknown")
                : "Unknown";
        String productType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                ? (productType2Service.getById(req.getProductType2Id()) != null
                ? productType2Service.getById(req.getProductType2Id()).getName() : "Unknown")
                : "Unknown";

        // Create DTO
        return new SummaryRequisitionDTO(
                req,
                supplierProduct,
                departmentRequests,
                productType1Name,
                productType2Name,
                sumBuy,
                totalPrice,
                req.getCreatedAt() != null ? req.getCreatedAt().toString() : null,
                req.getUpdatedAt() != null ? req.getUpdatedAt().toString() : null
        );
    }

    @PostMapping(value = "/upload-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload Requisition File",
            description = "Upload an Excel .xlsx file with data starting from row 4. Columns: No (A), Item (B) [VN/EN with /], SAP code (C), Last times (D) [Price], Sup (E), Q'ty Request (F), Inhand (G), Buy (H), U/Price (I), Sup (J), Amount (K), Dept request (L), Reason (M), Picture (N)"
    )
    public ResponseEntity<List<SummaryRequisition>> uploadRequisitionFile(
            @Parameter(description = "Excel .xlsx file containing requisition data")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Group ID", required = true)
            @RequestParam(value = "groupId", required = true) String groupId) {

        List<SummaryRequisition> requisitions = new ArrayList<>();
        Map<String, Integer> descriptionMap = new HashMap<>();

        // Validate file
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonList(new SummaryRequisition() {{
                        setRemark("No file uploaded.");
                    }}));
        }
        if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonList(new SummaryRequisition() {{
                        setRemark("Only .xlsx files are allowed.");
                    }}));
        }

        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonList(new SummaryRequisition() {{
                            setRemark("Sheet not found in the uploaded file.");
                        }}));
            }

            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell noCell = getMergedCellValue(sheet, i, 0, mergedRegions);
                if (noCell == null || !isValidNo(noCell)) {
                    break;
                }

                SummaryRequisition req = new SummaryRequisition();
                req.setCreatedAt(LocalDateTime.now());
                req.setUpdatedAt(LocalDateTime.now());
                req.setGroupId(groupId);

                // Parse Item description (Column B - index 1)
                Cell descriptionCell = getMergedCellValue(sheet, i, 1, mergedRegions);
                String description = descriptionCell != null ? getCellValue(descriptionCell) : null;
                String vietnameseName = null;
                String englishName = null;
                if (description != null && description.contains("/")) {
                    String[] parts = description.split("/", 2);
                    vietnameseName = parts[0].trim();
                    if (parts.length > 1) {
                        englishName = parts[1].trim();
                    }
                } else {
                    vietnameseName = description != null ? description.trim() : null;
                }
                req.setVietnameseName(vietnameseName);
                req.setEnglishName(englishName);

                // Check for duplicates
                String key = (vietnameseName != null ? vietnameseName.toLowerCase() : "null") + "|" +
                        (englishName != null ? englishName.toLowerCase() : "null");
                if (descriptionMap.containsKey(key)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Collections.singletonList(new SummaryRequisition() {{
                                setRemark(String.format("Duplicate entry found "));
                            }}));
                }
                descriptionMap.put(key, i + 1);

                // Parse SAP code (Column C - index 2)
                Cell sapCell = getMergedCellValue(sheet, i, 2, mergedRegions);
                String sapCode = sapCell != null ? getCellValue(sapCell) : null;
                if (sapCode != null) {
                    if ("New".equalsIgnoreCase(sapCode.trim())) {
                        req.setHanaSapCode(sapCode);
                    } else {
                        req.setOldSapCode(sapCode);
                    }
                }

                // Parse Reason (Column M - index 12)
                Cell reasonCell = getMergedCellValue(sheet, i, 12, mergedRegions);
                req.setReason(reasonCell != null ? getCellValue(reasonCell) : null);

                // Parse Dept request (Column L - index 11) and find department ID, handling merge
                Cell deptCell = getMergedCellValue(sheet, i, 11, mergedRegions);
                String deptName = deptCell != null ? getCellValue(deptCell) : null;
                String deptId = null;
                if (deptName != null && !deptName.trim().isEmpty()) { // Kiểm tra deptName không rỗng
                    Department dept = departmentRepository.findByDepartmentName(deptName.trim());
                    deptId = dept != null ? dept.getId() : null;
                    if (deptId == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Collections.singletonList(new SummaryRequisition() {{
                                    setRemark(String.format("Department '%s' not found at row %d", deptName,  1));
                                }}));
                    }
                }

                // Parse Request qty (Column F - index 5), default to 0.0 if null or invalid
                Cell requestQtyCell = getMergedCellValue(sheet, i, 5, mergedRegions);
                Double requestQty = parseDouble(requestQtyCell) != null ? parseDouble(requestQtyCell) : 0.0;

                // Parse Inhand (Column G - index 6), default to 0.0 if null or invalid
                Cell inhandCell = getMergedCellValue(sheet, i, 6, mergedRegions);
                Double inhand = parseDouble(inhandCell) != null ? parseDouble(inhandCell) : 0.0;

                // Parse Buy (Column H - index 7), handle formula case
                Cell buyCell = getMergedCellValue(sheet, i, 7, mergedRegions);
                Double buy = 0.0;
                if (buyCell != null && buyCell.getCellType() == CellType.FORMULA) {
                    // If formula, calculate Buy as Request qty - Inhand
                    buy = requestQty - inhand;
                } else {
                    // If direct number, parse the value
                    buy = parseDouble(buyCell) != null ? parseDouble(buyCell) : 0.0;
                }

                // Set department quantity if valid
                if (deptId != null) {
                    Map<String, DepartmentQty> deptQtyMap = new HashMap<>();
                    DepartmentQty deptQty = new DepartmentQty();
                    deptQty.setQty(requestQty);
                    deptQty.setBuy(buy);
                    deptQtyMap.put(deptId, deptQty);
                    req.setDepartmentRequestQty(deptQtyMap);
                } else {
                    req.setDepartmentRequestQty(new HashMap<>());
                }

                // Handle Picture (Column N - index 13)
                List<String> imageUrls = new ArrayList<>();
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture picture = (XSSFPicture) shape;
                        XSSFClientAnchor anchor = picture.getClientAnchor();
                        // Kiểm tra nếu anchor thuộc row hiện tại (cột N - index 13)
                        if (anchor.getCol1() == 13 && anchor.getRow1() <= i && anchor.getRow2() >= i) {
                            byte[] imageBytes = picture.getPictureData().getData();
                            String imagePath = saveImage(imageBytes, "image_" + i + "_" + System.currentTimeMillis() + ".png");
                            if (imagePath != null) {
                                imageUrls.add(imagePath);
                            }
                        }
                    }
                }
                req.setImageUrls(imageUrls.isEmpty() ? null : imageUrls);

                requisitions.add(req);
            }

            if (requisitions.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonList(new SummaryRequisition() {{
                            setRemark("No valid data found in the file starting from row 4.");
                        }}));
            }

            List<SummaryRequisition> savedRequisitions = requisitionRepository.saveAll(requisitions);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRequisitions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonList(new SummaryRequisition() {{
                        setRemark("Error processing file: " + e.getMessage());
                    }}));
        }
    }

    // Helper method to validate No (STT) (only accept numeric, break if not number)
    private boolean isValidNo(Cell noCell) {
        if (noCell == null) return false;
        return noCell.getCellType() == CellType.NUMERIC; // Chỉ chấp nhận số, break nếu không phải số
    }

    // Helper method to get cell value, handling merge cells
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

    // Helper method to parse Double value, handling merge cells
    private Double parseDouble(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null; // Trả null nếu không parse được
            }
        }
        return null;
    }

    // Helper method to get value from cell, handling if it's part of a merged region
    private Cell getMergedCellValue(Sheet sheet, int rowIndex, int colIndex, List<CellRangeAddress> mergedRegions) {
        for (CellRangeAddress range : mergedRegions) {
            if (range.isInRange(rowIndex, colIndex)) {
                // Lấy cell đầu tiên (top-left) của merge region
                Row firstRow = sheet.getRow(range.getFirstRow());
                return firstRow.getCell(range.getFirstColumn(), Row.MissingCellPolicy.RETURN_NULL_AND_BLANK);
            }
        }
        // Nếu không phải merge, lấy cell bình thường
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

    private boolean isValidStt(Cell cell) {
        if (cell == null) return false;
        try {
            double sttValue = cell.getNumericCellValue();
            int sttInt = (int) sttValue;
            return sttValue == sttInt && sttInt >= 1;
        } catch (Exception e) {
            return false;
        }
    }

    @GetMapping("/search/comparison")
    @Operation(summary = "Search requisitions for comparison by group ID and optional filters",
            description = "Retrieve a paginated or full list of summary requisitions for a given group ID with optional filters for product types, names, Old SAP code, Hana SAP code, supplier name, and department name, sorted by updatedAt or createdAt in descending order. Includes sumBuy (total buy quantity across departments) and financial totals for each requisition.")
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
            Pageable pageable) {

        // Fetch GroupSummaryRequisition once using groupId
        Optional<GroupSummaryRequisition> group = groupSummaryRequisitionService.getGroupSummaryRequisitionById(groupId);
        String currency = group.isPresent() && group.get().getCurrency() != null ? group.get().getCurrency() : "";

        List<ComparisonRequisitionDTO> dtoList;
        long totalElements;

        if (disablePagination) {
            // No pagination: fetch all records and apply filters
            List<SummaryRequisition> requisitions = requisitionRepository.findByGroupId(groupId);
            totalElements = requisitions.size();

            dtoList = requisitions.stream()
                    .filter(req -> {
                        boolean matches = true;

                        String reqProductType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                                ? (productType1Service.getById(req.getProductType1Id()) != null
                                ? productType1Service.getById(req.getProductType1Id()).getName() : "Unknown")
                                : "Unknown";
                        String reqProductType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                                ? (productType2Service.getById(req.getProductType2Id()) != null
                                ? productType2Service.getById(req.getProductType2Id()).getName() : "Unknown")
                                : "Unknown";

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
                    .map(req -> convertToDtos(req, currency)) // Pass currency to convertToDtos
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

            // Create Page object for response
            Page<ComparisonRequisitionDTO> page = new PageImpl<>(dtoList, PageRequest.of(0, Math.max(dtoList.size(), 1)), totalElements);

            // Create response DTO
            ComparisonRequisitionResponseDTO response = new ComparisonRequisitionResponseDTO(
                    page,
                    totalAmtVnd,
                    totalAmtDifference,
                    totalDifferencePercentage
            );

            return ResponseEntity.ok(response);
        } else {
            // Apply pagination
            Page<SummaryRequisition> requisitionPage = requisitionRepository.findByGroupId(groupId, pageable);
            totalElements = requisitionPage.getTotalElements();

            dtoList = requisitionPage.getContent().stream()
                    .filter(req -> {
                        boolean matches = true;

                        String reqProductType1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                                ? (productType1Service.getById(req.getProductType1Id()) != null
                                ? productType1Service.getById(req.getProductType1Id()).getName() : "Unknown")
                                : "Unknown";
                        String reqProductType2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                                ? (productType2Service.getById(req.getProductType2Id()) != null
                                ? productType2Service.getById(req.getProductType2Id()).getName() : "Unknown")
                                : "Unknown";

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
                    .map(req -> convertToDtos(req, currency)) // Pass currency to convertToDtos
                    .collect(Collectors.toList());

            // Calculate totals
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

            // Create Page object for response
            Page<ComparisonRequisitionDTO> page = new PageImpl<>(dtoList, pageable, totalElements);

            // Create response DTO
            ComparisonRequisitionResponseDTO response = new ComparisonRequisitionResponseDTO(
                    page,
                    totalAmt,
                    totalAmtDifference,
                    totalDifferencePercentage
            );

            return ResponseEntity.ok(response);
        }
    }

    private ComparisonRequisitionDTO convertToDtos(SummaryRequisition req, String currency) {
        List<ComparisonRequisitionDTO.SupplierDTO> suppliers;

        String sapCode = req.getOldSapCode() != null && !req.getOldSapCode().isEmpty() ? req.getOldSapCode() : null;
        String selectedSupplierId = req.getSupplierId();

        String unit = "";
        String goodType = null; // Default goodType

        if (sapCode != null && sapCode.length() >= 3) {
            // Use currency passed from searchRequisitions
            List<SupplierProduct> supplierProducts = (currency != null && !currency.isEmpty()) ?
                    supplierProductRepository.findBySapCodeAndCurrency(sapCode, currency) :
                    supplierProductRepository.findBySapCode(sapCode);

            suppliers = supplierProducts.stream()
                    .map(sp -> new ComparisonRequisitionDTO.SupplierDTO(
                            sp.getPrice(),
                            sp.getSupplierName(),
                            selectedSupplierId != null && !selectedSupplierId.isEmpty() && selectedSupplierId.equals(sp.getId()) ? 1 : 0,
                            sp.getUnit()))
                    .sorted(Comparator.comparing(ComparisonRequisitionDTO.SupplierDTO::getPrice, Comparator.nullsLast(BigDecimal::compareTo)))
                    .collect(Collectors.toList());

            if (selectedSupplierId != null && !selectedSupplierId.isEmpty()) {
                SupplierProduct selectedSupplier = supplierProducts.stream()
                        .filter(sp -> sp.getId().equals(selectedSupplierId))
                        .findFirst()
                        .orElse(null);
                if (selectedSupplier != null) {
                    unit = selectedSupplier.getUnit() != null ? selectedSupplier.getUnit() : unit;
                    currency = selectedSupplier.getCurrency() != null ? selectedSupplier.getCurrency() : currency; // Update currency if available
                    goodType = selectedSupplier.getGoodType() != null ? selectedSupplier.getGoodType() : null;
                }
            }
        } else {
            suppliers = Collections.emptyList();
        }

        // Calculate requestQty to store in DTO (total buy from departmentRequestQty)
        int requestQty = 0;
        if (req.getDepartmentRequestQty() != null) {
            for (Object value : req.getDepartmentRequestQty().values()) {
                if (value instanceof DepartmentQty deptQty) {
                    requestQty += deptQty.getBuy() != null ? deptQty.getBuy().intValue() : 0;
                } else if (value instanceof Double qty) {
                    requestQty += qty.intValue(); // Old data: buy = qty
                }
            }
        }

        // Use orderQty (BigDecimal) for calculations
        BigDecimal orderQty = req.getOrderQty() != null && req.getOrderQty().compareTo(BigDecimal.ZERO) != 0 ? req.getOrderQty() : BigDecimal.ZERO;

        BigDecimal selectedPrice = null;
        BigDecimal highestPrice = null;
        if (selectedSupplierId != null && !selectedSupplierId.isEmpty() && !suppliers.isEmpty()) {
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
        }

        BigDecimal amtVnd = selectedPrice != null && orderQty != null ? selectedPrice.multiply(orderQty) : null;

        BigDecimal amtDifference = (amtVnd != null && highestPrice != null && orderQty != null)
                ? amtVnd.subtract(highestPrice.multiply(orderQty))
                : null;

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
                                buy = qty; // Old data: buy = qty
                            }

                            Department dept = departmentRepository.findById(deptId).orElse(null);
                            String deptName = dept != null ? dept.getDepartmentName() : "Unknown";
                            return new ComparisonRequisitionDTO.DepartmentRequestDTO(deptId, deptName, qty, buy);
                        })
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        String type1Name = req.getProductType1Id() != null && !req.getProductType1Id().isEmpty()
                ? (productType1Service.getById(req.getProductType1Id()) != null
                ? productType1Service.getById(req.getProductType1Id()).getName() : "Unknown")
                : "Unknown";
        String type2Name = req.getProductType2Id() != null && !req.getProductType2Id().isEmpty()
                ? (productType2Service.getById(req.getProductType2Id()) != null
                ? productType2Service.getById(req.getProductType2Id()).getName() : "Unknown")
                : "Unknown";

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
                goodType
        );
    }
}