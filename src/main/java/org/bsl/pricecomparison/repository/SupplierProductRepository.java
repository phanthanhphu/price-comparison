package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SupplierProductRepository extends MongoRepository<SupplierProduct, String> {

    @Query("{ 'itemNo': { $regex: ?0, $options: 'i' } }")
    List<SupplierProduct> findByItemNoContainingIgnoreCase(String keyword);

    List<SupplierProduct> findBySapCodeAndSupplierCode(String sapCode, String supplierCode);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' } }")
    List<SupplierProduct> findBySapCodeContainingIgnoreCase(String sapCode);

    @Query("{ 'itemNo': { $regex: ?0, $options: 'i' }, 'sapCode': { $regex: ?1, $options: 'i' } }")
    List<SupplierProduct> findByItemNoContainingIgnoreCaseAndSapCodeContainingIgnoreCase(String itemNo, String sapCode);

    boolean existsBySupplierCodeAndSapCodeAndPrice(String supplierCode, String sapCode, BigDecimal price);

    boolean existsBySupplierCodeAndSapCodeAndPriceAndIdNot(String supplierCode, String sapCode, BigDecimal price, String id);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' }, 'supplierCode': { $regex: ?1, $options: 'i' }, 'itemNo': { $regex: ?2, $options: 'i' }, 'supplierName': { $regex: ?3, $options: 'i' } }")
    Page<SupplierProduct> findBySapCodeContainingIgnoreCaseOrSupplierCodeContainingIgnoreCaseOrItemNoContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
            String sapCode, String supplierCode, String itemNo, String supplierName, Pageable pageable);

    List<SupplierProduct> findBySapCode(String sapCode);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' }, 'currency': { $regex: ?1, $options: 'i' } }")
    List<SupplierProduct> findBySapCodeAndCurrency(String sapCode, String currency);

    Optional<SupplierProduct> findById(String id);

    // 🔥 CASE-INSENSITIVE SEARCH với MongoDB Text Index
    @Query("{'supplierName': { $regex: ?0, $options: 'i' }}")
    List<SupplierProduct> findBySupplierNameContainingIgnoreCase(String supplierName);

    // 🔥 OPTIMIZED: Find by multiple supplier IDs
    @Query("{'_id': { $in: ?0 }}")
    List<SupplierProduct> findByIds(List<String> ids);


    // 🔥 THÊM VÀO CUỐI SupplierProductRepository.java
    @Query("{ 'sapCode': { $in: ?0 }, 'currency': { $in: ?1 } }")
    List<SupplierProduct> findBySapCodesAndCurrencies(List<String> sapCodes, Set<String> currencies);
}