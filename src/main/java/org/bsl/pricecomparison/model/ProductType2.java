package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "product_type_2")
public class ProductType2 {

    @Id
    private String id;
    private String name;
    private LocalDateTime createdDate;
    private String productType1Id;

    public ProductType2() {
    }

    public ProductType2(String name, LocalDateTime createdDate, String productType1Id) {
        this.name = name;
        this.createdDate = createdDate;
        this.productType1Id = productType1Id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getProductType1Id() {
        return productType1Id;
    }

    public void setProductType1Id(String productType1Id) {
        this.productType1Id = productType1Id;
    }
}
