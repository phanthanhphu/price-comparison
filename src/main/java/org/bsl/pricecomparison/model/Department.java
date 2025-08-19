package org.bsl.pricecomparison.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "departments")
public class Department {

    @Id
    private String id;

    private String name;
    private String englishName;

    @Indexed
    private LocalDateTime createdAt;

    public Department() {
        // Set createdAt when creating a new department
        this.createdAt = LocalDateTime.now();
    }

    public Department(String id, String name, String englishName) {
        this.id = id;
        this.name = name;
        this.englishName = englishName;
        this.createdAt = LocalDateTime.now();
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

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
