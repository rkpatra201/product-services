package com.example.ps.cache.strategy;

import java.util.Optional;

public interface Cache<K, V> {
  public void save(K k, V v);

  public Optional<V> fetch(K k);

  public int size();
}
