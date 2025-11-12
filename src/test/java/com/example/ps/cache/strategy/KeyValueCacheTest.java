package com.example.ps.cache.strategy;

import com.example.ps.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class KeyValueCacheTest {

    private KeyValueCache<String, String> cache;
    private CacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig("test-cache", 3, true);
        cache = new KeyValueCache<>(cacheConfig);
    }

    @Test
    void testSaveAndFetch() {
        cache.save("key1", "value1");
        
        Optional<String> result = cache.fetch("key1");
        
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void testFetchNonExistentKey() {
        Optional<String> result = cache.fetch("nonexistent");
        
        assertFalse(result.isPresent());
    }

    @Test
    void testCacheSize() {
        assertEquals(0, cache.size());
        
        cache.save("key1", "value1");
        assertEquals(1, cache.size());
        
        cache.save("key2", "value2");
        assertEquals(2, cache.size());
    }

    @Test
    void testLRUEviction() {
        // Fill cache to capacity
        cache.save("key1", "value1");
        cache.save("key2", "value2");
        cache.save("key3", "value3");
        assertEquals(3, cache.size());
        
        // Adding another item should evict the least recently used (key1)
        cache.save("key4", "value4");
        assertEquals(3, cache.size());
        
        // key1 should be evicted
        assertFalse(cache.fetch("key1").isPresent());
        assertTrue(cache.fetch("key2").isPresent());
        assertTrue(cache.fetch("key3").isPresent());
        assertTrue(cache.fetch("key4").isPresent());
    }

    @Test
    void testLRUAccessOrder() {
        cache.save("key1", "value1");
        cache.save("key2", "value2");
        cache.save("key3", "value3");
        
        // Access key1 to make it most recently used
        cache.fetch("key1");
        
        // Add new item, key2 should be evicted (least recently used)
        cache.save("key4", "value4");
        
        assertTrue(cache.fetch("key1").isPresent());  // Should still be present
        assertFalse(cache.fetch("key2").isPresent()); // Should be evicted
        assertTrue(cache.fetch("key3").isPresent());
        assertTrue(cache.fetch("key4").isPresent());
    }

    @Test
    void testOverwriteExistingKey() {
        cache.save("key1", "value1");
        assertEquals(1, cache.size());
        
        cache.save("key1", "value2");
        assertEquals(1, cache.size());
        
        Optional<String> result = cache.fetch("key1");
        assertTrue(result.isPresent());
        assertEquals("value2", result.get());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int itemsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // Launch multiple threads that add items concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < itemsPerThread; j++) {
                        String key = "key_" + threadId + "_" + j;
                        String value = "value_" + threadId + "_" + j;
                        cache.save(key, value);
                        
                        // Also test concurrent reads
                        cache.fetch(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Cache should maintain its capacity constraint even with concurrent access
        assertTrue(cache.size() <= cacheConfig.capacity());
        
        // All operations should complete without exceptions due to proper synchronization
        assertTrue(cache.size() >= 0);
    }

    @Test
    void testCacheCapacityRespected() {
        CacheConfig largeCacheConfig = new CacheConfig("large-cache", 1000, true);
        KeyValueCache<Integer, String> largeCache = new KeyValueCache<>(largeCacheConfig);
        
        // Add more items than capacity
        for (int i = 0; i < 1500; i++) {
            largeCache.save(i, "value" + i);
        }
        
        // Size should not exceed capacity
        assertEquals(1000, largeCache.size());
    }

    @Test
    void testEmptyCache() {
        assertEquals(0, cache.size());
        assertFalse(cache.fetch("any_key").isPresent());
    }
}
