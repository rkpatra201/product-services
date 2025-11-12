package com.example.ps.cache.provider;

import com.example.ps.cache.factory.CacheFactory;
import com.example.ps.cache.strategy.Cache;
import com.example.ps.cache.strategy.TypeAwareCache;
import com.example.ps.config.CacheProperties;
import com.example.ps.domain.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CacheProvider {
  @Autowired
  private CacheProperties cacheProperties;

  private Cache<String, Product> productIdCache;
  private TypeAwareCache<String, Product> typeCache;
  private TypeAwareCache<String, Product> recommendationCache;

  @PostConstruct
  public void init() {
    initCache();
  }

  private void initCache() {
    productIdCache = CacheFactory.getCache("id-cache", cacheProperties);
    typeCache = CacheFactory.getTypeCache("type-cache", cacheProperties);
    recommendationCache = CacheFactory.getTypeCache("recommendation-cache", cacheProperties);
  }

  public Cache<String, Product> getProductIdCache() {
    return productIdCache;
  }

  public TypeAwareCache<String, Product> getRecommendationCache() {
    return recommendationCache;
  }

  public TypeAwareCache<String, Product> getTypeCache() {
    return typeCache;
  }
}
