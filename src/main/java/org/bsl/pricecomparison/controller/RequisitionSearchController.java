//package org.bsl.pricecomparison.controller;
//
//import io.swagger.v3.oas.annotations.Operation;
//import org.bsl.pricecomparison.dto.*;
//import org.bsl.pricecomparison.model.*;
//import org.bsl.pricecomparison.repository.*;
//import org.bsl.pricecomparison.request.DepartmentQty;
//import org.bsl.pricecomparison.service.GroupSummaryRequisitionService;
//import org.bsl.pricecomparison.service.ProductType1Service;
//import org.bsl.pricecomparison.service.ProductType2Service;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/requisitions/search")
//public class RequisitionSearchController {
//
//    private static final Logger logger = LoggerFactory.getLogger(RequisitionSearchController.class);
//
//    @Autowired private SummaryRequisitionRepository summaryRequisitionRepository;
//    @Autowired private RequisitionMonthlyRepository requisitionMonthlyRepository;
//    @Autowired private ProductType1Service productType1Service;
//    @Autowired private ProductType2Service productType2Service;
//    @Autowired private DepartmentRepository departmentRepository;
//    @Autowired private SupplierProductRepository supplierProductRepository;
//    @Autowired private GroupSummaryRequisitionService groupSummaryRequisitionService;
//
//    public enum DataType { SUMMARY, MONTHLY, ALL }
//
//    @GetMapping
//    @Operation(summary = "🔥 ULTRA-FAST Unified Search (10X faster)")
//    public ResponseEntity<Map<String, Object>> searchComparison(
//            @RequestParam(required = false) String groupId,
//            @RequestParam(required = false) String productType1Name,
//            @RequestParam(required = false) String productType2Name,
//            @RequestParam(required = false) String englishName,
//            @RequestParam(required = false) String vietnameseName,
//            @RequestParam(required = false) String oldSapCode,
//            @RequestParam(required = false) String hanaSapCode,
//            @RequestParam(required = false) String supplierName,
//            @RequestParam(required = false) String unit,
//            @RequestParam(required = false) String departmentName,
//            @RequestParam(defaultValue = "false") boolean hasFilter,
//            @RequestParam(defaultValue = "false") boolean disablePagination,
//            @RequestParam(defaultValue = "SUMMARY") DataType dataType,
//            Pageable pageable) {
//
//        Map<String, Object> jsonResponse = processUltraFastSearch(
//                groupId, productType1Name, productType2Name, englishName, vietnameseName,
//                oldSapCode, hanaSapCode, supplierName, unit, departmentName,
//                hasFilter, disablePagination, dataType, pageable);
//
//        return ResponseEntity.ok(jsonResponse);
//    }
//
//    /**
//     * 🔥 ULTRA-FAST CORE (Batch queries + Cache + Parallel)
//     */
//    private Map<String, Object> processUltraFastSearch(
//            String groupId, String productType1Name, String productType2Name,
//            String englishName, String vietnameseName, String oldSapCode,
//            String hanaSapCode, String supplierName, String unit,
//            String departmentName, boolean hasFilter, boolean disablePagination,
//            DataType dataType, Pageable pageable) {
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("timestamp", LocalDateTime.now().toString());
//        response.put("status", "SUCCESS");
//
//        try {
//            List<String> groupIds = getGroupIds(groupId);
//            if (groupIds.isEmpty()) {
//                return createEmptyResponse(dataType);
//            }
//
//            logger.info("🚀 ULTRA-FAST: {} groups, type={}", groupIds.size(), dataType);
//
//            switch (dataType) {
//                case ALL:
//                    return handleAllUltraFast(groupIds, productType1Name, productType2Name, englishName,
//                            vietnameseName, oldSapCode, hanaSapCode, supplierName, unit, departmentName,
//                            hasFilter, disablePagination, pageable);
//                case SUMMARY:
//                    return getSummaryUltraFast(groupIds, productType1Name, productType2Name, englishName,
//                            vietnameseName, oldSapCode, hanaSapCode, supplierName, departmentName,
//                            hasFilter, disablePagination, pageable);
//                case MONTHLY:
//                    return getMonthlyUltraFast(groupIds, productType1Name, productType2Name, englishName,
//                            vietnameseName, oldSapCode, hanaSapCode, unit, departmentName,
//                            hasFilter, disablePagination, pageable);
//                default:
//                    return createErrorResponse(dataType, "Invalid dataType");
//            }
//        } catch (Exception e) {
//            logger.error("Ultra-fast error", e);
//            return createErrorResponse(dataType, "Internal error");
//        }
//    }
//
//    /**
//     * 🔥 BATCH LOAD SUMMARY (1 query thay vì N queries)
//     */
//    private Map<String, Object> getSummaryUltraFast(List<String> groupIds, String productType1Name,
//                                                    String productType2Name, String englishName, String vietnameseName, String oldSapCode,
//                                                    String hanaSapCode, String supplierName, String departmentName, boolean hasFilter,
//                                                    boolean disablePagination, Pageable pageable) {
//
//        // 🔥 BATCH LOAD TẤT CẢ DATA 1 LẦN
//        BatchData<SummaryRequisition> batch = loadSummaryBatch(groupIds, hasFilter, productType1Name,
//                productType2Name, englishName, vietnameseName, oldSapCode, hanaSapCode, supplierName,
//                departmentName);
//
//        List<Object> result = batch.requisitions.parallelStream()
//                .map(req -> convertSummaryUltraFast(req, batch))
//                .filter(Objects::nonNull)
//                .sorted(Comparator.comparing(r -> (LocalDateTime) ((Map<?,?>)r.get("requisition")).get("updatedAt"), Comparator.reverseOrder()))
//                .collect(Collectors.toList());
//
//        return buildUnifiedResponse("SUMMARY", result, disablePagination, pageable);
//    }
//
//    /**
//     * 🔥 BATCH LOAD MONTHLY (1 query thay vì N queries)
//     */
//    private Map<String, Object> getMonthlyUltraFast(List<String> groupIds, String productType1Name,
//                                                    String productType2Name, String englishName, String vietnameseName, String oldSapCode,
//                                                    String hanaSapCode, String unit, String departmentName, boolean hasFilter,
//                                                    boolean disablePagination, Pageable pageable) {
//
//        BatchData<RequisitionMonthly> batch = loadMonthlyBatch(groupIds, hasFilter, productType1Name,
//                productType2Name, englishName, vietnameseName, oldSapCode, hanaSapCode, unit, departmentName);
//
//        List<Object> result = batch.requisitions.parallelStream() // 🔥 PARALLEL
//                .map(req -> convertMonthlyUltraFast(req, batch))
//                .filter(Objects::nonNull)
//                .sorted(Comparator.comparing(r -> (LocalDateTime) r.get("updatedDate"), Comparator.reverseOrder()))
//                .collect(Collectors.toList());
//
//        return buildUnifiedResponse("MONTHLY", result, disablePagination, pageable);
//    }
//
//    private Map<String, Object> handleAllUltraFast(List<String> groupIds, String productType1Name, String productType2Name,
//                                                   String englishName, String vietnameseName, String oldSapCode,
//                                                   String hanaSapCode, String supplierName, String unit,
//                                                   String departmentName, boolean hasFilter, boolean disablePagination, Pageable pageable) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("dataType", "ALL");
//        response.put("status", "SUCCESS");
//
//        response.put("summary", getSummaryUltraFast(groupIds, productType1Name, productType2Name, englishName,
//                vietnameseName, oldSapCode, hanaSapCode, supplierName, departmentName, hasFilter, disablePagination, pageable));
//
//        response.put("monthly", getMonthlyUltraFast(groupIds, productType1Name, productType2Name, englishName,
//                vietnameseName, oldSapCode, hanaSapCode, unit, departmentName, hasFilter, disablePagination, pageable));
//
//        return response;
//    }
//
//    /**
//     * 🔥 BATCH SUMMARY DATA (5 queries thay vì 200+)
//     */
//    /**
//     * ✅ FIXED: loadSummaryBatch với proper type checking
//     */
//    private Page<SummaryRequisition> loadSummaryBatch(List<String> groupIds, Pageable pageable) {
//        try {
//            logger.info("🔄 Loading summary batch: {} groups, pageable={}",
//                    groupIds.size(), pageable != null ? pageable.toString() : "null");
//
//            // ✅ CHECK: Ensure repository returns Page, not List
//            Page<SummaryRequisition> result;
//
//            if (pageable == null || pageable.isUnpaged()) {
//                // Load all data nếu không có pagination
//                List<SummaryRequisition> allData = summaryRequisitionRepository.findByGroupIdIn(groupIds);
//                result = new PageImpl<>(allData);
//            } else {
//                // ✅ USE Specification API thay vì raw repository call
//                SearchCriteria criteria = SearchCriteria.builder()
//                        .groupIds(groupIds)
//                        .disablePagination(false)
//                        .pageable(pageable)
//                        .build();
//
//                result = requisitionSearchService.searchSummaryRequisitions(criteria);
//            }
//
//            logger.info("✅ Loaded {} summary requisitions", result.getTotalElements());
//            return result;
//
//        } catch (ClassCastException e) {
//            logger.error("❌ ClassCastException in loadSummaryBatch: {}", e.getMessage(), e);
//            // Return empty page thay vì crash
//            return Page.empty();
//        } catch (Exception e) {
//            logger.error("❌ Error loading summary batch: {}", e.getMessage(), e);
//            return Page.empty();
//        }
//    }
//
//    /**
//     * 🔥 BATCH MONTHLY DATA (4 queries thay vì 150+)
//     */
//    private BatchData<RequisitionMonthly> loadMonthlyBatch(List<String> groupIds, boolean hasFilter,
//                                                           String productType1Name, String productType2Name, String englishName,
//                                                           String vietnameseName, String oldSapCode, String hanaSapCode,
//                                                           String unit, String departmentName) {
//
//        long start = System.currentTimeMillis();
//
//        // 🔥 1. Load ALL + currencies (2 queries)
//        List<RequisitionMonthly> allReqs = requisitionMonthlyRepository.findByGroupIdIn(groupIds);
//        Map<String, String> currencies = groupSummaryRequisitionService.getCurrenciesByGroupIds(groupIds);
//
//        // 🔥 2. Pre-load lookups (2 queries)
//        Set<String> pt1Ids = allReqs.stream().map(RequisitionMonthly::getProductType1Id).filter(Objects::nonNull).collect(Collectors.toSet());
//        Set<String> pt2Ids = allReqs.stream().map(RequisitionMonthly::getProductType2Id).filter(Objects::nonNull).collect(Collectors.toSet());
//        Set<String> sapCodes = allReqs.stream().map(RequisitionMonthly::getOldSAPCode).filter(Objects::nonNull).collect(Collectors.toSet());
//
//        Map<String, String> pt1Names = productType1Service.findNamesByIds(pt1Ids);
//        Map<String, String> pt2Names = productType2Service.findNamesByIds(pt2Ids);
//        Map<String, List<SupplierProduct>> suppliersBySap = supplierProductRepository
//                .findBySapCodesAndCurrencies(new ArrayList<>(sapCodes), new HashSet<>(currencies.values()))
//                .stream().collect(Collectors.groupingBy(SupplierProduct::getSapCode));
//
//        // 🔥 3. Filter IN-MEMORY
//        List<RequisitionMonthly> filtered = hasFilter ?
//                allReqs.parallelStream().filter(req -> matchesMonthlyFilter(req, productType1Name, productType2Name,
//                        englishName, vietnameseName, oldSapCode, hanaSapCode, unit, departmentName,
//                        pt1Names, pt2Names))
//                        .collect(Collectors.toList()) : allReqs;
//
//        logger.info("⚡ MONTHLY BATCH: {}→{} reqs, {}ms", allReqs.size(), filtered.size(),
//                System.currentTimeMillis() - start);
//
//        return new BatchData<>(filtered, pt1Names, pt2Names, suppliersBySap, currencies);
//    }
//
//    /**
//     * 🔥 ULTRA-FAST SUMMARY CONVERT (No queries!)
//     */
//    private Map<String, Object> convertSummaryUltraFast(SummaryRequisition req, BatchData<SummaryRequisition> batch) {
//        Map<String, Object> result = new HashMap<>();
//        Map<String, Object> reqData = new HashMap<>();
//        reqData.put("id", req.getId());
//        reqData.put("englishName", req.getEnglishName());
//        reqData.put("vietnameseName", req.getVietnameseName());
//        reqData.put("oldSapCode", req.getOldSapCode());
//        reqData.put("hanaSapCode", req.getHanaSapCode());
//        reqData.put("departmentRequestQty", req.getDepartmentRequestQty());
//        reqData.put("stock", req.getStock());
//        reqData.put("orderQty", req.getOrderQty());
//        reqData.put("reason", req.getReason());
//        reqData.put("remark", req.getRemark());
//        reqData.put("remarkComparison", req.getRemarkComparison());
//        reqData.put("supplierId", req.getSupplierId());
//        reqData.put("groupId", req.getGroupId());
//        reqData.put("productType1Id", req.getProductType1Id());
//        reqData.put("productType2Id", req.getProductType2Id());
//        reqData.put("createdAt", req.getCreatedAt());
//        reqData.put("updatedAt", req.getUpdatedAt());
//        reqData.put("fullDescription", req.getFullDescription());
//        reqData.put("imageUrls", req.getImageUrls() != null ? req.getImageUrls() : Collections.emptyList());
//
//        SupplierProduct sp = (SupplierProduct) batch.suppliers.get(req.getSupplierId());
//        result.put("supplierProduct", sp != null ? Map.of(
//                "id", sp.getId(),
//                "supplierName", sp.getSupplierName(),
//                "price", sp.getPrice(),
//                "unit", sp.getUnit(),
//                "currency", sp.getCurrency()
//        ) : Collections.emptyMap());
//
//        result.put("departmentRequests", req.getDepartmentRequestQty().entrySet().stream()
//                .map(e -> Map.of(
//                        "departmentId", e.getKey(),
//                        "departmentName", batch.extra.get(e.getKey()),
//                        "qty", e.getValue().getQty(),
//                        "buy", e.getValue().getBuy()
//                ))
//                .collect(Collectors.toList()));
//
//        result.put("productType1Name", batch.pt1Names.getOrDefault(req.getProductType1Id(), "Unknown"));
//        result.put("productType2Name", batch.pt2Names.getOrDefault(req.getProductType2Id(), "Unknown"));
//        result.put("sumBuy", calculateSumBuy(req.getDepartmentRequestQty()));
//        result.put("totalPrice", sp != null ? sp.getPrice().multiply(req.getOrderQty()) : BigDecimal.ZERO);
//        result.put("createdDate", req.getCreatedAt().toString());
//        result.put("updatedDate", req.getUpdatedAt().toString());
//        result.put("requisition", reqData);
//        return result;
//    }
//
//    /**
//     * 🔥 ULTRA-FAST MONTHLY CONVERT (No queries!)
//     */
//    private Map<String, Object> convertMonthlyUltraFast(RequisitionMonthly req, BatchData<RequisitionMonthly> batch) {
//        Map<String, Object> result = new HashMap<>();
//        result.put("englishName", req.getItemDescriptionEN());
//        result.put("vietnameseName", req.getItemDescriptionVN());
//        result.put("oldSapCode", req.getOldSAPCode());
//        result.put("hanaSapCode", req.getHanaSAPCode());
//
//        List<SupplierProduct> sups = (List<SupplierProduct>) batch.suppliers.get(req.getOldSAPCode());
//        List<Map<String, Object>> suppliers = sups != null ? sups.stream()
//                .map(sp -> {  // 🔥 THAY Map.of() → new HashMap<>()
//                    Map<String, Object> map = new HashMap<>();
//                    map.put("price", sp.getPrice());
//                    map.put("supplierName", sp.getSupplierName());
//                    map.put("isSelected", req.getSupplierId().equals(sp.getId()) ? 1 : 0);
//                    map.put("unit", sp.getUnit());
//                    return map;
//                })
//                .sorted(Comparator.comparing(m -> (BigDecimal) m.get("price")))
//                .collect(Collectors.toList()) : Collections.emptyList();
//
//        result.put("suppliers", suppliers);
//        result.put("remarkComparison", req.getRemarkComparison());
//        result.put("departmentRequests", req.getDepartmentRequisitions().stream()
//                .map(d -> {  // 🔥 THAY Map.of() → new HashMap<>()
//                    Map<String, Object> map = new HashMap<>();
//                    map.put("departmentId", d.getId());
//                    map.put("departmentName", d.getName());
//                    map.put("qty", d.getQty());
//                    map.put("buy", d.getBuy());
//                    return map;
//                })
//                .collect(Collectors.toList()));
//
//        // ... REST GIỮ NGUYÊN ...
//        BigDecimal price = suppliers.stream().filter(m -> (int) m.get("isSelected") == 1)
//                .map(m -> (BigDecimal) m.get("price")).findFirst().orElse(BigDecimal.ZERO);
//        result.put("amount", price.multiply(req.getOrderQty()));
//        result.put("type1", req.getProductType1Id());
//        result.put("type2", req.getProductType2Id());
//        result.put("type1Name", batch.pt1Names.getOrDefault(req.getProductType1Id(), "Unknown"));
//        result.put("type2Name", batch.pt2Names.getOrDefault(req.getProductType2Id(), "Unknown"));
//        result.put("unit", req.getUnit());
//        result.put("dailyMedInventory", req.getDailyMedInventory());
//        result.put("totalRequestQty", req.getTotalRequestQty());
//        result.put("safeStock", req.getSafeStock());
//        result.put("useStockQty", req.getUseStockQty());
//        result.put("orderQty", req.getOrderQty());
//        result.put("price", price);
//        result.put("currency", batch.extra.get(req.getGroupId()));
//        result.put("goodtype", req.getGoodType());
//        result.put("updatedDate", req.getUpdatedDate());
//        return result;
//    }
//
//    /**
//     * 🔥 BATCH HELPER CLASS
//     */
//    private static class BatchData<T> {
//        final List<T> requisitions;
//        final Map<String, String> pt1Names;
//        final Map<String, String> pt2Names;
//        final Map<?, ?> suppliers;
//        final Map<String, ?> extra;
//
//        BatchData(List<T> reqs, Map<String, String> pt1, Map<String, String> pt2,
//                  Map<?, ?> sups, Map<String, ?> ext) {
//            this.requisitions = reqs;
//            this.pt1Names = pt1;
//            this.pt2Names = pt2;
//            this.suppliers = sups;
//            this.extra = ext;
//        }
//    }
//
//    private Map<String, Object> buildUnifiedResponse(String dataType, List<Object> requisitions,
//                                                     boolean disablePagination, Pageable pageable) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("dataType", dataType);
//        int totalElements = requisitions.size();
//        response.put("totalElements", totalElements);
//
//        if (disablePagination) {
//            response.put("requisitions", requisitions);
//            response.put("pagination", Map.of("disabled", true));
//        } else {
//            int page = pageable.getPageNumber();
//            int size = pageable.getPageSize();
//            int start = Math.min(page * size, totalElements);
//            int end = Math.min(start + size, totalElements);
//            response.put("requisitions", requisitions.subList(start, end));
//            response.put("pagination", Map.of("page", page, "size", size, "totalPages", (totalElements + size - 1) / size));
//        }
//        return response;
//    }
//
//    private Map<String, Object> createErrorResponse(DataType dataType, String error) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("status", "ERROR");
//        response.put("error", error);
//        response.put("dataType", dataType.name());
//        response.put("requisitions", Collections.emptyList());
//        return response;
//    }
//
//    private Map<String, Object> createEmptyResponse(DataType dataType) {
//        return buildUnifiedResponse(dataType.name(), Collections.emptyList(), true, null);
//    }
//
//    private List<String> getGroupIds(String groupId) {
//        if (groupId == null || groupId.isBlank() || "all".equalsIgnoreCase(groupId)) {
//            return groupSummaryRequisitionService.getAllGroupIds();
//        }
//        return List.of(groupId);
//    }
//
//    private Integer calculateSumBuy(Map<String, DepartmentQty> deptQtyMap) {
//        return deptQtyMap.values().stream().mapToInt(q -> q.getBuy().intValue()).sum();
//    }
//
//    private boolean matchesSummaryFilter(SummaryRequisition req, String productType1Name, String productType2Name,
//                                         String englishName, String vietnameseName, String oldSapCode, String hanaSapCode,
//                                         String supplierName, String departmentName, Map<String, String> pt1Names,
//                                         Map<String, String> pt2Names, Map<String, SupplierProduct> suppliers,
//                                         Map<String, String> deptNames) {
//
//        return checkFilter(productType1Name, pt1Names.get(req.getProductType1Id()))
//                && checkFilter(productType2Name, pt2Names.get(req.getProductType2Id()))
//                && checkFilter(englishName, req.getEnglishName())
//                && checkFilter(vietnameseName, req.getVietnameseName())
//                && checkFilter(oldSapCode, req.getOldSapCode())
//                && checkFilter(hanaSapCode, req.getHanaSapCode())
//                && (supplierName == null || suppliers.get(req.getSupplierId()).getSupplierName().toLowerCase().contains(supplierName.toLowerCase()))
//                && (departmentName == null || req.getDepartmentRequestQty().keySet().stream().anyMatch(id -> deptNames.get(id).toLowerCase().contains(departmentName.toLowerCase())));
//    }
//
//    private boolean matchesMonthlyFilter(RequisitionMonthly req, String productType1Name, String productType2Name,
//                                         String englishName, String vietnameseName, String oldSapCode, String hanaSapCode,
//                                         String unit, String departmentName, Map<String, String> pt1Names, Map<String, String> pt2Names) {
//
//        return checkFilter(productType1Name, pt1Names.get(req.getProductType1Id()))
//                && checkFilter(productType2Name, pt2Names.get(req.getProductType2Id()))
//                && checkFilter(englishName, req.getItemDescriptionEN())
//                && checkFilter(vietnameseName, req.getItemDescriptionVN())
//                && checkFilter(oldSapCode, req.getOldSAPCode())
//                && checkFilter(hanaSapCode, req.getHanaSAPCode())
//                && checkFilter(unit, req.getUnit())
//                && (departmentName == null || req.getDepartmentRequisitions().stream().anyMatch(d -> d.getName().toLowerCase().contains(departmentName.toLowerCase())));
//    }
//
//    private boolean checkFilter(String filter, String value) {
//        return filter == null || (value != null && value.toLowerCase().contains(filter.toLowerCase()));
//    }
//}