package org.bsl.pricecomparison.costom;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierProductRepositoryCustom {
    Page<SupplierProduct> filterSupplierProducts(
            String supplierCode,
            String supplierName,
            String sapCode,
            String hanaSapCode,
            String itemDescriptionEN,
            String itemDescriptionVN,
            String currency,
            String goodType,
            String productType1Id,
            String productType2Id,
            Pageable pageable
    );

    Page<SupplierProduct> findBySapCodeWithPagination(String sapCode, String itemNo, String itemDescription, String currency, Pageable pageable);
}
