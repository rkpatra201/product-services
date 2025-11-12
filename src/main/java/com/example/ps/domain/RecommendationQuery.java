package com.example.ps.domain;

public record RecommendationQuery(Long minPrice, Long maxPrice, String type, String category, Integer age) {

  @Override
  public String toString() {
    return "RecommendationQuery{" +
        "minPrice=" + minPrice +
        ", maxPrice=" + maxPrice +
        ", type='" + type + '\'' +
        ", category='" + category + '\'' +
        ", age=" + age +
        '}';
  }
}
