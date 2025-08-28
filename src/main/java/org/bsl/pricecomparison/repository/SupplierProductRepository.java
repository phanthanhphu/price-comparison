package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.model.SupplierProduct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SupplierProductRepository extends MongoRepository<SupplierProduct, String> {

    @Query("{ 'itemNo': { $regex: ?0, $options: 'i' } }")
    List<SupplierProduct> findByItemNoContainingIgnoreCase(String keyword);

    List<SupplierProduct> findBySapCodeAndSupplierCode(String sapCode, String supplierCode);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' } }")
    List<SupplierProduct> findBySapCodeContainingIgnoreCase(String sapCode);

    @Query("{ 'itemNo': { $regex: ?0, $options: 'i' }, 'sapCode': { $regex: ?1, $options: 'i' } }")
    List<SupplierProduct> findByItemNoContainingIgnoreCaseAndSapCodeContainingIgnoreCase(String itemNo, String sapCode);

    boolean existsBySupplierCodeAndSapCodeAndPrice(String supplierCode, String sapCode, Double price);

    boolean existsBySupplierCodeAndSapCodeAndPriceAndIdNot(String supplierCode, String sapCode, double price, String id);

    @Query("{ 'sapCode': { $regex: ?0, $options: 'i' }, 'supplierCode': { $regex: ?1, $options: 'i' }, 'itemNo': { $regex: ?2, $options: 'i' }, 'supplierName': { $regex: ?3, $options: 'i' } }")
    Page<SupplierProduct> findBySapCodeContainingIgnoreCaseOrSupplierCodeContainingIgnoreCaseOrItemNoContainingIgnoreCaseOrSupplierNameContainingIgnoreCase(
            String sapCode, String supplierCode, String itemNo, String supplierName, Pageable pageable);
}