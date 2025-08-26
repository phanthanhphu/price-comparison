package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierProductRepository extends MongoRepository<SupplierProduct, String> {

    @Query("{ 'productFullName': { $regex: ?0, $options: 'i' } }")
    List<SupplierProduct> findByProductFullNameContainingIgnoreCase(String keyword);

    List<SupplierProduct> findBySapCodeAndSupplierCode(String sapCode, String supplierCode);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' } }")
    List<SupplierProduct> findBySapCodeContainingIgnoreCase(String sapCode);

    @Query("{ 'productFullName': { $regex: ?0, $options: 'i' }, 'sapCode': { $regex: ?1, $options: 'i' } }")
    List<SupplierProduct> findByProductFullNameContainingIgnoreCaseAndSapCodeContainingIgnoreCase(String productFullName, String sapCode);

    boolean existsBySupplierCodeAndSapCodeAndPrice(String supplierCode, String sapCode, Double price);

    boolean existsBySupplierCodeAndSapCodeAndPriceAndIdNot(String supplierCode, String sapCode, double price, String id);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' }, 'supplierCode': { $regex: ?1, $options: 'i' }, 'productFullName': { $regex: ?2, $options: 'i' }, 'supplierName': { $regex: ?3, $options: 'i' } }")
    Page<SupplierProduct> findBySapCodeContainingIgnoreCaseOrSupplierCodeContainingIgnoreCaseOrProductFullNameContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
            String sapCode, String supplierCode, String productFullName, String supplierName, Pageable pageable);

    @Query("{" +
            "  $and: [" +
            "    { 'supplierCode': { $regex: ?0, $options: 'i' } }," +
            "    { 'supplierName': { $regex: ?1, $options: 'i' } }," +
            "    { 'sapCode': { $regex: ?2, $options: 'i' } }," +
            "    { 'productFullName': { $regex: ?3, $options: 'i' } }," +
            "    { 'productShortName': { $regex: ?4, $options: 'i' } }," +
            "    { 'productType1Id': { $regex: ?5, $options: 'i' } }," +
            "    { 'productType2Id': { $regex: ?6, $options: 'i' } }" +
            "  ]" +
            "}")
    Page<SupplierProduct> filterSupplierProducts(
            String supplierCode,
            String supplierName,
            String sapCode,
            String productFullName,
            String productShortName,
            String productType1Id,
            String productType2Id,
            Pageable pageable);

}
