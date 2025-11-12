package com.example.ps.service;

import com.example.ps.cache.provider.CacheProvider;
import com.example.ps.cache.strategy.Cache;
import com.example.ps.cache.strategy.TypeAwareCache;
import com.example.ps.domain.Product;
import com.example.ps.domain.RecommendationQuery;
import com.example.ps.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class RecommendationService {

  private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
  private static final Pattern AGE_RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");

  private final ProductService productService;
  private final CacheProvider cacheProvider;

  public RecommendationService(ProductService productService,
                               CacheProvider cacheProvider) {
    this.productService = productService;
    this.cacheProvider = cacheProvider;

  }

  public List<Product> getRecommendations(RecommendationQuery query) {
    if (query == null) {
      throw new IllegalArgumentException("Recommendation query cannot be null");
    }

    try {
      TypeAwareCache<String, Product> recommendationCache = cacheProvider.getRecommendationCache();

      // Check cache first
      String cacheKey = query.toString();
      Iterable<Product> cachedResult = recommendationCache.fetch(cacheKey);
      if (cachedResult != null) {
        logger.debug("Cache hit for recommendation key: {}", cacheKey);
        List<Product> resultList = new ArrayList<>();
        cachedResult.forEach(resultList::add);
        return resultList;
      }

      logger.debug("Cache miss for recommendation key: {}", cacheKey);

      // Compute recommendations
      List<Product> recommendations = computeRecommendations(query);

      // Store in cache - don't fail if cache operation fails
      try {
        recommendationCache.save(cacheKey, recommendations);
        logger.debug("Stored {} recommendations in cache for key: {}", recommendations.size(), cacheKey);
      } catch (CacheException e) {
        logger.warn("Failed to store recommendations in cache: {}", e.getMessage());
        // Continue without caching - don't fail the request
      }

      return recommendations;
    } catch (Exception e) {
      logger.error("Error processing recommendations for query: {}", query, e);
      throw e; // Re-throw to be handled by global exception handler
    }
  }

  private List<Product> computeRecommendations(RecommendationQuery query) {
    List<Product> allProducts = productService.findAll();

    return allProducts.stream()
        .filter(product -> matchesType(product, query.type()))
        .filter(product -> matchesCategory(product, query.category()))
        .filter(product -> matchesPriceRange(product, query.minPrice(), query.maxPrice()))
        .filter(product -> matchesAgeRange(product, query.age()))
        .toList();
  }

  private boolean matchesPriceRange(Product product, Long minPrice, Long maxPrice) {
    Long productPrice = product.price();

    if (minPrice != null && productPrice < minPrice) {
      return false;
    }

    if (maxPrice != null && productPrice > maxPrice) {
      return false;
    }

    return true;
  }

  private boolean matchesType(Product product, String type) {
    return type == null || type.equalsIgnoreCase(product.type());
  }

  private boolean matchesCategory(Product product, String category) {
    return category == null || category.equalsIgnoreCase(product.category());
  }

  private boolean matchesAgeRange(Product product, Integer age) {
    if (age == null) {
      return true;
    }

    String ageRange = product.recommendedAgeGroup();
    if (ageRange == null || ageRange.trim().isEmpty()) {
      logger.debug("Product {} has no age range specified", product.id());
      return true;
    }

    return parseAgeRange(ageRange)
        .map(range -> age >= range.minAge() && age <= range.maxAge())
        .orElse(false);
  }

  private Optional<AgeRange> parseAgeRange(String ageRange) {
    if (ageRange == null || ageRange.trim().isEmpty()) {
      logger.debug("Age range is null or empty");
      return Optional.empty();
    }

    var matcher = AGE_RANGE_PATTERN.matcher(ageRange);
    if (matcher.matches()) {
      try {
        int minAge = Integer.parseInt(matcher.group(1));
        int maxAge = Integer.parseInt(matcher.group(2));
        return Optional.of(new AgeRange(minAge, maxAge));
      } catch (NumberFormatException e) {
        logger.warn("Invalid age range format: {}", ageRange);
        return Optional.empty();
      }
    } else {
      logger.debug("Unrecognized age range format: {}", ageRange);
      return Optional.empty();
    }
  }

  public record AgeRange(int minAge, int maxAge) {
    public AgeRange {
      if (minAge < 0 || maxAge < 0 || minAge > maxAge) {
        throw new IllegalArgumentException("Invalid age range: " + minAge + "-" + maxAge);
      }
    }
  }

}
