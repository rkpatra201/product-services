package com.example.ps.cache.strategy;

import java.util.Optional;
import java.util.Set;

public interface TypeAwareCache<T, K> {
    void save(T type, Iterable<K> items);
    Iterable<K> fetch(T type);
    int totalSize();
    int typeSize(T type);
}
