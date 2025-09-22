package org.bsl.pricecomparison.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class CreateRequisitionMonthlyRequest {

    @ArraySchema(
            arraySchema = @Schema(description = "Requisition image files", type = "array"),
            minItems = 0,
            maxItems = 10,
            uniqueItems = false,
            schema = @Schema(type = "string", format = "binary")
    )
    private List<MultipartFile> files;

    @Schema(description = "English item description", example = "Product XYZ")
    private String itemDescriptionEN;

    @Schema(description = "Vietnamese item description", example = "Sản phẩm XYZ")
    private String itemDescriptionVN;

    @Schema(description = "Old SAP code", example = "OLD123")
    private String oldSAPCode;

    @Schema(description = "New SAP code", example = "NEW456")
    private String sapCodeNewSAP;

    @Schema(description = "Department requisitions as JSON string", example = "[{\"departmentId\": \"dept1\", \"departmentName\": \"IT Department\", \"qty\": 10, \"buy\": 8}, {\"departmentId\": \"dept2\", \"departmentName\": \"HR Department\", \"qty\": 20, \"buy\": 15}]")
    private String departmentRequisitions;

    @Schema(description = "Total not issued quantity", example = "50.0")
    private Double totalNotIssuedQty;

    @Schema(description = "In-hand quantity", example = "100.0")
    private Double inHand;

    @Schema(description = "Supplier ID", example = "1")
    private String supplierId;

    @Schema(description = "Product Type 1 ID", example = "1")
    private String productType1Id;

    @Schema(description = "Product Type 2 ID", example = "2")
    private String productType2Id;

    // --- Getters and Setters ---

    public List<MultipartFile> getFiles() {
        return files;
    }

    public void setFiles(List<MultipartFile> files) {
        this.files = files;
    }

    public String getItemDescriptionEN() {
        return itemDescriptionEN;
    }

    public void setItemDescriptionEN(String itemDescriptionEN) {
        this.itemDescriptionEN = itemDescriptionEN;
    }

    public String getItemDescriptionVN() {
        return itemDescriptionVN;
    }

    public void setItemDescriptionVN(String itemDescriptionVN) {
        this.itemDescriptionVN = itemDescriptionVN;
    }

    public String getOldSAPCode() {
        return oldSAPCode;
    }

    public void setOldSAPCode(String oldSAPCode) {
        this.oldSAPCode = oldSAPCode;
    }

    public String getSapCodeNewSAP() {
        return sapCodeNewSAP;
    }

    public void setSapCodeNewSAP(String sapCodeNewSAP) {
        this.sapCodeNewSAP = sapCodeNewSAP;
    }

    public String getDepartmentRequisitions() {
        return departmentRequisitions;
    }

    public void setDepartmentRequisitions(String departmentRequisitions) {
        this.departmentRequisitions = departmentRequisitions;
    }

    public Double getTotalNotIssuedQty() {
        return totalNotIssuedQty;
    }

    public void setTotalNotIssuedQty(Double totalNotIssuedQty) {
        this.totalNotIssuedQty = totalNotIssuedQty;
    }

    public Double getInHand() {
        return inHand;
    }

    public void setInHand(Double inHand) {
        this.inHand = inHand;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getProductType1Id() {
        return productType1Id;
    }

    public void setProductType1Id(String productType1Id) {
        this.productType1Id = productType1Id;
    }

    public String getProductType2Id() {
        return productType2Id;
    }

    public void setProductType2Id(String productType2Id) {
        this.productType2Id = productType2Id;
    }
}