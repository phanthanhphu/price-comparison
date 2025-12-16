package org.bsl.pricecomparison.repository;

import org.bsl.pricecomparison.dto.PurchaseHistoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseHistoryItemRepository extends MongoRepository<PurchaseHistoryItem, String> {

    List<PurchaseHistoryItem> findByGroupIdAndRequisitionMonthlyIdIn(String groupId, List<String> requisitionMonthlyIds);

    Optional<PurchaseHistoryItem> findTopByGroupIdAndOldSapCodeAndSupplierIdOrderByPurchasedAtDesc(
            String groupId, String oldSapCode, String supplierId
    );

    Optional<PurchaseHistoryItem> findTopByGroupIdAndOldSapCodeOrderByPurchasedAtDesc(
            String groupId, String oldSapCode
    );
}
