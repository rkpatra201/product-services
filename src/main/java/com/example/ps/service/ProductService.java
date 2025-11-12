package com.example.ps.service;

import com.example.ps.cache.provider.CacheProvider;
import com.example.ps.cache.strategy.Cache;
import com.example.ps.cache.strategy.TypeAwareCache;
import com.example.ps.domain.Product;
import com.example.ps.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class ProductService {

  private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

  private final ProductRepository productRepository;
  private final CacheProvider cacheProvider;
  // Dedicated cache for type-based product lists
  private final Map<String, List<Product>> typeBasedListCache = new ConcurrentHashMap<>();

  public ProductService(ProductRepository productRepository, CacheProvider cacheProvider) {
    this.productRepository = productRepository;
    this.cacheProvider = cacheProvider;
  }

  public Optional<Product> findById(String productId) {
    if (productId == null || productId.trim().isEmpty()) {
      throw new IllegalArgumentException("Product ID cannot be null or empty");
    }

    logger.debug("Fetching product with ID: {} (checking cache first)", productId);

    try {
      Cache<String, Product> productCache = cacheProvider.getProductIdCache();
      Optional<Product> cachedProduct = productCache.fetch(productId);
      if (cachedProduct.isPresent()) {
        logger.debug("Product {} found in cache", productId);
        return cachedProduct;
      }

      logger.debug("Product {} not in cache, fetching from database", productId);
      Optional<Product> product = productRepository.findById(productId);

      if (product.isPresent()) {
        try {
          productCache.save(productId, product.get());
          logger.debug("Product {} cached after database fetch", productId);
        } catch (Exception e) {
          logger.warn("Failed to cache product {}: {}", productId, e.getMessage());
          // Continue without caching - don't fail the request
        }
      }

      return product;
    } catch (Exception e) {
      logger.error("Error fetching product with ID: {}", productId, e);
      throw e; // Re-throw to be handled by global exception handler
    }
  }

  public List<Product> findByType(String type) {
    logger.debug("Fetching products of type: {} (checking type-based cache first)", type);

    String typeKey = type.toUpperCase();

    TypeAwareCache<String, Product> typeCache = cacheProvider.getTypeCache();

    // First check our dedicated type-based list cache
    Iterable<Product> cachedProducts = typeCache.fetch(typeKey);
    if (cachedProducts != null) {
      logger.debug("Found {} cached products for type: {}", typeCache.totalSize(), type);
      List<Product> resultList = new ArrayList<>();
      cachedProducts.forEach(resultList::add);
      return resultList;
    }

    logger.debug("Type {} not in list cache, fetching from database", type);
    List<Product> products = productRepository.findByType(type);

    // Cache each product individually for single product lookups
    typeCache.save(type, products);

    logger.debug("Cached {} products for type: {}", products.size(), type);

    return products;
  }


  public List<Product> findAll() {
    logger.debug("Fetching all products from database");
    List<Product> products = productRepository.findAll();
    return products;
  }

  public Product save(Product product) {
    logger.debug("Saving product: {}", product.id());
    Product savedProduct = productRepository.save(product);
    return savedProduct;
  }
}
