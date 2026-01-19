package org.bsl.pricecomparison.common;

import org.bsl.pricecomparison.model.DepartmentRequisitionMonthly;
import org.bsl.pricecomparison.model.RequisitionMonthly;
import org.bsl.pricecomparison.repository.RequisitionMonthlyRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class CommonRequisitionUtils {

    private static final String UPLOAD_DIR = "./uploads/";

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

    /**
     * ✅ NEW: Build key include DEPT NAME (so duplicate check is per-department)
     * deptName null/blank => "NONE"
     */
    public static String buildRowKeyWithUnitByPriority(
            String unit,
            String sapCodeNormOrRaw,
            String hanaCodeNormOrRaw,
            String descVN,
            String descEN,
            String deptName
    ) {
        String base = buildRowKeyWithUnitByPriority(unit, sapCodeNormOrRaw, hanaCodeNormOrRaw, descVN, descEN);
        if (base == null) return null;

        String d = normText(deptName);
        String deptPart = (d == null) ? "NONE" : d.toLowerCase();

        return base + "|DEPT|" + deptPart;
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

        // ✅ NEW: deptName (use NAME as you requested)
        String deptName = null;
        List<DepartmentRequisitionMonthly> drs = rm.getDepartmentRequisitions();
        if (drs != null && !drs.isEmpty()) {
            DepartmentRequisitionMonthly dr0 = drs.get(0);
            if (dr0 != null) deptName = dr0.getName();
        }

        return buildRowKeyWithUnitByPriority(unit, sap, hana, vn, en, deptName);
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

    public String getImageExtension(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    public String saveImage(byte[] imageBytes, String fileName, String mimeType) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) return null;

        if (!isAllowedImageMime(mimeType)) {
            System.err.println("Skipped unsupported format: " + mimeType);
            return null;
        }

        Path path = Paths.get(UPLOAD_DIR).resolve(fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, imageBytes);

        return "/uploads/" + fileName;
    }

    private String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IOException("Only JPEG, PNG, and GIF files are allowed");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());

        file.transferTo(path);

        return "/uploads/" + fileName;
    }

    private boolean isAllowedImageMime(String mimeType) {
        if (mimeType == null) return false;
        Set<String> allowed = Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp");
        return allowed.contains(mimeType);
    }

    /* =========================
      ✅ NEW: chỉ thêm fun để gộp phòng ban vào request
      - không đụng logic khác
   ========================= */
    public void mergeDeptIntoRequest(
            RequisitionMonthly req,
            String deptId,
            String deptNameFromDb,
            BigDecimal requestQty,
            BigDecimal buy
    ) {
        if (req == null) return;
        if (deptId == null || deptId.trim().isEmpty()) return;

        List<DepartmentRequisitionMonthly> list = req.getDepartmentRequisitions();
        if (list == null) {
            list = new ArrayList<>();
            req.setDepartmentRequisitions(list);
        }

        // bình thường không xảy ra vì deptKey đã chặn duplicate,
        // nhưng thêm safeguard: nếu có dept rồi thì cộng dồn
        for (DepartmentRequisitionMonthly dr : list) {
            if (dr != null && deptId.equals(dr.getId())) {
                dr.setQty(nz(dr.getQty()).add(requestQty));
                dr.setBuy(nz(dr.getBuy()).add(buy));
                return;
            }
        }

        DepartmentRequisitionMonthly dr = new DepartmentRequisitionMonthly();
        dr.setId(deptId);
        dr.setName(deptNameFromDb);
        dr.setQty(requestQty);
        dr.setBuy(buy);
        list.add(dr);
    }

    public BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public RequisitionMonthly upsertMergedRequisition(
            Map<String, RequisitionMonthly> mergedByItemKey,
            String itemKey,
            String groupId,

            // identity fields
            String oldSap,
            String hanaCode,
            String itemVN,
            String itemEN,
            String unit,

            // numbers
            BigDecimal requestQty,
            BigDecimal buy,                 // bạn tự tính theo rule của API (vd buy = dailyMedInventory)
            BigDecimal dailyMedInventory,    // giá trị M hoặc inhand tuỳ API

            // dept
            String deptId,
            String deptNameFromDb,

            // others
            String reason,
            List<String> imageUrls,

            // optional extras
            String currencyFromGroup // null nếu API không set currency
    ) {
        RequisitionMonthly req = mergedByItemKey.get(itemKey);

        if (req == null) {
            req = new RequisitionMonthly();
            req.setGroupId(groupId);
            req.setCreatedDate(LocalDateTime.now());
            req.setUpdatedDate(LocalDateTime.now());

            req.setOldSAPCode(oldSap);
            req.setHanaSAPCode(hanaCode);
            req.setItemDescriptionVN(itemVN);
            req.setItemDescriptionEN(itemEN);

            req.setUnit(unit);

            // totals (init)
            req.setTotalRequestQty(nz(requestQty));
            req.setOrderQty(nz(buy));
            req.setDailyMedInventory(nz(dailyMedInventory));

            // currency optional
            if (currencyFromGroup != null && !currencyFromGroup.trim().isEmpty()) {
                req.setCurrency(currencyFromGroup.trim());
            }

            // reason
            if (reason != null && !reason.trim().isEmpty()) {
                req.setReason(reason);
            }

            // images
            req.setImageUrls((imageUrls == null || imageUrls.isEmpty()) ? null : new ArrayList<>(imageUrls));

            // dept
            if (deptId != null) {
                DepartmentRequisitionMonthly dr = new DepartmentRequisitionMonthly();
                dr.setId(deptId);
                dr.setName(deptNameFromDb);
                dr.setQty(nz(requestQty));
                dr.setBuy(nz(buy));
                req.setDepartmentRequisitions(new ArrayList<>(List.of(dr)));
            }

            mergedByItemKey.put(itemKey, req);
            return req;
        }

        // ===== MERGE EXISTING =====
        mergeDeptIntoRequest(req, deptId, deptNameFromDb, requestQty, buy);

        req.setTotalRequestQty(nz(req.getTotalRequestQty()).add(nz(requestQty)));
        req.setOrderQty(nz(req.getOrderQty()).add(nz(buy)));
        req.setDailyMedInventory(nz(req.getDailyMedInventory()).add(nz(dailyMedInventory)));

        // images append
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<String> existing = req.getImageUrls();
            if (existing == null) existing = new ArrayList<>();
            existing.addAll(imageUrls);
            req.setImageUrls(existing);
        }

        // reason: chỉ set nếu đang trống
        if ((req.getReason() == null || req.getReason().trim().isEmpty())
                && reason != null && !reason.trim().isEmpty()) {
            req.setReason(reason);
        }

        // currency: nếu req chưa có currency mà có currencyFromGroup
        if ((req.getCurrency() == null || req.getCurrency().trim().isEmpty())
                && currencyFromGroup != null && !currencyFromGroup.trim().isEmpty()) {
            req.setCurrency(currencyFromGroup.trim());
        }

        req.setUpdatedDate(LocalDateTime.now());
        return req;
    }

}
