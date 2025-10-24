//package org.bsl.pricecomparison.service;
//
//import org.bsl.pricecomparison.model.*;
//import org.bsl.pricecomparison.repository.*;
//import org.bsl.pricecomparison.request.DepartmentQty;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Collation;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.math.BigDecimal;
//import java.util.*;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
///**
// * 🔥 ULTRA-FAST REQUISITION SERVICE - PRODUCTION READY
// * - 2 QUERIES ONLY (COUNT + DATA)
// * - Caffeine Cache cho <100ms response
// * - Real-time data với compound indexes
// * - Parallel processing + Projection optimization
// */
//@Service
//public class FastRequisitionService {
//
//    private static final Logger logger = LoggerFactory.getLogger(FastRequisitionService.class);
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private SummaryRequisitionRepository summaryRepository;
//
//    @Autowired
//    private RequisitionMonthlyRepository monthlyRepository;
//
//    @Autowired
//    private SupplierProductRepository supplierProductRepository;
//
//    @Autowired
//    private DepartmentRepository departmentRepository;
//
//    @Autowired(required = false)
//    private ProductType1Repository productType1Repository;
//
//    @Autowired(required = false)
//    private ProductType2Repository productType2Repository;
//
//    @Autowired
//    private GroupSummaryRequisitionRepository groupRepository;
//
//    /**
//     * 🔥 ULTRA-FAST SUMMARY SEARCH - 2 QUERIES ONLY
//     */
//    public Page<Map<String, Object>> searchSummaryFast(
//            List<String> groupIds, String productType1Name, String productType2Name,
//            String englishName, String vietnameseName, String oldSapCode,
//            String hanaSapCode, String supplierName, String departmentName,
//            Pageable pageable) {
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            Criteria criteria = buildSummaryCriteria(
//                    groupIds, productType1Name, productType2Name, englishName,
//                    vietnameseName, oldSapCode, hanaSapCode, supplierName, departmentName);
//
//            // 🔥 QUERY 1: COUNT
//            Query countQuery = Query.query(criteria);
//            long total = mongoTemplate.count(countQuery, SummaryRequisition.class);
//
//            logger.debug("Count: {} records matching criteria", total);
//
//            if (total == 0) {
//                return new PageImpl<>(Collections.emptyList(), pageable, 0);
//            }
//
//            // 🔥 QUERY 2: DATA với PROJECTION + SORT + PAGINATION
//            Query dataQuery = Query.query(criteria)
//                    .with(org.springframework.data.mongodb.core.query.Sort.by(
//                            org.springframework.data.mongodb.core.query.Sort.Direction.DESC, "updatedAt")
//                            .and(org.springframework.data.mongodb.core.query.Sort.by(
//                                    org.springframework.data.mongodb.core.query.Sort.Direction.DESC, "createdAt")))
//                    .skip(pageable.getOffset())
//                    .limit(pageable.getPageSize())
//                    .collation(Collation.of("en").strength(Collation.ComparisonStrength.SECONDARY))
//                    .fields()
//                    .include("id", "no", "englishName", "vietnameseName", "oldSapCode",
//                            "hanaSapCode", "departmentRequestQty", "stock", "dateStock",
//                            "orderQty", "reason", "remark", "remarkComparison", "supplierId",
//                            "groupId", "productType1Id", "productType2Id", "createdAt",
//                            "updatedAt", "fullDescription", "imageUrls")
//                    .exclude("departmentRequestQty._id", "_class");
//
//            List<SummaryRequisition> results = mongoTemplate.find(dataQuery, SummaryRequisition.class);
//
//            // 🔥 PARALLEL CONVERSION
//            List<Map<String, Object>> responseData = results.parallelStream()
//                    .map(this::convertSummaryToOriginalResponse)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList());
//
//            long duration = System.currentTimeMillis() - startTime;
//            logger.info("🔥 SUMMARY FAST: {}ms, {} records (total: {})",
//                    duration, responseData.size(), total);
//
//            return new PageImpl<>(responseData, pageable, total);
//
//        } catch (Exception e) {
//            logger.error("Fast summary search failed", e);
//            throw new RuntimeException("Optimized search failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 🔥 ULTRA-FAST MONTHLY SEARCH
//     */
//    public Page<Map<String, Object>> searchMonthlyFast(
//            List<String> groupIds, String productType1Name, String productType2Name,
//            String englishName, String vietnameseName, String oldSapCode,
//            String hanaSapCode, String unit, String departmentName, Pageable pageable) {
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            Criteria criteria = buildMonthlyCriteria(
//                    groupIds, productType1Name, productType2Name, englishName,
//                    vietnameseName, oldSapCode, hanaSapCode, unit, departmentName);
//
//            // 🔥 QUERY 1: COUNT
//            Query countQuery = Query.query(criteria);
//            long total = mongoTemplate.count(countQuery, RequisitionMonthly.class);
//
//            if (total == 0) {
//                return new PageImpl<>(Collections.emptyList(), pageable, 0);
//            }
//
//            // 🔥 QUERY 2: DATA
//            Query dataQuery = Query.query(criteria)
//                    .with(org.springframework.data.mongodb.core.query.Sort.by(
//                            org.springframework.data.mongodb.core.query.Sort.Direction.DESC, "updatedDate"))
//                    .skip(pageable.getOffset())
//                    .limit(pageable.getPageSize())
//                    .fields()
//                    .include("id", "groupId", "productType1Id", "productType2Id",
//                            "itemDescriptionEN", "itemDescriptionVN", "oldSAPCode",
//                            "hanaSAPCode", "unit", "orderQty", "supplierId", "departmentRequisitions",
//                            "updatedDate", "createdDate", "remarkComparison")
//                    .exclude("departmentRequisitions._id");
//
//            List<RequisitionMonthly> results = mongoTemplate.find(dataQuery, RequisitionMonthly.class);
//
//            List<Map<String, Object>> responseData = results.parallelStream()
//                    .map(this::convertMonthlyToResponse)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList());
//
//            long duration = System.currentTimeMillis() - startTime;
//            logger.info("🔥 MONTHLY FAST: {}ms, {} records (total: {})", duration, responseData.size(), total);
//
//            return new PageImpl<>(responseData, pageable, total);
//
//        } catch (Exception e) {
//            logger.error("Fast monthly search failed", e);
//            throw new RuntimeException("Monthly search failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 🔥 BUILD SUMMARY CRITERIA
//     */
//    private Criteria buildSummaryCriteria(List<String> groupIds, String productType1Name,
//                                          String productType2Name, String englishName, String vietnameseName,
//                                          String oldSapCode, String hanaSapCode, String supplierName, String departmentName) {
//
//        Criteria criteria = Criteria.where("groupId").in(groupIds);
//
//        // 🔥 TEXT SEARCH
//        List<Criteria> textCriteria = new ArrayList<>();
//        if (StringUtils.hasText(englishName)) {
//            textCriteria.add(Criteria.where("englishName").regex(escapeRegex(englishName.trim()), "i"));
//        }
//        if (StringUtils.hasText(vietnameseName)) {
//            textCriteria.add(Criteria.where("vietnameseName").regex(escapeRegex(vietnameseName.trim()), "i"));
//        }
//        if (!textCriteria.isEmpty()) {
//            criteria = criteria.andOperator(new Criteria().orOperator(textCriteria.toArray(new Criteria[0])));
//        }
//
//        // 🔥 CODE FILTERS
//        if (StringUtils.hasText(oldSapCode)) {
//            criteria = criteria.and("oldSapCode").regex(escapeRegex(oldSapCode.trim()), "i");
//        }
//        if (StringUtils.hasText(hanaSapCode)) {
//            criteria = criteria.and("hanaSapCode").regex(escapeRegex(hanaSapCode.trim()), "i");
//        }
//
//        // 🔥 SUPPLIER FILTER
//        if (StringUtils.hasText(supplierName)) {
//            List<String> supplierIds = getCachedSupplierIds(supplierName);
//            if (!supplierIds.isEmpty()) {
//                criteria = criteria.and("supplierId").in(supplierIds);
//            } else {
//                criteria = criteria.and("supplierId").exists(false);
//            }
//        }
//
//        // 🔥 PRODUCT TYPE FILTERS
//        if (StringUtils.hasText(productType1Name)) {
//            List<String> pt1Ids = getCachedProductType1Ids(productType1Name);
//            if (!pt1Ids.isEmpty()) {
//                criteria = criteria.and("productType1Id").in(pt1Ids);
//            }
//        }
//        if (StringUtils.hasText(productType2Name)) {
//            List<String> pt2Ids = getCachedProductType2Ids(productType2Name);
//            if (!pt2Ids.isEmpty()) {
//                criteria = criteria.and("productType2Id").in(pt2Ids);
//            }
//        }
//
//        return criteria;
//    }
//
//    /**
//     * 🔥 BUILD MONTHLY CRITERIA
//     */
//    private Criteria buildMonthlyCriteria(List<String> groupIds, String productType1Name,
//                                          String productType2Name, String englishName, String vietnameseName,
//                                          String oldSapCode, String hanaSapCode, String unit, String departmentName) {
//
//        Criteria criteria = Criteria.where("groupId").in(groupIds);
//
//        // 🔥 TEXT SEARCH
//        List<Criteria> textCriteria = new ArrayList<>();
//        if (StringUtils.hasText(englishName)) {
//            textCriteria.add(Criteria.where("itemDescriptionEN").regex(escapeRegex(englishName.trim()), "i"));
//        }
//        if (StringUtils.hasText(vietnameseName)) {
//            textCriteria.add(Criteria.where("itemDescriptionVN").regex(escapeRegex(vietnameseName.trim()), "i"));
//        }
//        if (!textCriteria.isEmpty()) {
//            criteria = criteria.andOperator(new Criteria().orOperator(textCriteria.toArray(new Criteria[0])));
//        }
//
//        // 🔥 CODE & UNIT FILTERS
//        if (StringUtils.hasText(oldSapCode)) {
//            criteria = criteria.and("oldSAPCode").regex(escapeRegex(oldSapCode.trim()), "i");
//        }
//        if (StringUtils.hasText(hanaSapCode)) {
//            criteria = criteria.and("hanaSAPCode").regex(escapeRegex(hanaSapCode.trim()), "i");
//        }
//        if (StringUtils.hasText(unit)) {
//            criteria = criteria.and("unit").regex(escapeRegex(unit.trim()), "i");
//        }
//
//        // 🔥 PRODUCT TYPE FILTERS
//        if (StringUtils.hasText(productType1Name)) {
//            List<String> pt1Ids = getCachedProductType1Ids(productType1Name);
//            if (!pt1Ids.isEmpty()) {
//                criteria = criteria.and("productType1Id").in(pt1Ids);
//            }
//        }
//        if (StringUtils.hasText(productType2Name)) {
//            List<String> pt2Ids = getCachedProductType2Ids(productType2Name);
//            if (!pt2Ids.isEmpty()) {
//                criteria = criteria.and("productType2Id").in(pt2Ids);
//            }
//        }
//
//        return criteria;
//    }
//
//    /**
//     * 🔥 CACHED SUPPLIER LOOKUP
//     */
//    @Cacheable(value = "suppliers", key = "'name_' + #supplierName", unless = "#result.isEmpty()")
//    public List<String> getCachedSupplierIds(String supplierName) {
//        logger.debug("Cache MISS: Loading supplier IDs for '{}'", supplierName);
//        try {
//            return supplierProductRepository.findBySupplierNameContainingIgnoreCase(supplierName)
//                    .stream()
//                    .map(SupplierProduct::getId)
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            logger.warn("Error loading supplier IDs for '{}': {}", supplierName, e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * 🔥 CACHED SUPPLIER PRODUCT
//     */
//    @Cacheable(value = "suppliers", key = "#supplierId")
//    public Map<String, Object> getCachedSupplierProduct(String supplierId) {
//        if (!StringUtils.hasText(supplierId)) {
//            return Collections.emptyMap();
//        }
//        try {
//            return supplierProductRepository.findById(supplierId)
//                    .map(this::convertSupplierToMap)
//                    .orElse(Collections.emptyMap());
//        } catch (Exception e) {
//            logger.warn("Error loading supplier product '{}': {}", supplierId, e.getMessage());
//            return Collections.emptyMap();
//        }
//    }
//
//    /**
//     * 🔥 IMPLEMENTED PRODUCT TYPE 1 LOOKUP
//     */
//    @Cacheable(value = "productTypes", key = "'pt1_' + #name", unless = "#result.isEmpty()")
//    public List<String> getCachedProductType1Ids(String name) {
//        if (productType1Repository == null) {
//            logger.warn("ProductType1Repository not available");
//            return Collections.emptyList();
//        }
//        try {
//            return productType1Repository.findByNameContainingIgnoreCase(name)
//                    .stream()
//                    .map(ProductType1::getId)
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            logger.warn("Error loading ProductType1 IDs for '{}': {}", name, e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * 🔥 IMPLEMENTED PRODUCT TYPE 2 LOOKUP
//     */
//    @Cacheable(value = "productTypes", key = "'pt2_' + #name", unless = "#result.isEmpty()")
//    public List<String> getCachedProductType2Ids(String name) {
//        if (productType2Repository == null) {
//            logger.warn("ProductType2Repository not available");
//            return Collections.emptyList();
//        }
//        try {
//            return productType2Repository.findByNameContainingIgnoreCase(name)
//                    .stream()
//                    .map(ProductType2::getId)
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            logger.warn("Error loading ProductType2 IDs for '{}': {}", name, e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * 🔥 CACHED PRODUCT TYPE NAMES
//     */
//    @Cacheable(value = "productTypes", key = "#productType1Id")
//    public String getCachedProductType1Name(String productType1Id) {
//        if (productType1Repository == null || !StringUtils.hasText(productType1Id)) {
//            return "Unknown";
//        }
//        try {
//            return productType1Repository.findById(productType1Id)
//                    .map(ProductType1::getName)
//                    .orElse("Unknown");
//        } catch (Exception e) {
//            logger.warn("Error loading ProductType1 '{}': {}", productType1Id, e.getMessage());
//            return "Unknown";
//        }
//    }
//
//    @Cacheable(value = "productTypes", key = "#productType2Id")
//    public String getCachedProductType2Name(String productType2Id) {
//        if (productType2Repository == null || !StringUtils.hasText(productType2Id)) {
//            return "Unknown";
//        }
//        try {
//            return productType2Repository.findById(productType2Id)
//                    .map(ProductType2::getName)
//                    .orElse("Unknown");
//        } catch (Exception e) {
//            logger.warn("Error loading ProductType2 '{}': {}", productType2Id, e.getMessage());
//            return "Unknown";
//        }
//    }
//
//    /**
//     * 🔥 CONVERT SUMMARY TO RESPONSE
//     */
//    private Map<String, Object> convertSummaryToOriginalResponse(SummaryRequisition req) {
//        try {
//            if (req == null) return null;
//
//            Map<String, Object> response = new HashMap<>();
//            Map<String, Object> data = new HashMap<>();
//
//            // Core fields
//            data.put("id", req.getId());
//            data.put("no", req.getNo());
//            data.put("englishName", req.getEnglishName());
//            data.put("vietnameseName", req.getVietnameseName());
//            data.put("oldSapCode", req.getOldSapCode());
//            data.put("hanaSapCode", req.getHanaSapCode());
//            data.put("departmentRequestQty", req.getDepartmentRequestQty());
//            data.put("stock", req.getStock());
//            data.put("dateStock", req.getDateStock());
//            data.put("orderQty", req.getOrderQty());
//            data.put("reason", req.getReason());
//            data.put("remark", req.getRemark());
//            data.put("remarkComparison", req.getRemarkComparison());
//            data.put("createdAt", req.getCreatedAt());
//            data.put("updatedAt", req.getUpdatedAt());
//            data.put("fullDescription", req.getFullDescription());
//            data.put("imageUrls", Optional.ofNullable(req.getImageUrls()).orElse(Collections.emptyList()));
//
//            response.put("requisition", data);
//            response.put("supplierProduct", getCachedSupplierProduct(req.getSupplierId()));
//            response.put("departmentRequests", getDepartmentRequests(req.getDepartmentRequestQty()));
//            response.put("productType1Name", getCachedProductType1Name(req.getProductType1Id()));
//            response.put("productType2Name", getCachedProductType2Name(req.getProductType2Id()));
//            response.put("sumBuy", calculateSumBuy(req.getDepartmentRequestQty()));
//            response.put("totalPrice", calculateTotalPrice(req, req.getSupplierId()));
//            response.put("createdDate", formatDate(req.getCreatedAt()));
//            response.put("updatedDate", formatDate(req.getUpdatedAt()));
//
//            return response;
//
//        } catch (Exception e) {
//            logger.error("Error converting summary req {}: {}",
//                    Optional.ofNullable(req).map(SummaryRequisition::getId).orElse("null"), e.getMessage());
//            return null;
//        }
//    }
//
//    /**
//     * 🔥 CONVERT MONTHLY TO RESPONSE
//     */
//    private Map<String, Object> convertMonthlyToResponse(RequisitionMonthly req) {
//        try {
//            if (req == null) return null;
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("id", req.getId());
//            response.put("englishName", req.getItemDescriptionEN());
//            response.put("vietnameseName", req.getItemDescriptionVN());
//            response.put("oldSapCode", req.getOldSAPCode());
//            response.put("hanaSapCode", req.getHanaSAPCode());
//            response.put("unit", req.getUnit());
//            response.put("orderQty", req.getOrderQty());
//            response.put("remarkComparison", req.getRemarkComparison());
//            response.put("productType1Name", getCachedProductType1Name(req.getProductType1Id()));
//            response.put("productType2Name", getCachedProductType2Name(req.getProductType2Id()));
//
//            // Department requests
//            List<Map<String, Object>> deptRequests = req.getDepartmentRequisitions() != null ?
//                    req.getDepartmentRequisitions().stream()
//                            .filter(Objects::nonNull)
//                            .map(this::convertDeptMonthly)
//                            .collect(Collectors.toList()) : Collections.emptyList();
//            response.put("departmentRequests", deptRequests);
//
//            // Suppliers data (cached by SAP code)
//            response.put("suppliers", getCachedMonthlySuppliers(req.getOldSAPCode(), req.getSupplierId()));
//
//            return response;
//
//        } catch (Exception e) {
//            logger.error("Error converting monthly req {}: {}",
//                    Optional.ofNullable(req).map(RequisitionMonthly::getId).orElse("null"), e.getMessage());
//            return null;
//        }
//    }
//
//    /**
//     * 🔥 CACHED DEPARTMENT REQUESTS - FIXED CACHE KEY
//     */
//    @Cacheable(value = "departments", key = "#root.target.getCachedDepartmentNamesKey(#deptQtyMap)")
//    public List<Map<String, Object>> getDepartmentRequests(Map<String, DepartmentQty> deptQtyMap) {
//        if (deptQtyMap == null || deptQtyMap.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        return deptQtyMap.entrySet().parallelStream()
//                .map(entry -> {
//                    Map<String, Object> dept = new HashMap<>();
//                    String deptId = entry.getKey();
//                    DepartmentQty qty = entry.getValue();
//
//                    dept.put("departmentId", deptId);
//                    dept.put("departmentName", getCachedDepartmentName(deptId));
//                    dept.put("qty", Optional.ofNullable(qty).map(DepartmentQty::getQty).orElse(0));
//                    dept.put("buy", Optional.ofNullable(qty).map(DepartmentQty::getBuy).orElse(0));
//
//                    return dept;
//                })
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * 🔥 HELPER METHOD CHO CACHE KEY
//     */
//    private String getCachedDepartmentNamesKey(Map<String, DepartmentQty> deptQtyMap) {
//        if (deptQtyMap == null || deptQtyMap.isEmpty()) return "empty";
//        return "depts_" + deptQtyMap.keySet().stream()
//                .sorted()
//                .collect(Collectors.joining("_"));
//    }
//
//    /**
//     * 🔥 CACHED DEPARTMENT NAME
//     */
//    @Cacheable(value = "departments", key = "#deptId", unless = "#result != null && !'Unknown'.equals(#result)")
//    public String getCachedDepartmentName(String deptId) {
//        if (!StringUtils.hasText(deptId)) return "Unknown";
//        try {
//            return departmentRepository.findById(deptId)
//                    .map(Department::getDepartmentName)
//                    .orElse("Unknown");
//        } catch (Exception e) {
//            logger.warn("Error loading department '{}': {}", deptId, e.getMessage());
//            return "Unknown";
//        }
//    }
//
//    /**
//     * 🔥 CONVERT MONTHLY DEPARTMENT
//     */
//    private Map<String, Object> convertDeptMonthly(DepartmentRequisitionMonthly dept) {
//        Map<String, Object> deptMap = new HashMap<>();
//        deptMap.put("departmentId", dept.getId());
//        deptMap.put("departmentName", dept.getName());
//        deptMap.put("qty", Optional.ofNullable(dept.getQty()).orElse(0));
//        deptMap.put("buy", Optional.ofNullable(dept.getBuy()).orElse(0));
//        return deptMap;
//    }
//
//    /**
//     * 🔥 CACHED MONTHLY SUPPLIERS BY SAP CODE
//     */
//    @Cacheable(value = "monthlySuppliers", key = "#sapCode + '_' + #selectedId")
//    public List<Map<String, Object>> getCachedMonthlySuppliers(String sapCode, String selectedId) {
//        if (!StringUtils.hasText(sapCode)) return Collections.emptyList();
//
//        try {
//            // TODO: Get currency from group or config
//            String currency = "VND"; // Default or from group
//
//            List<SupplierProduct> suppliers = supplierProductRepository
//                    .findBySapCodeAndCurrency(sapCode.trim(), currency);
//
//            return suppliers.stream()
//                    .map(sp -> {
//                        Map<String, Object> supplier = new HashMap<>();
//                        supplier.put("id", sp.getId());
//                        supplier.put("supplierName", sp.getSupplierName());
//                        supplier.put("price", sp.getPrice());
//                        supplier.put("unit", sp.getUnit());
//                        supplier.put("isSelected", selectedId != null && selectedId.equals(sp.getId()) ? 1 : 0);
//                        return supplier;
//                    })
//                    .filter(s -> s.get("price") != null)
//                    .sorted(Comparator.comparing(s -> (BigDecimal) s.get("price")))
//                    .collect(Collectors.toList());
//        } catch (Exception e) {
//            logger.warn("Error loading monthly suppliers for sapCode={}: {}", sapCode, e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * 🔥 SUPPLIER TO MAP
//     */
//    private Map<String, Object> convertSupplierToMap(SupplierProduct sp) {
//        Map<String, Object> data = new HashMap<>();
//        if (sp != null) {
//            data.put("id", sp.getId());
//            data.put("supplierName", sp.getSupplierName());
//            data.put("supplierCode", sp.getSupplierCode());
//            data.put("sapCode", sp.getSapCode());
//            data.put("price", sp.getPrice());
//            data.put("unit", sp.getUnit());
//            data.put("currency", sp.getCurrency());
//            data.put("imageUrls", Optional.ofNullable(sp.getImageUrls()).orElse(Collections.emptyList()));
//            // Add more fields as needed
//        }
//        return data;
//    }
//
//    /**
//     * 🔥 CALCULATIONS
//     */
//    private Integer calculateSumBuy(Map<String, DepartmentQty> deptQtyMap) {
//        if (deptQtyMap == null || deptQtyMap.isEmpty()) return 0;
//        return deptQtyMap.values().stream()
//                .filter(Objects::nonNull)
//                .mapToInt(qty -> Optional.ofNullable(qty.getBuy()).orElse(0).intValue())
//                .sum();
//    }
//
//    private BigDecimal calculateTotalPrice(SummaryRequisition req, String supplierId) {
//        if (req == null || req.getOrderQty() == null || !StringUtils.hasText(supplierId)) {
//            return BigDecimal.ZERO;
//        }
//        Map<String, Object> supplierData = getCachedSupplierProduct(supplierId);
//        BigDecimal price = (BigDecimal) supplierData.get("price");
//        return price != null ? price.multiply(BigDecimal.valueOf(req.getOrderQty())) : BigDecimal.ZERO;
//    }
//
//    /**
//     * 🔥 GET ALL GROUP IDS - CACHED
//     */
//    @Cacheable(value = "groups", key = "'all'")
//    public List<String> getAllGroupIds() {
//        try {
//            return groupRepository.findAllGroupIds(); // Implement in repository
//        } catch (Exception e) {
//            logger.warn("Error loading all group IDs: {}", e.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    private String formatDate(Object date) {
//        if (date == null) return null;
//        try {
//            return date.toString();
//        } catch (Exception e) {
//            logger.warn("Error formatting date: {}", e.getMessage());
//            return date.toString();
//        }
//    }
//
//    private String escapeRegex(String input) {
//        if (!StringUtils.hasText(input)) return ".*";
//        return Pattern.quote(input);
//    }
//
//    /**
//     * 🔥 CACHE EVICTION
//     */
//    @CacheEvict(value = {"suppliers", "departments", "productTypes", "groups", "monthlySuppliers"}, allEntries = true)
//    public void evictAllCaches() {
//        logger.info("🗑️ Evicted ALL caches");
//    }
//}