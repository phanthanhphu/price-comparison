package org.bsl.pricecomparison.impl;

import org.bsl.pricecomparison.costom.SupplierProductRepositoryCustom;
import org.bsl.pricecomparison.model.SupplierProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SupplierProductRepositoryImpl implements SupplierProductRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<SupplierProduct> filterSupplierProducts(
            String supplierCode,
            String supplierName,
            String sapCode,
            String itemNo,
            String itemDescription,
            String fullDescription,
            String currency,
            String goodType,
            String productType1Id,
            String productType2Id,
            Pageable pageable) {

        Criteria criteria = new Criteria();

        // Tạo list điều kiện
        List<Criteria> criteriaList = new ArrayList<>();

        if (supplierCode != null && !supplierCode.isEmpty()) {
            criteriaList.add(Criteria.where("supplierCode").regex(supplierCode, "i"));
        }
        if (supplierName != null && !supplierName.isEmpty()) {
            criteriaList.add(Criteria.where("supplierName").regex(supplierName, "i"));
        }
        if (sapCode != null && !sapCode.isEmpty()) {
            criteriaList.add(Criteria.where("sapCode").regex(sapCode, "i"));
        }
        if (itemNo != null && !itemNo.isEmpty()) {
            criteriaList.add(Criteria.where("itemNo").regex(itemNo, "i"));
        }
        if (itemDescription != null && !itemDescription.isEmpty()) {
            criteriaList.add(Criteria.where("itemDescription").regex(itemDescription, "i"));
        }
        if (fullDescription != null && !fullDescription.isEmpty()) {
            criteriaList.add(Criteria.where("fullDescription").regex(fullDescription, "i"));
        }
        if (currency != null && !currency.isEmpty()) {
            criteriaList.add(Criteria.where("currency").is(currency));
        }
        if (goodType != null && !goodType.isEmpty()) {
            criteriaList.add(Criteria.where("goodType").is(goodType));
        }
        if (productType1Id != null && !productType1Id.isEmpty()) {
            criteriaList.add(Criteria.where("productType1Id").is(productType1Id));
        }
        if (productType2Id != null && !productType2Id.isEmpty()) {
            criteriaList.add(Criteria.where("productType2Id").is(productType2Id));
        }

        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        long count = mongoTemplate.count(query, SupplierProduct.class);
        query.with(pageable);

        List<SupplierProduct> list = mongoTemplate.find(query, SupplierProduct.class);

        return new PageImpl<>(list, pageable, count);
    }

    @Override
    public Page<SupplierProduct> findBySapCodeWithPagination(String sapCode, String itemNo, String itemDescription, String currency, Pageable pageable) {
        Criteria criteria = new Criteria();

        List<Criteria> criteriaList = new ArrayList<>();

        if (sapCode != null && !sapCode.isEmpty()) {
            criteriaList.add(Criteria.where("sapCode").regex(sapCode, "i"));
        }
        if (itemNo != null && !itemNo.isEmpty()) {
            criteriaList.add(Criteria.where("itemNo").regex(itemNo, "i"));
        }
        if (itemDescription != null && !itemDescription.isEmpty()) {
            criteriaList.add(Criteria.where("itemDescription").regex(itemDescription, "i"));
        }
        if (currency != null && !currency.isEmpty()) {
            criteriaList.add(Criteria.where("currency").regex(currency, "i"));
        }

        // If no filter parameters are provided, don't apply any criteria (return all)
        if (!criteriaList.isEmpty()) {
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        Query query = new Query(criteria);
        long count = mongoTemplate.count(query, SupplierProduct.class);
        query.with(pageable);

        List<SupplierProduct> list = mongoTemplate.find(query, SupplierProduct.class);

        return new PageImpl<>(list, pageable, count);
    }
}