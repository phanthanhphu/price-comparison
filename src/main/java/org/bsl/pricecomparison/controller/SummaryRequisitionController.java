package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.bsl.pricecomparison.dto.SummaryRequisitionDTO;
import org.bsl.pricecomparison.dto.SummaryRequisitionWithSupplierDTO;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.DepartmentRepository;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.bsl.pricecomparison.repository.SummaryRequisitionRepository;
import org.bsl.pricecomparison.request.CreateSummaryRequisitionRequest;
import org.bsl.pricecomparison.request.UpdateProductRequest;
import org.bsl.pricecomparison.request.UpdateSummaryRequisitionRequest;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
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
                    SummaryRequisitionWithSupplierDTO dto = new SummaryRequisitionWithSupplierDTO(req, supplierProduct);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

//    @PostMapping
//    public ResponseEntity<?> create(@RequestBody SummaryRequisition summary) {
//        Optional<SummaryRequisition> existing = requisitionRepository.findByProductType1IdAndProductType2IdAndNewSapCode(
//                summary.getProductType1Id(),
//                summary.getProductType2Id(),
//                summary.getNewSapCode()
//        );
//
//        if (existing.isPresent()) {
//            return ResponseEntity
//                    .badRequest()
//                    .body("Duplicate entry: productType1Id, productType2Id, and newSapCode must be unique together.");
//        }
//
//        summary.setCreatedAt(LocalDateTime.now());
//        summary.setUpdatedAt(LocalDateTime.now());
//
//        SummaryRequisition saved = requisitionRepository.save(summary);
//        return ResponseEntity.ok(saved);
//    }

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
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Double> deptQty;
            try {
                // Chuyển đổi departmentRequestQty từ chuỗi JSON thành Map
                deptQty = request.getDepartmentRequestQty() != null
                        ? mapper.readValue(request.getDepartmentRequestQty(), new TypeReference<Map<String, Double>>() {})
                        : new HashMap<>();
            } catch (JsonProcessingException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid JSON format for departmentRequestQty: " + e.getMessage());
            }

            // Kiểm tra trùng lặp
            Optional<SummaryRequisition> existing = requisitionRepository.findByProductType1IdAndProductType2IdAndNewSapCode(
                    request.getProductType1Id(),
                    request.getProductType2Id(),
                    request.getNewSapCode()
            );

            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Duplicate entry: productType1Id, productType2Id, and newSapCode must be unique together.");
            }

            // Xử lý file upload
            List<String> imageUrls = new ArrayList<>();
            List<MultipartFile> files = request.getFiles();
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String imageUrl = saveImage(file);
                        imageUrls.add(imageUrl);
                    }
                }
            }

            // Kiểm tra số lượng hình ảnh
            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Maximum 10 images allowed.");
            }

            // Tạo SummaryRequisition
            SummaryRequisition summary = new SummaryRequisition();
            summary.setEnglishName(request.getEnglishName());
            summary.setVietnameseName(request.getVietnameseName());
            summary.setOldSapCode(request.getOldSapCode());
            summary.setNewSapCode(request.getNewSapCode());
            summary.setDepartmentRequestQty(deptQty);
            summary.setStock(request.getStock());
            summary.setPurchasingSuggest(request.getPurchasingSuggest());
            summary.setReason(request.getReason());
            summary.setRemark(request.getRemark());
            summary.setSupplierId(request.getSupplierId());
            summary.setGroupId(request.getGroupId());
            summary.setProductType1Id(request.getProductType1Id());
            summary.setProductType2Id(request.getProductType2Id());
            summary.setFullDescription(request.getFullDescription());
            summary.setImageUrls(imageUrls);
            summary.setCreatedAt(LocalDateTime.now());
            summary.setUpdatedAt(LocalDateTime.now());

            SummaryRequisition saved = requisitionRepository.save(summary);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
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

//    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(
//            summary = "Update an existing summary requisition with multiple image uploads",
//            description = "Update a summary requisition and manage multiple images using multipart/form-data.",
//            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    content = @Content(
//                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
//                            schema = @Schema(implementation = CreateSummaryRequisitionRequest.class)
//                    )
//            )
//    )
//    public ResponseEntity<?> update(@PathVariable String id, @ModelAttribute CreateSummaryRequisitionRequest request) {
//        try {
//            Optional<SummaryRequisition> existingRequisition = requisitionRepository.findById(id);
//            if (!existingRequisition.isPresent()) {
//                return ResponseEntity
//                        .status(HttpStatus.NOT_FOUND)
//                        .body("Requisition with ID " + id + " not found.");
//            }
//
//            // Kiểm tra trùng lặp (trừ chính bản ghi đang cập nhật)
//            Optional<SummaryRequisition> duplicate = requisitionRepository.findByProductType1IdAndProductType2IdAndNewSapCode(
//                    request.getProductType1Id(),
//                    request.getProductType2Id(),
//                    request.getNewSapCode()
//            );
//            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
//                return ResponseEntity
//                        .status(HttpStatus.CONFLICT)
//                        .body("Duplicate entry: productType1Id, productType2Id, and newSapCode must be unique together.");
//            }
//
//            SummaryRequisition current = existingRequisition.get();
//
//            // Chuyển đổi departmentRequestQty từ chuỗi JSON thành Map
//            ObjectMapper mapper = new ObjectMapper();
//            Map<String, Double> deptQty = request.getDepartmentRequestQty() != null
//                    ? mapper.readValue(request.getDepartmentRequestQty(), new TypeReference<Map<String, Double>>() {})
//                    : current.getDepartmentRequestQty();
//
//            // Xử lý file upload và cập nhật imageUrls
//            List<String> updatedImageUrls = new ArrayList<>(current.getImageUrls() != null ? current.getImageUrls() : new ArrayList<>());
//            List<MultipartFile> files = request.getFiles();
//            if (files != null && !files.isEmpty()) {
//                for (MultipartFile file : files) {
//                    if (file != null && !file.isEmpty()) {
//                        String imageUrl = saveImage(file);
//                        updatedImageUrls.add(imageUrl);
//                    }
//                }
//            }
//
//            // Kiểm tra số lượng hình ảnh
//            if (updatedImageUrls.size() > 10) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body("Maximum 10 images allowed.");
//            }
//
//            // Cập nhật các trường
//            current.setEnglishName(request.getEnglishName() != null ? request.getEnglishName() : current.getEnglishName());
//            current.setVietnameseName(request.getVietnameseName() != null ? request.getVietnameseName() : current.getVietnameseName());
//            current.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : current.getFullDescription());
//            current.setOldSapCode(request.getOldSapCode() != null ? request.getOldSapCode() : current.getOldSapCode());
//            current.setNewSapCode(request.getNewSapCode() != null ? request.getNewSapCode() : current.getNewSapCode());
//            current.setDepartmentRequestQty(deptQty);
//            current.setStock(request.getStock() != null ? request.getStock() : current.getStock());
//            current.setPurchasingSuggest(request.getPurchasingSuggest() != null ? request.getPurchasingSuggest() : current.getPurchasingSuggest());
//            current.setReason(request.getReason() != null ? request.getReason() : current.getReason());
//            current.setRemark(request.getRemark() != null ? request.getRemark() : current.getRemark());
//            current.setSupplierId(request.getSupplierId() != null && !request.getSupplierId().isEmpty()
//                    ? request.getSupplierId()
//                    : current.getSupplierId());
//            current.setGroupId(request.getGroupId() != null ? request.getGroupId() : current.getGroupId());
//            current.setProductType1Id(request.getProductType1Id() != null ? request.getProductType1Id() : current.getProductType1Id());
//            current.setProductType2Id(request.getProductType2Id() != null ? request.getProductType2Id() : current.getProductType2Id());
//            current.setImageUrls(updatedImageUrls);
//            current.setUpdatedAt(LocalDateTime.now());
//
//            SummaryRequisition updated = requisitionRepository.save(current);
//            return ResponseEntity.ok(updated);
//
//        } catch (JsonProcessingException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body("Invalid JSON format for departmentRequestQty: " + e.getMessage());
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error processing file: " + e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("An error occurred while updating the requisition: " + e.getMessage());
//        }
//    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update an existing summary requisition",
            description = "Update a summary requisition with updated fields and manage image URLs. The provided imageUrls list is treated as the definitive list of images to retain. Images not included in the list are deleted. Supports file uploads for new images.",
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
                        .body("Requisition with ID " + id + " not found.");
            }

            Optional<SummaryRequisition> duplicate = requisitionRepository.findByProductType1IdAndProductType2IdAndNewSapCode(
                    request.getProductType1Id(),
                    request.getProductType2Id(),
                    request.getNewSapCode()
            );
            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body("Duplicate entry: productType1Id, productType2Id, and newSapCode must be unique together.");
            }

            SummaryRequisition current = existingRequisition.get();

            Map<String, Double> deptQty = request.getDepartmentRequestQty() != null
                    ? objectMapper.readValue(request.getDepartmentRequestQty(), new TypeReference<Map<String, Double>>() {})
                    : current.getDepartmentRequestQty();

            // Xử lý imageUrls
            List<String> newImageUrls = request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>();
            List<String> currentImageUrls = current.getImageUrls() != null ? current.getImageUrls() : new ArrayList<>();

            // Xác định các hình ảnh cần xóa
            for (String url : currentImageUrls) {
                if (!newImageUrls.contains(url)) {
                    deleteImage(url); // Hàm xóa file vật lý trên server
                }
            }

            // Kiểm tra số lượng hình ảnh
            if (newImageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Maximum 10 images allowed.");
            }

            // Cập nhật các trường
            current.setEnglishName(request.getEnglishName() != null ? request.getEnglishName() : current.getEnglishName());
            current.setVietnameseName(request.getVietnameseName() != null ? request.getVietnameseName() : current.getVietnameseName());
            current.setFullDescription(request.getFullDescription() != null ? request.getFullDescription() : current.getFullDescription());
            current.setOldSapCode(request.getOldSapCode() != null ? request.getOldSapCode() : current.getOldSapCode());
            current.setNewSapCode(request.getNewSapCode() != null ? request.getNewSapCode() : current.getNewSapCode());
            current.setDepartmentRequestQty(deptQty);
            current.setStock(request.getStock() != null ? request.getStock() : current.getStock());
            current.setPurchasingSuggest(request.getPurchasingSuggest() != null ? request.getPurchasingSuggest() : current.getPurchasingSuggest());
            current.setReason(request.getReason() != null ? request.getReason() : current.getReason());
            current.setRemark(request.getRemark() != null ? request.getRemark() : current.getRemark());
            current.setSupplierId(request.getSupplierId() != null && !request.getSupplierId().isEmpty()
                    ? request.getSupplierId()
                    : current.getSupplierId());
            current.setGroupId(request.getGroupId() != null ? request.getGroupId() : current.getGroupId());
            current.setProductType1Id(request.getProductType1Id() != null ? request.getProductType1Id() : current.getProductType1Id());
            current.setProductType2Id(request.getProductType2Id() != null ? request.getProductType2Id() : current.getProductType2Id());
            current.setImageUrls(newImageUrls);
            current.setUpdatedAt(LocalDateTime.now());

            SummaryRequisition updated = requisitionRepository.save(current);
            return ResponseEntity.ok(updated);

        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON format for departmentRequestQty: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating the requisition: " + e.getMessage());
        }
    }

    // Hàm xóa file vật lý trên server
    private void deleteImage(String imageUrl) {
        try {
            // Giả sử imageUrl là đường dẫn tương đối, ví dụ: "/uploads/image.jpg"
            String filePath = UPLOAD_DIR + imageUrl; // Điều chỉnh theo cấu hình lưu trữ
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("Failed to delete image: " + imageUrl + ", error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        requisitionRepository.deleteById(id);
    }

    @GetMapping("/with-suppliers")
    public List<SummaryRequisitionWithSupplierDTO> getAllWithSupplierInfo() {
        List<SummaryRequisition> requisitions = requisitionRepository.findAll();

        return requisitions.stream()
                .map(req -> {
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository
                                .findById(req.getSupplierId())
                                .orElse(null);
                    }
                    return new SummaryRequisitionWithSupplierDTO(req, supplierProduct);
                })
                .collect(Collectors.toList());
    }


    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<SummaryRequisitionDTO>> getAllByGroupId(@PathVariable String groupId) {
        List<SummaryRequisition> requisitions = requisitionRepository.findByGroupId(groupId);

        // Sort requisitions by updatedAt (if exists) and then createdAt (descending)
        List<SummaryRequisitionDTO> dtoList = requisitions.stream()
                .sorted((req1, req2) -> {
                    LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .map(req -> {
                    // Fetch SupplierProduct
                    SupplierProduct supplierProduct = null;
                    if (req.getSupplierId() != null) {
                        supplierProduct = supplierProductRepository.findById(req.getSupplierId()).orElse(null);
                    }

                    // Fetch Department Name for departmentRequestQty
                    List<SummaryRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequestQty().entrySet().stream()
                            .map(entry -> {
                                String departmentId = entry.getKey();
                                Double quantityDouble = entry.getValue();
                                Integer quantity = quantityDouble != null ? quantityDouble.intValue() : 0;
                                Department department = departmentRepository.findById(departmentId).orElse(null);
                                String departmentName = department != null ? department.getDepartmentName() : "Unknown";
                                return new SummaryRequisitionDTO.DepartmentRequestDTO(departmentId, departmentName, quantity);
                            })
                            .collect(Collectors.toList());

                    // Fetch ProductType1 and ProductType2 Names
                    String productType1Name = null;
                    if (req.getProductType1Id() != null) {
                        ProductType1 productType1 = productType1Service.getById(req.getProductType1Id());
                        productType1Name = productType1 != null ? productType1.getName() : "Unknown";
                    }

                    String productType2Name = null;
                    if (req.getProductType2Id() != null) {
                        ProductType2 productType2 = productType2Service.getById(req.getProductType2Id());
                        productType2Name = productType2 != null ? productType2.getName() : "Unknown";
                    }

                    // Create DTO with all required fields
                    return new SummaryRequisitionDTO(
                            req, supplierProduct, departmentRequests, productType1Name, productType2Name
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<SummaryRequisitionDTO>> searchRequisitions(
            @RequestParam String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String newSapCode,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String departmentName) {

        List<SummaryRequisitionDTO> dtoList = requisitionRepository.findByGroupId(groupId).stream()
                .filter(req -> {
                    boolean matches = true;

                    // Fetch ProductType1 and ProductType2 Names for filtering
                    String reqProductType1Name = req.getProductType1Id() != null ?
                            (productType1Service.getById(req.getProductType1Id()) != null ?
                                    productType1Service.getById(req.getProductType1Id()).getName() : "Unknown") : "Unknown";
                    String reqProductType2Name = req.getProductType2Id() != null ?
                            (productType2Service.getById(req.getProductType2Id()) != null ?
                                    productType2Service.getById(req.getProductType2Id()).getName() : "Unknown") : "Unknown";

                    // Fetch Department Names for filtering
                    List<String> deptNames = req.getDepartmentRequestQty().entrySet().stream()
                            .map(entry -> {
                                String deptId = entry.getKey();
                                Department dept = departmentRepository.findById(deptId).orElse(null);
                                return dept != null ? dept.getDepartmentName() : "Unknown";
                            })
                            .collect(Collectors.toList());

                    // Apply filters
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
                    if (newSapCode != null && !newSapCode.isEmpty()) {
                        matches = matches && req.getNewSapCode() != null && req.getNewSapCode().toLowerCase().contains(newSapCode.toLowerCase());
                    }
                    if (unit != null && !unit.isEmpty()) {
                        SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;
                        String reqUnit = supplierProduct != null ? supplierProduct.getUnit() : "";
                        matches = matches && reqUnit.toLowerCase().contains(unit.toLowerCase());
                    }
                    if (departmentName != null && !departmentName.isEmpty()) {
                        matches = matches && deptNames.stream().anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                    }

                    return matches;
                })
                .sorted((req1, req2) -> {
                    LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .map(req -> convertToDto(req))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    private SummaryRequisitionDTO convertToDto(SummaryRequisition req) {
        // Fetch SupplierProduct
        SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;

        // Fetch Department Name for departmentRequestQty
        List<SummaryRequisitionDTO.DepartmentRequestDTO> departmentRequests = req.getDepartmentRequestQty().entrySet().stream()
                .map(entry -> {
                    String departmentId = entry.getKey();
                    Double quantityDouble = entry.getValue();
                    Integer quantity = quantityDouble != null ? quantityDouble.intValue() : 0;
                    Department department = departmentRepository.findById(departmentId).orElse(null);
                    String departmentName = department != null ? department.getDepartmentName() : "Unknown";
                    return new SummaryRequisitionDTO.DepartmentRequestDTO(departmentId, departmentName, quantity);
                })
                .collect(Collectors.toList());

        // Fetch ProductType1 and ProductType2 Names
        String productType1Name = req.getProductType1Id() != null ?
                (productType1Service.getById(req.getProductType1Id()) != null ?
                        productType1Service.getById(req.getProductType1Id()).getName() : "Unknown") : "Unknown";
        String productType2Name = req.getProductType2Id() != null ?
                (productType2Service.getById(req.getProductType2Id()) != null ?
                        productType2Service.getById(req.getProductType2Id()).getName() : "Unknown") : "Unknown";

        return new SummaryRequisitionDTO(req, supplierProduct, departmentRequests, productType1Name, productType2Name);
    }

    private List<SummaryRequisitionDTO> convertToDtoList(List<SummaryRequisition> requisitions) {
        return requisitions.stream()
                .sorted((req1, req2) -> {
                    LocalDateTime date1 = req1.getUpdatedAt() != null ? req1.getUpdatedAt() : req1.getCreatedAt() != null ? req1.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime date2 = req2.getUpdatedAt() != null ? req2.getUpdatedAt() : req2.getCreatedAt() != null ? req2.getCreatedAt() : LocalDateTime.MIN;
                    return date2.compareTo(date1); // Descending order (newest first)
                })
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/search/comparison")
    public ResponseEntity<Page<SummaryRequisitionDTO>> searchRequisitions(
            @RequestParam String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String newSapCode,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        // Create Pageable object for pagination and sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt")));

        // Fetch all requisitions for the groupId
        List<SummaryRequisition> requisitions = requisitionRepository.findByGroupId(groupId);

        // Apply filtering
        List<SummaryRequisitionDTO> filteredList = requisitions.stream()
                .filter(req -> {
                    boolean matches = true;

                    // Fetch ProductType1 and ProductType2 Names for filtering
                    String reqProductType1Name = req.getProductType1Id() != null ?
                            (productType1Service.getById(req.getProductType1Id()) != null ?
                                    productType1Service.getById(req.getProductType1Id()).getName() : "Unknown") : "Unknown";
                    String reqProductType2Name = req.getProductType2Id() != null ?
                            (productType2Service.getById(req.getProductType2Id()) != null ?
                                    productType2Service.getById(req.getProductType2Id()).getName() : "Unknown") : "Unknown";

                    // Fetch Department Names for filtering
                    List<String> deptNames = req.getDepartmentRequestQty().entrySet().stream()
                            .map(entry -> {
                                String deptId = entry.getKey();
                                Department dept = departmentRepository.findById(deptId).orElse(null);
                                return dept != null ? dept.getDepartmentName() : "Unknown";
                            })
                            .collect(Collectors.toList());

                    // Apply filters
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
                    if (newSapCode != null && !newSapCode.isEmpty()) {
                        matches = matches && req.getNewSapCode() != null && req.getNewSapCode().toLowerCase().contains(newSapCode.toLowerCase());
                    }
                    if (unit != null && !unit.isEmpty()) {
                        SupplierProduct supplierProduct = req.getSupplierId() != null ? supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;
                        String reqUnit = supplierProduct != null ? supplierProduct.getUnit() : "";
                        matches = matches && reqUnit.toLowerCase().contains(unit.toLowerCase());
                    }
                    if (departmentName != null && !departmentName.isEmpty()) {
                        matches = matches && deptNames.stream().anyMatch(dept -> dept.toLowerCase().contains(departmentName.toLowerCase()));
                    }

                    return matches;
                })
                .map(req -> convertToDto(req))
                .collect(Collectors.toList());

        // Apply pagination manually
        int start = Math.min((int) pageable.getOffset(), filteredList.size());
        int end = Math.min(start + pageable.getPageSize(), filteredList.size());
        List<SummaryRequisitionDTO> pagedList = filteredList.subList(start, end);

        // Create Page object
        Page<SummaryRequisitionDTO> pagedResult = new PageImpl<>(pagedList, pageable, filteredList.size());

        return ResponseEntity.ok(pagedResult);
    }

    @PostMapping(value = "/upload-requisition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Requisition File", description = "Upload an Excel .xlsx file with data starting from row 13. Columns: STT (A), Description (B) [Vietnamese/English], (C empty), Ref. No. (D), Part. No. (E), OLD Sap Code (F), NEW Sap Code (G), Quantity (H), Unit (I), Inventory SAP (J), Inventory MED (K), Picture (L), Last order (M), Remarks (N)")
    public ResponseEntity<List<SummaryRequisition>> uploadRequisitionFile(
            @Parameter(description = "Excel .xlsx file containing requisition data")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "ID of the department")
            @RequestParam(value = "idPhongBan", required = true) String idPhongBan,
            @Parameter(description = "Group ID")
            @RequestParam(value = "groupId", required = true) String groupId) {

        List<SummaryRequisition> requisitions = new ArrayList<>();
        Map<String, Integer> descriptionMap = new HashMap<>();

        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 12; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell sttCell = row.getCell(0);
                if (sttCell == null || !isValidStt(sttCell)) {
                    break;
                }

                SummaryRequisition req = new SummaryRequisition();

                String description = getCellValue(row.getCell(1));
                String vietnameseName = null;
                String englishName = null;
                if (description != null && description.contains("/")) {
                    String[] parts = description.split("/", 2);
                    vietnameseName = parts[0].trim();
                    if (parts.length > 1) {
                        englishName = parts[1].trim();
                    }
                } else {
                    vietnameseName = description;
                }
                req.setVietnameseName(vietnameseName);
                req.setEnglishName(englishName);

                String key = (vietnameseName != null ? vietnameseName.toLowerCase() : "null") + "|" +
                        (englishName != null ? englishName.toLowerCase() : "null");
                if (descriptionMap.containsKey(key)) {
                    String finalVietnameseName = vietnameseName;
                    String finalEnglishName = englishName;
                    int finalI = i;
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Collections.singletonList(new SummaryRequisition() {{
                                setRemark(String.format("Duplicate entry found at row %d and row %d: Vietnamese='%s', English='%s'",
                                        descriptionMap.get(key) + 1, finalI + 1, finalVietnameseName, finalEnglishName));
                            }}));
                } else {
                    descriptionMap.put(key, i + 1);
                }

                req.setOldSapCode(getCellValue(row.getCell(5)));     // col F (index 5): OLD Sap Code
                req.setNewSapCode(getCellValue(row.getCell(6)));     // col G (index 6): NEW Sap Code
                Double quantity = parseDouble(row.getCell(7));       // col H (index 7): Quantity (numeric)
                req.setRemark(getCellValue(row.getCell(13)));        // col N (index 13): Remarks

                if (quantity != null) {
                    Map<String, Double> deptQtyMap = new HashMap<>();
                    deptQtyMap.put(idPhongBan, quantity);
                    req.setDepartmentRequestQty(deptQtyMap);
                }

                req.setGroupId(groupId);
                req.setCreatedAt(LocalDateTime.now());
                req.setUpdatedAt(LocalDateTime.now());

                List<String> imageUrls = new ArrayList<>();
                XSSFDrawing drawing = workbook.getSheetAt(0).createDrawingPatriarch();
                List<XSSFShape> shapes = drawing.getShapes();
                for (XSSFShape shape : shapes) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture picture = (XSSFPicture) shape;
                        XSSFClientAnchor anchor = picture.getClientAnchor();
                        if (anchor.getCol1() == 11 && anchor.getRow1() == i) {
                            byte[] imageBytes = picture.getPictureData().getData();
                            String imagePath = saveImage(imageBytes, "image_" + i + ".png");
                            if (imagePath != null) {
                                imageUrls.add(imagePath);
                            }
                        }
                    }
                }
                req.setImageUrls(imageUrls.isEmpty() ? null : imageUrls);
                requisitions.add(req);
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
            int sttInt = (int) sttValue; // Chuyển sang int để kiểm tra
            return sttValue == sttInt && sttInt >= 1; // Chấp nhận bất kỳ số nguyên nào từ 1 trở lên
        } catch (Exception e) {
            return false; // Nếu không phải số, trả về false và gây break
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private Double parseDouble(Cell cell) {
        if (cell == null) return null;
        try {
            return cell.getNumericCellValue();
        } catch (Exception e) {
            return null;
        }
    }

}