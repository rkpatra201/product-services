package com.example.ps.controllers;

import com.example.ps.domain.Product;
import com.example.ps.domain.RecommendationQuery;
import com.example.ps.exception.InvalidRecommendationQueryException;
import com.example.ps.exception.ProductNotFoundException;
import com.example.ps.service.ProductService;
import com.example.ps.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    private final ProductService productService;
    private final RecommendationService recommendationService;

    public ProductController(ProductService productService, RecommendationService recommendationService) {
        this.productService = productService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(
            @PathVariable @NotBlank(message = "Product ID cannot be blank") String productId) {
        
        logger.info("Received request to get product with ID: {}", productId);
        
        Product product = productService.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
            
        logger.info("Product found with ID: {}", productId);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Product>> getProductsByType(
            @PathVariable @NotBlank(message = "Product type cannot be blank") String type) {
        
        logger.info("Received request to get products of type: {}", type);
        
        List<Product> products = productService.findByType(type);
        
        if (products.isEmpty()) {
            logger.info("No products found for type: {}", type);
            return ResponseEntity.noContent().build();
        }
        
        logger.info("Found {} products for type: {}", products.size(), type);
        return ResponseEntity.ok(products);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        logger.info("Received request to get all products");
        
        List<Product> products = productService.findAll();
        
        if (products.isEmpty()) {
            logger.info("No products found");
            return ResponseEntity.noContent().build();
        }
        
        logger.info("Found {} products", products.size());
        return ResponseEntity.ok(products);
    }


    @GetMapping("/recommendations")
    public ResponseEntity<List<Product>> getRecommendations(
            @RequestParam(required = false) @Positive(message = "Minimum price must be positive") Long minPrice,
            @RequestParam(required = false) @Positive(message = "Maximum price must be positive") Long maxPrice,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @Positive(message = "Age must be positive") Integer age) {
        
        // Validate price range
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new InvalidRecommendationQueryException("Minimum price cannot be greater than maximum price");
        }
        
        // Validate age range
        if (age != null && (age < 0 || age > 120)) {
            throw new InvalidRecommendationQueryException("Age must be between 0 and 120");
        }

        List<Product> recommendations = recommendationService.getRecommendations(
            new RecommendationQuery(minPrice, maxPrice, type, category, age));
        
        if (recommendations.isEmpty()) {
            logger.info("No product recommendations found with the specified filters");
            return ResponseEntity.noContent().build();
        }
        
        logger.info("Found {} product recommendations", recommendations.size());
        return ResponseEntity.ok(recommendations);
    }
}
