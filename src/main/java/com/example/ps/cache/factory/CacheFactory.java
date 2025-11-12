package com.example.ps.cache.factory;

import com.example.ps.cache.strategy.Cache;
import com.example.ps.cache.strategy.KeyValueCache;
import com.example.ps.cache.strategy.TypeAwareCache;
import com.example.ps.cache.strategy.TypeBasedCache;
import com.example.ps.config.CacheConfig;
import com.example.ps.config.CacheProperties;
import com.example.ps.config.TypeCacheConfig;

public class CacheFactory {

  public static <K, V> Cache<K, V> getCache(String name, CacheProperties props) {
    CacheConfig cacheConfig = props.getCacheConfigMap().get(name);
    return new KeyValueCache<>(cacheConfig);
  }

  public static <T, K> TypeAwareCache<T, K> getTypeCache(String name, CacheProperties props) {
    TypeCacheConfig typeCacheConfig = props.getTypeCacheConfigs().get(name);
    return new TypeBasedCache<>(typeCacheConfig);
  }

}
