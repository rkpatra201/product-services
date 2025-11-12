package com.example.ps.cache.strategy;

import com.example.ps.config.CacheConfig;
import com.example.ps.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Optional;

public class KeyValueCache<K, V> extends LinkedHashMap<K, V> implements Cache<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(KeyValueCache.class);
  private final CacheConfig cacheConfig;

  public KeyValueCache(CacheConfig cacheConfig) {
    super(cacheConfig.capacity(), 0.75f, true);
    this.cacheConfig = cacheConfig;
  }

  @Override
  public synchronized void save(K k, V v) {
    try {
      if (k == null) {
        throw new CacheException("Cache key cannot be null");
      }
      if (v == null) {
        throw new CacheException("Cache value cannot be null");
      }
      super.put(k, v);
      logger.debug("Stored value in cache for key: {}", k);
    } catch (Exception e) {
      logger.error("Failed to store value in cache for key: {}", k, e);
      throw new CacheException("Failed to store value in cache", e);
    }
  }

  @Override
  public synchronized Optional<V> fetch(K k) {
    try {
      if (k == null) {
        logger.warn("Attempted to fetch with null key");
        return Optional.empty();
      }
      V value = super.get(k);
      if (value != null) {
        logger.debug("Cache hit for key: {}", k);
      } else {
        logger.debug("Cache miss for key: {}", k);
      }
      return Optional.ofNullable(value);
    } catch (Exception e) {
      logger.error("Failed to fetch value from cache for key: {}", k, e);
      return Optional.empty(); // Return empty instead of throwing to allow fallback to database
    }
  }

  @Override
  public synchronized int size() {
    return super.size();
  }

  @Override
  protected synchronized boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
    boolean shouldEvict = size() > cacheConfig.capacity();
    return shouldEvict;
  }

}
