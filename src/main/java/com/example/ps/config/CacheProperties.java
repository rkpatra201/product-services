package com.example.ps.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

  private Map<String, CacheConfig> simpleCacheConfigs;
  private Map<String, TypeCacheConfig> typeCacheConfigs;

  @PostConstruct
  public void init(){
    // Configuration validation could be added here if needed
  }

  public Map<String, TypeCacheConfig> getTypeCacheConfigs() {
    return typeCacheConfigs;
  }

  public void setTypeCacheConfigs(Map<String, TypeCacheConfig> typeCacheConfigs) {
    this.typeCacheConfigs = typeCacheConfigs;
  }

  public Map<String, CacheConfig> getCacheConfigMap() {
    return simpleCacheConfigs;
  }

  public void setSimpleCacheConfigs(Map<String, CacheConfig> simpleCacheConfigs) {
    this.simpleCacheConfigs = simpleCacheConfigs;
  }

  public Map<String, CacheConfig> getSimpleCacheConfigs() {
    return simpleCacheConfigs;
  }
}
