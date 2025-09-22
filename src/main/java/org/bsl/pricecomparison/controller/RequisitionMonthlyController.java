package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.model.DepartmentRequisitionMonthly;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.bsl.pricecomparison.repository.RequisitionMonthlyRepository;
import org.bsl.pricecomparison.repository.SupplierProductRepository;
import org.bsl.pricecomparison.request.CreateRequisitionMonthlyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
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

    private static final String UPLOAD_DIR = "./Uploads/";

    @PostMapping(value = "/requisition-monthly", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addRequisitionMonthly(@ModelAttribute CreateRequisitionMonthlyRequest request) {
        try {
            // Validate input
            if (request.getOldSAPCode() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("oldSAPCode is required");
            }

            // Lấy thông tin Supplier nếu supplierId được cung cấp
            SupplierProduct supplier = null;
            if (request.getSupplierId() != null && !request.getSupplierId().isEmpty()) {
                Optional<SupplierProduct> supplierOptional = supplierRepository.findById(request.getSupplierId());
                if (supplierOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Supplier not found for supplierId: " + request.getSupplierId());
                }
                supplier = supplierOptional.get();
            }

            // Tạo đối tượng RequisitionMonthly
            RequisitionMonthly requisition = new RequisitionMonthly();
            requisition.setItemDescriptionEN(request.getItemDescriptionEN());
            requisition.setItemDescriptionVN(request.getItemDescriptionVN());
            requisition.setOldSAPCode(request.getOldSAPCode());
            requisition.setSapCodeNewSAP(request.getSapCodeNewSAP());
            requisition.setUnit(supplier != null ? supplier.getUnit() : null);
            requisition.setTotalNotIssuedQty(request.getTotalNotIssuedQty() != null ?
                    request.getTotalNotIssuedQty() : 0.0);
            requisition.setInHand(request.getInHand() != null ? request.getInHand() : 0.0);
            requisition.setPrice(supplier != null && supplier.getPrice() != null ? supplier.getPrice() : 0.0);
            requisition.setSupplierId(request.getSupplierId());
            requisition.setSupplierName(supplier != null ? supplier.getSupplierName() : null);
            requisition.setProductType1Id(request.getProductType1Id());
            requisition.setProductType2Id(request.getProductType2Id());
            requisition.setCreatedDate(LocalDateTime.now());
            requisition.setUpdatedDate(LocalDateTime.now());

            // Chuyển đổi chuỗi JSON departmentRequisitions thành List<DepartmentRequisitionMonthly>
            List<DepartmentRequisitionMonthly> deptRequisitions = new ArrayList<>();
            if (request.getDepartmentRequisitions() != null && !request.getDepartmentRequisitions().isEmpty()) {
                try {
                    List<DepartmentRequisitionMonthly.DepartmentRequestDTO> deptDTOs = objectMapper.readValue(
                            request.getDepartmentRequisitions(),
                            new TypeReference<List<DepartmentRequisitionMonthly.DepartmentRequestDTO>>() {}
                    );
                    for (DepartmentRequisitionMonthly.DepartmentRequestDTO dto : deptDTOs) {
                        deptRequisitions.add(new DepartmentRequisitionMonthly(
                                dto.getDepartmentId(),
                                dto.getDepartmentName(),
                                dto.getQty(),
                                dto.getBuy()
                        ));
                    }
                } catch (IOException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid departmentRequisitions JSON format: " + e.getMessage());
                }
            }
            requisition.setDepartmentRequisitions(deptRequisitions);

            // Tính toán totalRequestQty từ buy của các phòng ban
            Double totalRequestQty = deptRequisitions.stream()
                    .map(DepartmentRequisitionMonthly::getBuy)
                    .filter(buy -> buy != null)
                    .mapToDouble(Integer::doubleValue)
                    .sum();
            requisition.setTotalRequestQty(totalRequestQty);

            // Tính toán actualInHand: totalNotIssuedQty - inHand
            Double actualInHand = (requisition.getTotalNotIssuedQty() != null ? requisition.getTotalNotIssuedQty() : 0.0) -
                    (requisition.getInHand() != null ? requisition.getInHand() : 0.0);
            requisition.setActualInHand(actualInHand);

            // Tính toán orderQty: totalRequestQty - actualInHand
            Double orderQty = totalRequestQty - actualInHand;
            requisition.setOrderQty(orderQty);

            // Tính toán amount: price * orderQty
            Double amount = (requisition.getPrice() != null ? requisition.getPrice() : 0.0) * orderQty;
            requisition.setAmount(amount);

            // Xử lý upload hình ảnh
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

            // Kiểm tra số lượng hình ảnh
            if (imageUrls.size() > 10) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Maximum 10 images allowed.");
            }
            requisition.setImageUrls(imageUrls);

            // Lưu vào database
            RequisitionMonthly savedRequisition = requisitionMonthlyRepository.save(requisition);

            // Ghi log để debug
            System.out.println("Saved Requisition: id=" + savedRequisition.getId() +
                    ", unit=" + savedRequisition.getUnit() +
                    ", totalRequestQty=" + savedRequisition.getTotalRequestQty() +
                    ", actualInHand=" + savedRequisition.getActualInHand() +
                    ", orderQty=" + savedRequisition.getOrderQty() +
                    ", price=" + savedRequisition.getPrice() +
                    ", amount=" + savedRequisition.getAmount() +
                    ", supplierName=" + savedRequisition.getSupplierName() +
                    ", imageUrls=" + savedRequisition.getImageUrls());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedRequisition);

        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
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

        return "/Uploads/" + fileName;
    }

    @GetMapping("/requisition-monthly/filter")
    public ResponseEntity<Page<RequisitionMonthly>> filterRequisitions(
            @RequestParam String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String newSapCode,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String departmentName,
            Pageable pageable) {

        // Check if any optional filter parameters are provided
        boolean hasFilters = productType1Name != null || productType2Name != null ||
                englishName != null || vietnameseName != null ||
                oldSapCode != null || newSapCode != null ||
                unit != null || departmentName != null;

        List<RequisitionMonthly> requisitions;

        if (hasFilters) {
            // Apply filters if any filter parameters are provided
            requisitions = requisitionMonthlyRepository.findByGroupId(groupId).stream()
                    .filter(req -> {
                        boolean matches = true;

                        // Apply filters
                        if (productType1Name != null && !productType1Name.isEmpty()) {
                            matches = matches && req.getProductType1Id() != null &&
                                    req.getProductType1Id().toString().contains(productType1Name);
                        }
                        if (productType2Name != null && !productType2Name.isEmpty()) {
                            matches = matches && req.getProductType2Id() != null &&
                                    req.getProductType2Id().toString().contains(productType2Name);
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
                        if (newSapCode != null && !newSapCode.isEmpty()) {
                            matches = matches && req.getSapCodeNewSAP() != null &&
                                    req.getSapCodeNewSAP().toLowerCase().contains(newSapCode.toLowerCase());
                        }
                        if (unit != null && !unit.isEmpty()) {
                            SupplierProduct supplierProduct = req.getSupplierId() != null ?
                                    supplierRepository.findById(req.getSupplierId()).orElse(null) : null;
                            String reqUnit = supplierProduct != null ? supplierProduct.getUnit() : "";
                            matches = matches && reqUnit.toLowerCase().contains(unit.toLowerCase());
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
            // No filters provided, fetch all records for groupId with sorting
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

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), requisitions.size());
        List<RequisitionMonthly> pagedRequisitions = requisitions.subList(start, end);

        // Create Page object
        Page<RequisitionMonthly> pagedResult = new PageImpl<>(pagedRequisitions, pageable, requisitions.size());

        return ResponseEntity.ok(pagedResult);
    }
}