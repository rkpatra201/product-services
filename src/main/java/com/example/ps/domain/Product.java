package com.example.ps.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.util.Map;

/**
 * Product record representing a product in the catalog.
 * Uses Java 17 record features for immutable data structure.
 */
@Document(collection = "products")
public record Product(
    @Id
    String id,
    
    @NotBlank(message = "Product name cannot be blank")
    @Field("name")
    String name,
    
    @NotBlank(message = "Product type cannot be blank")
    @Field("type")
    String type,
    
    @NotBlank(message = "Product category cannot be blank")
    @Field("category")
    String category,
    
    @NotNull(message = "Product price cannot be null")
    @Positive(message = "Product price must be positive")
    @Field("price")
    Long price,
    
    @Field("recommendedAgeGroup")
    String recommendedAgeGroup,
    
    @Field("attributes")
    Map<String, String> attributes
) implements Serializable {

    /**
     * Compact constructor with validation for required fields.
     */
    public Product {
        // Validation can be added here if needed beyond annotations
        if (attributes == null) {
            attributes = Map.of(); // Default to empty map
        }
    }
    
    /**
     * Convenience constructor without ID (for new products).
     */
    public Product(String name, String type, String category, Long price, 
                  String recommendedAgeGroup, Map<String, String> attributes) {
        this(null, name, type, category, price, recommendedAgeGroup, attributes);
    }
}
