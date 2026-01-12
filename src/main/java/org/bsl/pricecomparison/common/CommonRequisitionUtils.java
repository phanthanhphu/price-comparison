package org.bsl.pricecomparison.common;

import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.repository.RequisitionMonthlyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CommonRequisitionUtils {

    private final RequisitionMonthlyRepository requisitionMonthlyRepository;

    public CommonRequisitionUtils(RequisitionMonthlyRepository requisitionMonthlyRepository) {
        this.requisitionMonthlyRepository = requisitionMonthlyRepository;
    }

    public static String normText(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static String safeTrim(String s) {
        return normText(s);
    }

    /** ✅ normCode: null/empty => null, "NEW" ignoreCase => null */
    public static String normCode(String s) {
        String t = normText(s);
        if (t == null) return null;
        return "new".equalsIgnoreCase(t) ? null : t;
    }

    /** "new"/"New"/"NEW" -> "NEW", null/blank -> null, else keep */
    public static String normalizeNewKeepNew(String s) {
        String t = normText(s);
        if (t == null) return null;
        return "new".equalsIgnoreCase(t) ? "NEW" : t;
    }

    /** usable = not null, not blank, not "NEW" (case-insensitive) */
    public static boolean isUsableKey(String s) {
        String t = normText(s);
        if (t == null) return false;
        return !"new".equalsIgnoreCase(t);
    }

    /**
     * ✅ Build unique key for upload/check duplicate:
     * UNIT + (SAP usable -> HANA usable -> VN -> EN)
     *
     * - unit: required (null/blank => null)
     * - SAP/HANA: ignore "NEW" (case-insensitive) and blank
     * - VN/EN: use trimmed lower-case
     *
     * return null if cannot build (unit missing or all keys empty/NEW)
     */
    public static String buildRowKeyWithUnitByPriority(
            String unit,
            String sapCodeNormOrRaw,
            String hanaCodeNormOrRaw,
            String descVN,
            String descEN
    ) {
        String u = normText(unit);
        if (u == null) return null;

        // NOTE: in upload you store "NEW" sometimes.
        // For key building, "NEW" is treated as NOT usable.
        String sap = normText(sapCodeNormOrRaw);
        String hana = normText(hanaCodeNormOrRaw);

        if (isUsableKey(sap)) {
            return "UNIT|" + u.toLowerCase() + "|SAP|" + sap.toUpperCase();
        }
        if (isUsableKey(hana)) {
            return "UNIT|" + u.toLowerCase() + "|HANA|" + hana.toUpperCase();
        }

        String vn = normText(descVN);
        if (vn != null) {
            return "UNIT|" + u.toLowerCase() + "|VN|" + vn.toLowerCase();
        }

        String en = normText(descEN);
        if (en != null) {
            return "UNIT|" + u.toLowerCase() + "|EN|" + en.toLowerCase();
        }

        return null;
    }

    /** Build key from DB record (RequisitionMonthly) using same rule */
    public static String buildExistingKeyFromDb(RequisitionMonthly rm) {
        if (rm == null) return null;

        String unit = rm.getUnit();

        // DB might have stored "NEW" or real codes; treat "NEW" as not usable in key
        String sap = rm.getOldSAPCode();
        String hana = rm.getHanaSAPCode();

        String vn = rm.getItemDescriptionVN();
        String en = rm.getItemDescriptionEN();

        return buildRowKeyWithUnitByPriority(unit, sap, hana, vn, en);
    }

    /**
     * ✅ Check tồn tại theo ưu tiên:
     * SAP (usable) -> HANA (usable) -> DesVN (usable) -> DesEN (usable)
     * Unique = groupId + unit + chosenKey
     *
     * currentId: exclude chính nó khi update
     */
    public Optional<RequisitionMonthly> checkExistsInGroupWithUnitByPriority(
            String groupId,
            String unit,
            String sapCode,
            String hanaCode,
            String descVN,
            String descEN,
            String currentId
    ) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId is required");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("unit is required");
        }

        String g = groupId.trim();
        String u = unit.trim();

        Optional<RequisitionMonthly> found;

        if (isUsableKey(sapCode)) {
            found = requisitionMonthlyRepository
                    .findFirstByGroupIdAndUnitIgnoreCaseAndOldSAPCodeIgnoreCase(g, u, sapCode.trim());
        } else if (isUsableKey(hanaCode)) {
            found = requisitionMonthlyRepository
                    .findFirstByGroupIdAndUnitIgnoreCaseAndHanaSAPCodeIgnoreCase(g, u, hanaCode.trim());
        } else if (isUsableKey(descVN)) {
            found = requisitionMonthlyRepository
                    .findFirstByGroupIdAndUnitIgnoreCaseAndItemDescriptionVNIgnoreCase(g, u, descVN.trim());
        } else if (isUsableKey(descEN)) {
            found = requisitionMonthlyRepository
                    .findFirstByGroupIdAndUnitIgnoreCaseAndItemDescriptionENIgnoreCase(g, u, descEN.trim());
        } else {
            return Optional.empty();
        }

        if (found.isPresent() && currentId != null && found.get().getId().equals(currentId)) {
            return Optional.empty();
        }

        return found;
    }
}
