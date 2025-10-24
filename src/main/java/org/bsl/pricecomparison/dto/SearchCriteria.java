//package org.bsl.pricecomparison.dto;
//
//import lombok.*;
//import org.springframework.data.domain.Pageable;
//import org.bsl.pricecomparison.controller.RequisitionSearchController;
//
//import java.util.List;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class SearchCriteria {
//    private List<String> groupIds;
//    private String productType1Name;
//    private String productType2Name;
//    private String englishName;
//    private String vietnameseName;
//    private String oldSapCode;
//    private String hanaSapCode;
//    private String supplierName;
//    private String unit;
//    private String departmentName;
//    private boolean hasFilter;
//    private boolean disablePagination;
//    private RequisitionSearchController.DataType dataType;
//    private Pageable pageable;
//
//    // Helper methods
//    public Pageable getEffectivePageable() {
//        if (disablePagination || pageable == null) {
//            return Pageable.unpaged(); // Hoặc custom Pageable với max size
//        }
//        return pageable;
//    }
//
//    public boolean hasAnyFilter() {
//        return hasFilter ||
//                (groupIds != null && !groupIds.isEmpty()) ||
//                isNotEmpty(productType1Name) ||
//                isNotEmpty(productType2Name) ||
//                isNotEmpty(englishName) ||
//                isNotEmpty(vietnameseName) ||
//                isNotEmpty(oldSapCode) ||
//                isNotEmpty(hanaSapCode) ||
//                isNotEmpty(supplierName) ||
//                isNotEmpty(unit) ||
//                isNotEmpty(departmentName);
//    }
//
//    private boolean isNotEmpty(String str) {
//        return str != null && !str.trim().isEmpty();
//    }
//}