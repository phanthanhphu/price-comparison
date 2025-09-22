package org.bsl.pricecomparison.request;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Department request quantities")
public class DepartmentRequestQtyDTO {

    @Schema(description = "Map of department IDs to their quantities and approved buy amounts",
            example = "{\"dept1\": {\"qty\": 10.0, \"buy\": 8.0}, \"dept2\": {\"qty\": 20.0, \"buy\": 15.0}}")
    private Map<String, DepartmentQty> quantities;

    public Map<String, DepartmentQty> getQuantities() {
        return quantities;
    }

    public void setQuantities(Map<String, DepartmentQty> quantities) {
        this.quantities = quantities;
    }
}