package org.bsl.pricecomparison.costom;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierProductRepositoryCustom {
    Page<SupplierProduct> filterSupplierProducts(
            String supplierCode,
            String supplierName,
            String sapCode,
            String itemNo,
            String itemDescription,
            String fullDescription,
            String materialGroupFullDescription,
            String productType1Id,
            String productType2Id,
            Pageable pageable);
}
