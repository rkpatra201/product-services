package com.example.ps.cache.strategy;

import com.example.ps.config.CacheConfig;
import com.example.ps.config.TypeCacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TypeBasedCache<T, K> implements TypeAwareCache<T, K> {

  private static final Logger logger = LoggerFactory.getLogger(TypeBasedCache.class);

  private TypeCacheConfig cacheConfig;
  private final Map<T, KeyValueCache<K, Long>> typeCaches = new HashMap<>();
  private int totalItems = 0;

  public TypeBasedCache(TypeCacheConfig cacheConfig) {
    this.cacheConfig = cacheConfig;
  }

  @Override
  public synchronized void save(T type, Iterable<K> items) {
    items.forEach(item -> {
      save(type, item, System.currentTimeMillis());
    });
  }

  private synchronized void save(T type, K key, Long value) {
    KeyValueCache<K, Long> cache = typeCaches.computeIfAbsent(
        type, t -> new KeyValueCache<>(new CacheConfig(this.cacheConfig.name(), this.cacheConfig.capacity(), this.cacheConfig.enabled()))
    );

    int before = cache.size();
    cache.save(key, value);
    int after = cache.size();

    // Only count if new item added
    if (after > before) totalItems++;

    evictGloballyIfNeeded();
  }

  @Override
  public Iterable<K> fetch(T type) {
    KeyValueCache<K, Long> cache = typeCaches.get(type);
    if (cache == null) return Collections.emptySet();
    return cache.keySet();
  }

  @Override
  public synchronized int totalSize() {
    return totalItems;
  }

  @Override
  public synchronized int typeSize(T type) {
    KeyValueCache<K, Long> cache = typeCaches.get(type);
    return (cache == null) ? 0 : cache.size();
  }

  private void evictGloballyIfNeeded() {
    while (totalItems > this.cacheConfig.count()) {
      // Remove oldest across all caches
      for (Iterator<Map.Entry<T, KeyValueCache<K, Long>>> it = typeCaches.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<T, KeyValueCache<K, Long>> entry = it.next();
        KeyValueCache<K, Long> cache = entry.getValue();
        if (!cache.isEmpty()) {
          K oldestKey = cache.keySet().iterator().next();
          cache.remove(oldestKey);
          totalItems--;
          if (cache.isEmpty()) it.remove();
          break;
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TypeBasedCache:\n");
    typeCaches.forEach((type, cache) ->
        sb.append("  ").append(type).append(" -> ").append(cache.keySet()).append("\n"));
    sb.append("Total: ").append(totalItems).append("/").append(cacheConfig.count());
    return sb.toString();
  }
}
