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

    @Query("{ 'sapCode': { $regex: '^?0$', $options: 'i' }, 'currency': { $regex: '^?1$', $options: 'i' } }")
    List<SupplierProduct> findBySapCodeAndCurrencyIgnoreCase(String sapCode, String currency);

    Optional<SupplierProduct> findById(String id);

    @Query("{ 'itemNo': { $regex: ?0, $options: 'i' }, 'currency': { $regex: '^?1$', $options: 'i' } }")
    List<SupplierProduct> findByItemNoContainingIgnoreCaseAndCurrency(String itemNo, String currency);

    @Query(value = "{'supplierCode': ?0, 'sapCode': ?1, 'currency': ?2, 'price': ?3}", exists = true)
    boolean existsBySupplierCodeAndSapCodeAndCurrencyAndPrice(String supplierCode, String sapCode, String currency, BigDecimal price);

    @Query(value = "{'supplierCode': ?0, 'hanaSapCode': ?1, 'currency': ?2, 'price': ?3}", exists = true)
    boolean existsBySupplierCodeAndHanaSapCodeAndCurrencyAndPrice(String supplierCode, String hanaSapCode, String currency, BigDecimal price);

    @Query(value = "{" +
            "  'supplierName': ?0," +
            "  'currency': ?1," +
            "  'price': ?2," +
            "  $and: [" +
            "    { $or: [" +
            "        { 'sapCode': { $in: [null, ''] } }," +
            "        { 'sapCode': { $regex: '^NEW$', $options: 'i' } }" +
            "    ] }," +
            "    { $or: [" +
            "        { 'hanaSapCode': { $in: [null, ''] } }," +
            "        { 'hanaSapCode': { $regex: '^NEW$', $options: 'i' } }" +
            "    ] }" +
            "  ]" +
            "}", exists = true)
    boolean existsFallbackBySupplierNameAndCurrencyAndPriceWhenCodesEmptyOrNew(
            String supplierName, String currency, BigDecimal price
    );

    // 1) sapCode + unit + currency
    List<SupplierProduct> findBySapCodeIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(
            String sapCode, String unit, String currency
    );

    // 2) hanaSapCode + unit + currency
    List<SupplierProduct> findByHanaSapCodeIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(
            String hanaSapCode, String unit, String currency
    );

    // 3) itemDescriptionVN contains + unit + currency
    List<SupplierProduct> findByItemDescriptionVNContainingIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(
            String keyword, String unit, String currency
    );

    // 4) itemDescriptionEN contains + unit + currency
    List<SupplierProduct> findByItemDescriptionENContainingIgnoreCaseAndUnitIgnoreCaseAndCurrencyIgnoreCase(
            String keyword, String unit, String currency
    );

}