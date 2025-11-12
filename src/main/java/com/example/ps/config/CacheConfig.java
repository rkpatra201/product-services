package com.example.ps.config;

public record CacheConfig(
    String name,
    int capacity,
    boolean enabled
) {
}


