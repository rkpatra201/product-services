package com.example.ps.repository;

import com.example.ps.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    @Query("{'type': ?0}")
    List<Product> findByType(String type);

    @Query("{'category': ?0}")
    List<Product> findByCategory(String category);

    @Query("{'type': ?0, 'category': ?1}")
    List<Product> findByTypeAndCategory(String type, String category);

    @Query(value = "{'price': {$gte: ?0, $lte: ?1}}")
    List<Product> findByPriceBetween(Long minPrice, Long maxPrice);
}
