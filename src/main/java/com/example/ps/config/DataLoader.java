package com.example.ps.config;

import com.example.ps.domain.Product;
import com.example.ps.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    
    private final ProductRepository productRepository;

    public DataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (productRepository.count() == 0) {
            logger.info("Database is empty. Loading sample data...");
            loadSampleData();
            logger.info("Sample data loaded successfully. Total products: {}", productRepository.count());
        } else {
            logger.info("Database already contains {} products. Skipping sample data loading.", 
                       productRepository.count());
        }
    }

    private void loadSampleData() {
        List<Product> sampleProducts = List.of(
            new Product("P10023", "Apple iPhone 16", "ELECTRONICS", "SMARTPHONE", 99900L, 
                       "18-45", Map.of("color", "Black", "storage", "128GB")),
            
            new Product("P10024", "Samsung Galaxy S24", "ELECTRONICS", "SMARTPHONE", 89900L, 
                       "18-50", Map.of("color", "Blue", "storage", "256GB")),
            
            new Product("P10025", "MacBook Pro M3", "ELECTRONICS", "LAPTOP", 199900L, 
                       "22-65", Map.of("color", "Space Gray", "ram", "16GB", "storage", "512GB")),
            
            new Product("P10026", "Nike Air Max", "FASHION", "SHOES", 12000L, 
                       "16-40", Map.of("color", "White", "size", "10")),
            
            new Product("P10027", "Levi's Jeans", "FASHION", "CLOTHING", 8000L, 
                       "18-50", Map.of("color", "Blue", "size", "32")),
            
            new Product("P10028", "Sony WH-1000XM5", "ELECTRONICS", "HEADPHONES", 35000L, 
                       "16-60", Map.of("color", "Black", "type", "Over-ear")),
            
            new Product("P10029", "The Great Gatsby", "BOOKS", "FICTION", 1500L, 
                       "16-80", Map.of("author", "F. Scott Fitzgerald", "pages", "180")),
            
            new Product("P10030", "Java Programming Book", "BOOKS", "PROGRAMMING", 4500L, 
                       "18-65", Map.of("author", "James Gosling", "pages", "650")),
            
            new Product("P10031", "Gaming Chair", "FURNITURE", "CHAIR", 25000L, 
                       "16-45", Map.of("color", "Black", "material", "Leather")),
            
            new Product("P10032", "Standing Desk", "FURNITURE", "DESK", 35000L, 
                       "25-60", Map.of("color", "Brown", "material", "Wood", "adjustable", "true")),
            
            new Product("P10033", "Protein Powder", "HEALTH", "SUPPLEMENT", 3500L, 
                       "18-50", Map.of("flavor", "Chocolate", "weight", "2kg")),
            
            new Product("P10034", "Yoga Mat", "HEALTH", "FITNESS", 2500L, 
                       "16-70", Map.of("color", "Purple", "thickness", "6mm")),
            
            new Product("P10035", "Smart Watch", "ELECTRONICS", "WEARABLE", 25000L, 
                       "16-65", Map.of("color", "Silver", "battery", "7-day")),
            
            new Product("P10036", "Coffee Maker", "APPLIANCES", "KITCHEN", 15000L, 
                       "25-70", Map.of("color", "Black", "capacity", "12-cup")),
            
            new Product("P10037", "Backpack", "FASHION", "ACCESSORIES", 5000L, 
                       "16-50", Map.of("color", "Gray", "capacity", "25L"))
        );

        productRepository.saveAll(sampleProducts);
    }
}
