package com.example.ps.service;

import com.example.ps.cache.provider.CacheProvider;
import com.example.ps.cache.strategy.TypeAwareCache;
import com.example.ps.domain.Product;
import com.example.ps.domain.RecommendationQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private CacheProvider cacheProvider;

    @Mock
    private TypeAwareCache<String, Product> recommendationCache;

    private RecommendationService recommendationService;

    private List<Product> testProducts;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(productService, cacheProvider);
        
        when(cacheProvider.getRecommendationCache()).thenReturn(recommendationCache);
        
        // Create test products
        testProducts = List.of(
            new Product("P1", "iPhone", "ELECTRONICS", "SMARTPHONE", 50000L, "18-45", 
                       Map.of("color", "Black")),
            new Product("P2", "Samsung Phone", "ELECTRONICS", "SMARTPHONE", 30000L, "16-40", 
                       Map.of("color", "Blue")),
            new Product("P3", "Laptop", "ELECTRONICS", "LAPTOP", 80000L, "22-65", 
                       Map.of("brand", "Dell")),
            new Product("P4", "Jeans", "FASHION", "CLOTHING", 5000L, "18-50", 
                       Map.of("size", "32")),
            new Product("P5", "Book", "BOOKS", "FICTION", 500L, "10-80", 
                       Map.of("pages", "300")),
            new Product("P6", "Expensive Watch", "ELECTRONICS", "WEARABLE", 200000L, "25-60", 
                       Map.of("luxury", "true"))
        );
    }

    @Test
    void testGetRecommendations_CacheMiss() {
        RecommendationQuery query = new RecommendationQuery(1000L, 50000L, "ELECTRONICS", null, 25);
        
        when(recommendationCache.fetch(query.toString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include iPhone and Samsung Phone (both in price range and age range)
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(p -> p.name().equals("iPhone")));
        assertTrue(results.stream().anyMatch(p -> p.name().equals("Samsung Phone")));
        
        // Verify cache was checked
        verify(recommendationCache).fetch(query.toString());
        verify(productService).findAll();
        verify(recommendationCache).save(eq(query.toString()), any(Iterable.class));
    }

    @Test
    void testGetRecommendations_DatabaseFallback() {
        RecommendationQuery query = new RecommendationQuery(1000L, 50000L, "ELECTRONICS", null, 25);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include iPhone and Samsung Phone (both in price range and age range)
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(p -> p.name().equals("iPhone")));
        assertTrue(results.stream().anyMatch(p -> p.name().equals("Samsung Phone")));
        
        // Verify cache interactions
        verify(recommendationCache).fetch(query.toString());
        verify(recommendationCache).save(eq(query.toString()), any(Iterable.class));
        verify(productService).findAll();
    }

    @Test
    void testPriceRangeFiltering() {
        RecommendationQuery query = new RecommendationQuery(1000L, 10000L, null, null, null);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should only include Jeans (5000L) - Book (500L) is below minPrice
        assertEquals(1, results.size());
        assertTrue(results.stream().allMatch(p -> p.price() >= 1000L && p.price() <= 10000L));
        assertTrue(results.stream().anyMatch(p -> p.name().equals("Jeans")));
    }

    @Test
    void testMinPriceOnly() {
        RecommendationQuery query = new RecommendationQuery(50000L, null, null, null, null);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include Laptop (80000L), iPhone (50000L), and Expensive Watch (200000L)
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(p -> p.price() >= 50000L));
    }

    @Test
    void testMaxPriceOnly() {
        RecommendationQuery query = new RecommendationQuery(null, 10000L, null, null, null);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include Jeans (5000L) and Book (500L)
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.price() <= 10000L));
    }

    @Test
    void testTypeFiltering() {
        RecommendationQuery query = new RecommendationQuery(null, null, "ELECTRONICS", null, null);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include iPhone, Samsung Phone, Laptop, and Expensive Watch
        assertEquals(4, results.size());
        assertTrue(results.stream().allMatch(p -> p.type().equals("ELECTRONICS")));
    }

    @Test
    void testCategoryFiltering() {
        RecommendationQuery query = new RecommendationQuery(null, null, null, "SMARTPHONE", null);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include iPhone and Samsung Phone
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.category().equals("SMARTPHONE")));
    }

    @Test
    void testAgeRangeFiltering() {
        RecommendationQuery query = new RecommendationQuery(null, null, null, null, 20);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include products where 20 falls within their age range
        // iPhone (18-45), Samsung (16-40), Jeans (18-50), Book (10-80) - Laptop (22-65) excludes 20
        assertEquals(4, results.size());
        assertFalse(results.stream().anyMatch(p -> p.name().equals("Expensive Watch"))); // 25-60, 20 is outside
        assertFalse(results.stream().anyMatch(p -> p.name().equals("Laptop"))); // 22-65, 20 is outside
    }

    @Test
    void testAgeRangeEdgeCases() {
        // Test age exactly at range boundaries
        RecommendationQuery query1 = new RecommendationQuery(null, null, null, null, 18);
        RecommendationQuery query2 = new RecommendationQuery(null, null, null, null, 45);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results1 = recommendationService.getRecommendations(query1);
        List<Product> results2 = recommendationService.getRecommendations(query2);
        
        // Age 18 should include iPhone (18-45), Jeans (18-50), and Book (10-80)
        assertTrue(results1.stream().anyMatch(p -> p.name().equals("iPhone")));
        assertTrue(results1.stream().anyMatch(p -> p.name().equals("Jeans")));
        assertTrue(results1.stream().anyMatch(p -> p.name().equals("Book")));
        
        // Age 45 should include iPhone (18-45) and Book (10-80)
        assertTrue(results2.stream().anyMatch(p -> p.name().equals("iPhone")));
        assertTrue(results2.stream().anyMatch(p -> p.name().equals("Book")));
    }

    @Test
    void testCombinedFilters() {
        // Test multiple filters together
        RecommendationQuery query = new RecommendationQuery(20000L, 60000L, "ELECTRONICS", "SMARTPHONE", 25);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Should include iPhone and Samsung Phone (both match all criteria)
        // iPhone: 50000L, ELECTRONICS, SMARTPHONE, age 25 in range 18-45
        // Samsung: 30000L, ELECTRONICS, SMARTPHONE, age 25 in range 16-40  
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(p -> p.name().equals("iPhone")));
        assertTrue(results.stream().anyMatch(p -> p.name().equals("Samsung Phone")));
    }

    @Test
    void testInvalidAgeRange() {
        // Test with product having invalid age range
        Product invalidProduct = new Product("P7", "Invalid Product", "TEST", "TEST", 1000L, 
                                           "invalid-range", Map.of());
        List<Product> productsWithInvalid = List.of(invalidProduct);
        
        RecommendationQuery query = new RecommendationQuery(null, null, null, null, 25);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(productsWithInvalid);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Product with invalid age range should be filtered out
        assertEquals(0, results.size());
        
        // Verify interactions
        verify(recommendationCache).fetch(anyString());
        verify(productService).findAll();
    }

    @Test
    void testNullAgeRange() {
        // Test with product having null age range
        Product nullAgeProduct = new Product("P8", "Null Age Product", "TEST", "TEST", 1000L, 
                                           null, Map.of());
        List<Product> productsWithNull = List.of(nullAgeProduct);
        
        RecommendationQuery query = new RecommendationQuery(null, null, null, null, 25);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(productsWithNull);
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        // Product with null age range should be included (default behavior)
        assertEquals(1, results.size());
        assertEquals("Null Age Product", results.get(0).name());
    }

    @Test
    void testEmptyProductList() {
        RecommendationQuery query = new RecommendationQuery(1000L, 50000L, "ELECTRONICS", null, 25);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(List.of());
        
        List<Product> results = recommendationService.getRecommendations(query);
        
        assertEquals(0, results.size());
        verify(recommendationCache).save(eq(query.toString()), eq(List.of()));
    }

    @Test
    void testCaseInsensitiveTypeAndCategoryMatching() {
        RecommendationQuery query1 = new RecommendationQuery(null, null, "electronics", null, null);
        RecommendationQuery query2 = new RecommendationQuery(null, null, null, "smartphone", null);
        
        when(recommendationCache.fetch(anyString())).thenReturn(null);
        when(productService.findAll()).thenReturn(testProducts);
        
        List<Product> results1 = recommendationService.getRecommendations(query1);
        List<Product> results2 = recommendationService.getRecommendations(query2);
        
        // Should match despite case difference
        assertEquals(4, results1.size()); // All ELECTRONICS products
        assertEquals(2, results2.size()); // All SMARTPHONE products
    }
}
