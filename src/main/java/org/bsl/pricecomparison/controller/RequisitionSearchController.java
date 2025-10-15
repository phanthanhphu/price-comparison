package org.bsl.pricecomparison.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.bsl.pricecomparison.dto.*;
import org.bsl.pricecomparison.model.*;
import org.bsl.pricecomparison.repository.*;
import org.bsl.pricecomparison.request.DepartmentQty;
import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
import org.bsl.pricecomparison.service.ProductType1Service;
import org.bsl.pricecomparison.service.ProductType2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/requisitions/search")
public class RequisitionSearchController {

    private static final Logger logger = LoggerFactory.getLogger(RequisitionSearchController.class);

    @Autowired
    private SummaryRequisitionRepository summaryRequisitionRepository;

    @Autowired
    private RequisitionMonthlyRepository requisitionMonthlyRepository;

    @Autowired
    private ProductType1Service productType1Service;

    @Autowired
    private ProductType2Service productType2Service;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SupplierProductRepository supplierProductRepository;

    @Autowired
    private GroupSummaryRequisitionService groupSummaryRequisitionService;

    public enum DataType {
        SUMMARY, MONTHLY, ALL
    }

    /**
     * ✅ UNIFIED RESPONSE với PAGINATION SUPPORT:
     * - SUMMARY/MONTHLY: { "dataType", "requisitions", "totalElements", "pagination" }
     * - ALL: { "dataType": "ALL", "summary": {...}, "monthly": {...} }
     */
    @GetMapping
    @Operation(
            summary = "Unified search for both Summary and Monthly requisitions",
            description = "Search with optional filters và pagination. **groupId='all'** gets data from ALL groups. " +
                    "**Pagination**: Use `page`, `size`, `sort`. **disablePagination=true** to get all data. " +
                    "Response always uses **'requisitions'** key."
    )
    public ResponseEntity<?> searchComparison(
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String productType1Name,
            @RequestParam(required = false) String productType2Name,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) String vietnameseName,
            @RequestParam(required = false) String oldSapCode,
            @RequestParam(required = false) String hanaSapCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String departmentName,
            @RequestParam(defaultValue = "false") boolean hasFilter,
            @RequestParam(defaultValue = "false") boolean disablePagination,
            @RequestParam(defaultValue = "SUMMARY") DataType dataType,
            Pageable pageable) {

        try {
            List<String> groupIds = getGroupIds(groupId);
            logger.info("🔍 Search: dataType={}, groups={}, hasFilter={}, disablePagination={}, page={}, size={}",
                    dataType, groupIds.size(), hasFilter, disablePagination,
                    pageable != null ? pageable.getPageNumber() : 0,
                    pageable != null ? pageable.getPageSize() : "all");

            switch (dataType) {
                case ALL:
                    return handleAllDataTypes(groupIds, productType1Name, productType2Name,
                            englishName, vietnameseName, oldSapCode, hanaSapCode, supplierName,
                            unit, departmentName, hasFilter, disablePagination, pageable);
                case SUMMARY:
                    return handleSummarySearch(groupIds, productType1Name, productType2Name,
                            englishName, vietnameseName, oldSapCode, hanaSapCode, supplierName,
                            departmentName, hasFilter, disablePagination, pageable);
                case MONTHLY:
                    return handleMonthlySearch(groupIds, productType1Name, productType2Name,
                            englishName, vietnameseName, oldSapCode, hanaSapCode, unit,
                            departmentName, hasFilter, disablePagination, pageable);
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid dataType"));
            }
        } catch (Exception e) {
            logger.error("❌ Search error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private List<String> getGroupIds(String groupId) {
        if (groupId == null || "all".equalsIgnoreCase(groupId.trim()) || groupId.trim().isEmpty()) {
            List<String> allGroupIds = groupSummaryRequisitionService.getAllGroupIds();
            logger.info("🔍 Getting ALL groups: {} groups", allGroupIds.size());
            return allGroupIds;
        }
        return Collections.singletonList(groupId.trim());
    }

    /**
     * ✅ ALL với PAGINATION cho sub-responses
     */
    private ResponseEntity<?> handleAllDataTypes(
            List<String> groupIds, String productType1Name, String productType2Name,
            String englishName, String vietnameseName, String oldSapCode,
            String hanaSapCode, String supplierName, String unit,
            String departmentName, boolean hasFilter, boolean disablePagination, Pageable pageable) {

        Map<String, Object> response = new HashMap<>();
        response.put("dataType", "ALL");
        response.put("groupIdsCount", groupIds.size());
        response.put("timestamp", LocalDateTime.now().toString());

        // Summary với pagination
        ResponseEntity<?> summaryResponse = getSummaryRequisitions(groupIds, productType1Name, productType2Name,
                englishName, vietnameseName, oldSapCode, hanaSapCode, supplierName, departmentName,
                hasFilter, disablePagination, pageable);
        if (summaryResponse.getBody() instanceof Map) {
            Map<String, Object> summary = (Map<String, Object>) summaryResponse.getBody();
            response.put("summary", Map.of(
                    "requisitions", summary.get("requisitions"),
                    "pagination", summary.get("pagination"),
                    "totalElements", summary.get("totalElements")
            ));
        }

        // Monthly với pagination
        ResponseEntity<?> monthlyResponse = getMonthlyRequisitions(groupIds, productType1Name, productType2Name,
                englishName, vietnameseName, oldSapCode, hanaSapCode, unit, departmentName,
                hasFilter, disablePagination, pageable);
        if (monthlyResponse.getBody() instanceof Map) {
            Map<String, Object> monthly = (Map<String, Object>) monthlyResponse.getBody();
            response.put("monthly", Map.of(
                    "requisitions", monthly.get("requisitions"),
                    "pagination", monthly.get("pagination"),
                    "totalElements", monthly.get("totalElements")
            ));
        }

        logger.info("✅ ALL: Summary={}, Monthly={}",
                ((Map<?, ?>) response.get("summary")).get("totalElements"),
                ((Map<?, ?>) response.get("monthly")).get("totalElements"));

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ SUMMARY Handler với PAGINATION
     */
    private ResponseEntity<?> handleSummarySearch(
            List<String> groupIds, String productType1Name, String productType2Name,
            String englishName, String vietnameseName, String oldSapCode,
            String hanaSapCode, String supplierName, String departmentName,
            boolean hasFilter, boolean disablePagination, Pageable pageable) {

        return getSummaryRequisitions(groupIds, productType1Name, productType2Name,
                englishName, vietnameseName, oldSapCode, hanaSapCode, supplierName, departmentName,
                hasFilter, disablePagination, pageable);
    }

    /**
     * ✅ MONTHLY Handler với PAGINATION
     */
    private ResponseEntity<?> handleMonthlySearch(
            List<String> groupIds, String productType1Name, String productType2Name,
            String englishName, String vietnameseName, String oldSapCode,
            String hanaSapCode, String unit, String departmentName,
            boolean hasFilter, boolean disablePagination, Pageable pageable) {

        return getMonthlyRequisitions(groupIds, productType1Name, productType2Name,
                englishName, vietnameseName, oldSapCode, hanaSapCode, unit, departmentName,
                hasFilter, disablePagination, pageable);
    }

    /**
     * ✅ CORE SUMMARY LOGIC với PAGINATION: Load → Filter → DTO → Paginate
     */
    private ResponseEntity<?> getSummaryRequisitions(
            List<String> groupIds, String productType1Name, String productType2Name,
            String englishName, String vietnameseName, String oldSapCode,
            String hanaSapCode, String supplierName, String departmentName,
            boolean hasFilter, boolean disablePagination, Pageable pageable) {

        // Load ALL data first
        List<SummaryRequisition> allRequisitions = new ArrayList<>();
        for (String gid : groupIds) {
            try {
                List<SummaryRequisition> requisitions = summaryRequisitionRepository.findByGroupId(gid);
                allRequisitions.addAll(requisitions);
                logger.debug("📦 Group {}: {} summary requisitions", gid, requisitions.size());
            } catch (Exception e) {
                logger.warn("⚠️ Error loading summary for group {}: {}", gid, e.getMessage());
            }
        }

        logger.info("📊 Total summary loaded: {} from {} groups", allRequisitions.size(), groupIds.size());

        // Filter & Sort
        List<SummaryRequisition> filtered = allRequisitions.stream()
                .filter(req -> applySummaryFilters(req, productType1Name, productType2Name,
                        englishName, vietnameseName, oldSapCode, hanaSapCode, supplierName, departmentName, hasFilter))
                .sorted(getSummaryComparator())
                .collect(Collectors.toList());

        // Convert to DTOs
        List<SummaryRequisitionDTO> dtoList = filtered.stream()
                .map(this::convertSummaryToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return buildPaginatedResponse("SUMMARY", dtoList, disablePagination, pageable);
    }

    /**
     * ✅ CORE MONTHLY LOGIC với PAGINATION
     */
    private ResponseEntity<?> getMonthlyRequisitions(
            List<String> groupIds, String productType1Name, String productType2Name,
            String englishName, String vietnameseName, String oldSapCode,
            String hanaSapCode, String unit, String departmentName,
            boolean hasFilter, boolean disablePagination, Pageable pageable) {

        List<RequisitionMonthly> allRequisitions = new ArrayList<>();
        Map<String, String> groupCurrencyMap = new HashMap<>();

        // Load currencies + data
        for (String gid : groupIds) {
            try {
                // Get currency
                groupSummaryRequisitionService.getGroupSummaryRequisitionById(gid)
                        .ifPresent(group -> {
                            String currency = group.getCurrency();
                            if (currency != null && !currency.trim().isEmpty()) {
                                groupCurrencyMap.put(gid, currency);
                            }
                        });

                // Load monthly data
                List<RequisitionMonthly> requisitions = requisitionMonthlyRepository.findByGroupId(gid);
                allRequisitions.addAll(requisitions);
                logger.debug("📦 Group {}: {} monthly requisitions", gid, requisitions.size());
            } catch (Exception e) {
                logger.warn("⚠️ Error loading monthly for group {}: {}", gid, e.getMessage());
            }
        }

        logger.info("📊 Total monthly loaded: {}", allRequisitions.size());

        // Filter
        List<RequisitionMonthly> filtered = allRequisitions;
        if (hasFilter) {
            filtered = allRequisitions.stream()
                    .filter(req -> applyMonthlyFilters(req, productType1Name, productType2Name,
                            englishName, vietnameseName, oldSapCode, hanaSapCode, unit, departmentName))
                    .collect(Collectors.toList());
        }

        filtered.sort(getMonthlyComparator());

        // Convert to DTOs với currency per group
        List<MonthlyComparisonRequisitionDTO> dtoList = filtered.parallelStream()
                .map(req -> {
                    String currency = groupCurrencyMap.getOrDefault(req.getGroupId(), "VND");
                    return convertMonthlyToComparisonDTO(req, currency);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return buildPaginatedResponse("MONTHLY", dtoList, disablePagination, pageable);
    }

    /**
     * ✅ BUILD PAGINATED RESPONSE - Unified cho cả SUMMARY và MONTHLY
     */
    private ResponseEntity<?> buildPaginatedResponse(String dataType, List<?> dtoList,
                                                     boolean disablePagination, Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        response.put("dataType", dataType);
        response.put("totalElements", dtoList.size());

        if (disablePagination || pageable == null) {
            // Return ALL data
            response.put("requisitions", dtoList);
            response.put("pagination", Map.of(
                    "disabled", true,
                    "totalElements", dtoList.size()
            ));
        } else {
            // Apply pagination
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();
            int start = page * size;
            int end = Math.min(start + size, dtoList.size());

            List<?> pagedList;
            if (start >= dtoList.size()) {
                pagedList = Collections.emptyList();
            } else {
                pagedList = dtoList.subList(start, end);
            }

            response.put("requisitions", pagedList);
            response.put("pagination", Map.of(
                    "page", page,
                    "size", size,
                    "totalPages", (int) Math.ceil((double) dtoList.size() / size),
                    "totalElements", dtoList.size(),
                    "currentElements", pagedList.size(),
                    "hasNext", (page + 1) * size < dtoList.size(),
                    "hasPrevious", page > 0
            ));
        }

        logger.info("✅ {}: {} total, page {} size {} (current: {})",
                dataType, dtoList.size(),
                pageable != null ? pageable.getPageNumber() : 0,
                pageable != null ? pageable.getPageSize() : dtoList.size(),
                ((List<?>) response.get("requisitions")).size());

        return ResponseEntity.ok(response);
    }

    // ✅ FILTERS
    private boolean applySummaryFilters(SummaryRequisition req, String productType1Name,
                                        String productType2Name, String englishName, String vietnameseName,
                                        String oldSapCode, String hanaSapCode, String supplierName,
                                        String departmentName, boolean hasFilter) {
        if (!hasFilter) return true;

        boolean matches = true;
        matches &= checkFilter(productType1Name, getProductType1Name(req.getProductType1Id()));
        matches &= checkFilter(productType2Name, getProductType2Name(req.getProductType2Id()));
        matches &= checkFilter(englishName, req.getEnglishName());
        matches &= checkFilter(vietnameseName, req.getVietnameseName());
        matches &= checkFilter(oldSapCode, req.getOldSapCode());
        matches &= checkFilter(hanaSapCode, req.getHanaSapCode());
        matches &= checkSupplierFilter(supplierName, req.getSupplierId());
        matches &= checkDepartmentFilter(departmentName, getSummaryDepartmentNames(req.getDepartmentRequestQty()));

        return matches;
    }

    private boolean applyMonthlyFilters(RequisitionMonthly req, String productType1Name,
                                        String productType2Name, String englishName, String vietnameseName,
                                        String oldSapCode, String hanaSapCode, String unit, String departmentName) {
        boolean matches = true;
        matches &= checkFilter(productType1Name, getProductType1Name(req.getProductType1Id()));
        matches &= checkFilter(productType2Name, getProductType2Name(req.getProductType2Id()));
        matches &= checkFilter(englishName, req.getItemDescriptionEN());
        matches &= checkFilter(vietnameseName, req.getItemDescriptionVN());
        matches &= checkFilter(oldSapCode, req.getOldSAPCode());
        matches &= checkFilter(hanaSapCode, req.getHanaSAPCode());
        matches &= checkFilter(unit, req.getUnit());
        matches &= checkDepartmentFilter(departmentName, getMonthlyDepartmentNames(req.getDepartmentRequisitions()));
        return matches;
    }

    // ✅ HELPERS
    private String getProductType1Name(String productType1Id) {
        if (productType1Id == null || productType1Id.isEmpty()) return "Unknown";
        try {
            ProductType1 pt1 = productType1Service.getById(productType1Id);
            return pt1 != null ? pt1.getName() : "Unknown";
        } catch (Exception e) {
            logger.warn("Error getting ProductType1 {}: {}", productType1Id, e.getMessage());
            return "Unknown";
        }
    }

    private String getProductType2Name(String productType2Id) {
        if (productType2Id == null || productType2Id.isEmpty()) return "Unknown";
        try {
            ProductType2 pt2 = productType2Service.getById(productType2Id);
            return pt2 != null ? pt2.getName() : "Unknown";
        } catch (Exception e) {
            logger.warn("Error getting ProductType2 {}: {}", productType2Id, e.getMessage());
            return "Unknown";
        }
    }

    private List<String> getMonthlyDepartmentNames(List<DepartmentRequisitionMonthly> depts) {
        if (depts == null) return Collections.emptyList();
        return depts.stream()
                .filter(Objects::nonNull)
                .map(DepartmentRequisitionMonthly::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> getSummaryDepartmentNames(Map<String, DepartmentQty> deptQtyMap) {
        if (deptQtyMap == null || deptQtyMap.isEmpty()) return Collections.emptyList();
        return deptQtyMap.keySet().stream()
                .filter(Objects::nonNull)
                .map(deptId -> departmentRepository.findById(deptId)
                        .map(Department::getDepartmentName)
                        .orElse("Unknown"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean checkFilter(String filter, String value) {
        return filter == null || filter.trim().isEmpty() ||
                (value != null && value.toLowerCase().contains(filter.toLowerCase().trim()));
    }

    private boolean checkSupplierFilter(String supplierName, String supplierId) {
        if (supplierName == null || supplierName.trim().isEmpty()) return true;
        if (supplierId == null) return false;
        try {
            SupplierProduct supplier = supplierProductRepository.findById(supplierId).orElse(null);
            return supplier != null && supplier.getSupplierName() != null &&
                    supplier.getSupplierName().toLowerCase().contains(supplierName.toLowerCase().trim());
        } catch (Exception e) {
            logger.warn("Error checking supplier filter for id {}: {}", supplierId, e.getMessage());
            return false;
        }
    }

    private boolean checkDepartmentFilter(String deptName, List<String> deptNames) {
        if (deptName == null || deptName.trim().isEmpty()) return true;
        return deptNames.stream()
                .anyMatch(d -> d.toLowerCase().contains(deptName.toLowerCase().trim()));
    }

    private Comparator<SummaryRequisition> getSummaryComparator() {
        return Comparator.comparing(
                (SummaryRequisition req) -> req.getUpdatedAt() != null ? req.getUpdatedAt() : req.getCreatedAt(),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    private Comparator<RequisitionMonthly> getMonthlyComparator() {
        return Comparator.comparing(
                (RequisitionMonthly req) -> req.getUpdatedDate() != null ? req.getUpdatedDate() : req.getCreatedDate(),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    // ✅ CONVERTERS
    private SummaryRequisitionDTO convertSummaryToDto(SummaryRequisition req) {
        try {
            SupplierProduct supplier = req.getSupplierId() != null ?
                    supplierProductRepository.findById(req.getSupplierId()).orElse(null) : null;

            List<SummaryRequisitionDTO.DepartmentRequestDTO> deptRequests =
                    getSummaryDeptRequests(req.getDepartmentRequestQty());

            String pt1Name = getProductType1Name(req.getProductType1Id());
            String pt2Name = getProductType2Name(req.getProductType2Id());

            // Tính sumBuy và totalPrice
            int sumBuy = 0;
            BigDecimal totalPrice = BigDecimal.ZERO;
            if (supplier != null && supplier.getPrice() != null &&
                    req.getOrderQty() != null && req.getOrderQty().compareTo(BigDecimal.ZERO) != 0) {
                totalPrice = supplier.getPrice().multiply(req.getOrderQty());
            }

            Map<String, DepartmentQty> deptQtyMap = req.getDepartmentRequestQty();
            if (deptQtyMap != null && !deptQtyMap.isEmpty()) {
                for (DepartmentQty deptQty : deptQtyMap.values()) {
                    if (deptQty != null && deptQty.getBuy() != null) {
                        sumBuy += deptQty.getBuy().intValue();
                    }
                }
            }

            return new SummaryRequisitionDTO(
                    req, supplier, deptRequests, pt1Name, pt2Name, sumBuy, totalPrice,
                    req.getCreatedAt() != null ? req.getCreatedAt().toString() : null,
                    req.getUpdatedAt() != null ? req.getUpdatedAt().toString() : null
            );
        } catch (Exception e) {
            logger.error("Error converting summary req {}: {}", req.getId(), e.getMessage());
            return null;
        }
    }

    private List<SummaryRequisitionDTO.DepartmentRequestDTO> getSummaryDeptRequests(Map<String, DepartmentQty> deptQtyMap) {
        if (deptQtyMap == null || deptQtyMap.isEmpty()) return Collections.emptyList();
        return deptQtyMap.entrySet().stream()
                .map(entry -> {
                    String deptId = entry.getKey();
                    DepartmentQty qty = entry.getValue();
                    int q = qty != null && qty.getQty() != null ? qty.getQty().intValue() : 0;
                    int b = qty != null && qty.getBuy() != null ? qty.getBuy().intValue() : 0;
                    String name = departmentRepository.findById(deptId)
                            .map(Department::getDepartmentName).orElse("Unknown");
                    return new SummaryRequisitionDTO.DepartmentRequestDTO(deptId, name, q, b);
                })
                .collect(Collectors.toList());
    }

    private MonthlyComparisonRequisitionDTO convertMonthlyToComparisonDTO(RequisitionMonthly req, String currency) {
        try {
            List<MonthlyComparisonRequisitionDTO.SupplierDTO> suppliers = getMonthlySuppliers(req, currency);

            BigDecimal price = getSelectedPrice(suppliers);
            BigDecimal highestPrice = getHighestPrice(suppliers);
            BigDecimal orderQty = req.getOrderQty() != null ? req.getOrderQty() : BigDecimal.ZERO;
            BigDecimal amount = price != null ? price.multiply(orderQty) : null;
            BigDecimal diff = (amount != null && highestPrice != null) ?
                    amount.subtract(highestPrice.multiply(orderQty)) : null;
            BigDecimal percentage = calculatePercentage(amount, diff);

            List<MonthlyComparisonRequisitionDTO.DepartmentRequestDTO> deptRequests =
                    getMonthlyDeptRequests(req.getDepartmentRequisitions());

            String pt1Name = getProductType1Name(req.getProductType1Id());
            String pt2Name = getProductType2Name(req.getProductType2Id());

            return new MonthlyComparisonRequisitionDTO(
                    req.getItemDescriptionEN(), req.getItemDescriptionVN(), req.getOldSAPCode(), req.getHanaSAPCode(),
                    suppliers, req.getRemarkComparison(), deptRequests, amount, diff, percentage,
                    highestPrice, req.getProductType1Id(), req.getProductType2Id(), pt1Name, pt2Name,
                    req.getUnit(), req.getDailyMedInventory(), req.getTotalRequestQty(), req.getSafeStock(),
                    req.getUseStockQty(), orderQty, price, currency,
                    req.getGoodType() != null ? req.getGoodType() : "");
        } catch (Exception e) {
            logger.error("Error converting monthly req {}: {}", req.getId(), e.getMessage());
            return null;
        }
    }

    private List<MonthlyComparisonRequisitionDTO.SupplierDTO> getMonthlySuppliers(RequisitionMonthly req, String currency) {
        String sapCode = req.getOldSAPCode();
        if (sapCode == null || sapCode.trim().isEmpty()) return Collections.emptyList();

        List<SupplierProduct> suppliers;
        try {
            suppliers = supplierProductRepository.findBySapCodeAndCurrency(sapCode.trim(), currency);
        } catch (Exception e) {
            logger.warn("Error finding suppliers for sapCode {} currency {}: {}", sapCode, currency, e.getMessage());
            return Collections.emptyList();
        }

        String selectedId = req.getSupplierId();
        return suppliers.stream()
                .map(sp -> new MonthlyComparisonRequisitionDTO.SupplierDTO(
                        sp.getPrice(), sp.getSupplierName(),
                        selectedId != null && selectedId.equals(sp.getId()) ? 1 : 0, sp.getUnit()))
                .sorted(Comparator.comparing(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private BigDecimal getSelectedPrice(List<MonthlyComparisonRequisitionDTO.SupplierDTO> suppliers) {
        return suppliers.stream()
                .filter(s -> s.getIsSelected() == 1)
                .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private BigDecimal getHighestPrice(List<MonthlyComparisonRequisitionDTO.SupplierDTO> suppliers) {
        return suppliers.stream()
                .map(MonthlyComparisonRequisitionDTO.SupplierDTO::getPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo).orElse(null);
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal diff) {
        if (amount == null || diff == null || amount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return diff.divide(amount, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private List<MonthlyComparisonRequisitionDTO.DepartmentRequestDTO> getMonthlyDeptRequests(List<DepartmentRequisitionMonthly> depts) {
        if (depts == null) return Collections.emptyList();
        return depts.stream()
                .filter(Objects::nonNull)
                .map(d -> new MonthlyComparisonRequisitionDTO.DepartmentRequestDTO(
                        d.getId(), d.getName(),
                        d.getQty() != null ? d.getQty() : 0,
                        d.getBuy() != null ? d.getBuy() : 0))
                .collect(Collectors.toList());
    }
}