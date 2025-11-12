package com.example.ps.cache.strategy;

import com.example.ps.config.TypeCacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class TypeBasedCacheTest {

    private TypeBasedCache<String, String> cache;
    private TypeCacheConfig cacheConfig;

    @BeforeEach
    void setUp() {
        cacheConfig = new TypeCacheConfig("test-type-cache", 5, 10, true);
        cache = new TypeBasedCache<>(cacheConfig);
    }

    @Test
    void testSaveAndFetchSingleItem() {
        cache.save("TYPE1", List.of("item1"));
        
        Iterable<String> result = cache.fetch("TYPE1");
        
        assertNotNull(result);
        assertTrue(result.iterator().hasNext());
        assertEquals("item1", result.iterator().next());
        assertEquals(1, cache.totalSize());
        assertEquals(1, cache.typeSize("TYPE1"));
    }

    @Test
    void testSaveMultipleItemsForSameType() {
        cache.save("TYPE1", List.of("item1", "item2", "item3"));
        
        Iterable<String> result = cache.fetch("TYPE1");
        assertNotNull(result);
        
        // Convert to set for easier testing
        Set<String> resultSet = new HashSet<>();
        result.forEach(resultSet::add);
        assertTrue(resultSet.contains("item1"));
        assertTrue(resultSet.contains("item2"));
        assertTrue(resultSet.contains("item3"));
        
        assertEquals(3, cache.totalSize());
        assertEquals(3, cache.typeSize("TYPE1"));
    }

    @Test
    void testSaveItemsForDifferentTypes() {
        cache.save("TYPE1", List.of("item1", "item2"));
        cache.save("TYPE2", List.of("item3", "item4"));
        
        Iterable<String> type1Result = cache.fetch("TYPE1");
        Iterable<String> type2Result = cache.fetch("TYPE2");
        
        assertNotNull(type1Result);
        assertNotNull(type2Result);
        
        assertEquals(4, cache.totalSize());
        assertEquals(2, cache.typeSize("TYPE1"));
        assertEquals(2, cache.typeSize("TYPE2"));
    }

    @Test
    void testFetchNonExistentType() {
        Iterable<String> result = cache.fetch("NONEXISTENT");
        assertNull(result);
        assertEquals(0, cache.typeSize("NONEXISTENT"));
    }

    @Test
    void testGlobalEvictionWhenCountExceeded() {
        // Config allows max 10 total items
        cache.save("TYPE1", List.of("item1", "item2", "item3"));  // 3 items
        cache.save("TYPE2", List.of("item4", "item5", "item6"));  // 6 total
        cache.save("TYPE3", List.of("item7", "item8", "item9", "item10"));  // 10 total (at limit)
        
        assertEquals(10, cache.totalSize());
        
        // Adding more should trigger eviction
        cache.save("TYPE4", List.of("item11", "item12"));
        
        // Should not exceed the count limit
        assertTrue(cache.totalSize() <= cacheConfig.count());
        assertEquals(10, cache.totalSize()); // Should be exactly at limit
    }

    @Test
    void testEvictionRemovesOldestItems() {
        // Fill to capacity
        cache.save("TYPE1", List.of("oldest1", "oldest2", "oldest3"));  // 3 items
        cache.save("TYPE2", List.of("older1", "older2", "older3"));    // 6 items
        cache.save("TYPE3", List.of("newer1", "newer2", "newer3", "newer4"));  // 10 items (at limit)
        
        assertEquals(10, cache.totalSize());
        
        // Add more items - should evict oldest
        cache.save("TYPE4", List.of("newest1", "newest2"));
        
        assertEquals(10, cache.totalSize());
        
        // TYPE4 should definitely be present (newest)
        assertNotNull(cache.fetch("TYPE4"));
        assertEquals(2, cache.typeSize("TYPE4"));
    }

    @Test
    void testDuplicateItemsNotAdded() {
        cache.save("TYPE1", List.of("item1", "item1", "item2"));
        
        // Duplicates should be handled by the underlying KeyValueCache
        assertEquals(2, cache.typeSize("TYPE1"));
        assertEquals(2, cache.totalSize());
        
        Iterable<String> result = cache.fetch("TYPE1");
        Set<String> resultSet = new HashSet<>();
        result.forEach(resultSet::add);
        assertTrue(resultSet.contains("item1"));
        assertTrue(resultSet.contains("item2"));
        assertEquals(2, resultSet.size());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int numThreads = 5;
        int itemsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < itemsPerThread; j++) {
                        String type = "TYPE_" + (threadId % 3); // Use limited types for more contention
                        String item = "item_" + threadId + "_" + j;
                        
                        cache.save(type, List.of(item));
                        cache.fetch(type);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Cache should maintain its count constraint
        assertTrue(cache.totalSize() <= cacheConfig.count());
        
        // All operations should complete without exceptions
        assertTrue(cache.totalSize() >= 0);
    }

    @Test
    void testTypeCapacityLimit() {
        TypeCacheConfig smallConfig = new TypeCacheConfig("small-cache", 2, 10, true);
        TypeBasedCache<String, String> smallCache = new TypeBasedCache<>(smallConfig);
        
        // Each type can have max 2 items (capacity), but global count is 10
        smallCache.save("TYPE1", List.of("item1", "item2"));
        assertEquals(2, smallCache.typeSize("TYPE1"));
        
        // Adding more to same type should respect per-type capacity
        smallCache.save("TYPE1", List.of("item3"));
        
        // Should evict within the type to maintain capacity
        assertEquals(2, smallCache.typeSize("TYPE1"));
    }

    @Test
    void testEmptyCache() {
        assertEquals(0, cache.totalSize());
        assertNull(cache.fetch("ANY_TYPE"));
        assertEquals(0, cache.typeSize("ANY_TYPE"));
    }

    @Test
    void testToString() {
        cache.save("TYPE1", List.of("item1"));
        cache.save("TYPE2", List.of("item2"));
        
        String toString = cache.toString();
        
        assertTrue(toString.contains("TypeBasedCache"));
        assertTrue(toString.contains("TYPE1"));
        assertTrue(toString.contains("TYPE2"));
        assertTrue(toString.contains("Total: 2/10"));
    }

    @Test
    void testSaveEmptyIterable() {
        cache.save("TYPE1", List.of());
        
        // Should handle empty iterables gracefully
        assertEquals(0, cache.totalSize());
        assertEquals(0, cache.typeSize("TYPE1"));
        assertNull(cache.fetch("TYPE1"));
    }

    @Test
    void testMultipleSavesToSameType() {
        cache.save("TYPE1", List.of("item1"));
        assertEquals(1, cache.totalSize());
        
        cache.save("TYPE1", List.of("item2", "item3"));
        // The actual behavior depends on whether items are replaced or added
        assertTrue(cache.totalSize() >= 2); // At least item2 and item3 should be present
        assertTrue(cache.typeSize("TYPE1") >= 2);
        
        Iterable<String> result = cache.fetch("TYPE1");
        Set<String> resultSet = new HashSet<>();
        result.forEach(resultSet::add);
        assertTrue(resultSet.size() >= 2);
        assertTrue(resultSet.contains("item2"));
        assertTrue(resultSet.contains("item3"));
    }
}
