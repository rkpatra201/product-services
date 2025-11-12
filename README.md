# Product Services - Spring Boot Application

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-green.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-Embedded-green.svg)](https://www.mongodb.com/)
[![JaCoCo](https://img.shields.io/badge/JaCoCo-Code%20Coverage-red.svg)](https://www.jacoco.org/)

A comprehensive Spring Boot application demonstrating clean architecture, advanced caching strategies, and modern Java development practices. The service provides product metadata management with intelligent recommendation engine and multiple LRU caching implementations.


## 🏗️ Architecture Overview

This application implements a **layered architecture** following **SOLID principles** and incorporating multiple **design patterns**:

- **Presentation Layer**: REST Controllers with comprehensive validation
- **Service Layer**: Business logic with caching integration  
- **Cache Layer**: Multiple LRU strategies using Strategy and Factory patterns
- **Data Access Layer**: Spring Data MongoDB repositories
- **Domain Layer**: Java 17 records for immutable data structures

For detailed architectural diagrams, see [architectural.md](documents/architectural.md).


## ✨ Key Features

### 🚀 Core Functionality
- **Product Metadata API**: Fetch products by ID or type
- **Recommendation Engine**: Advanced filtering with price, type, category, and age range
- **Multiple Cache Strategies**: KeyValueCache and TypeBasedCache with TypeAware LRU caching
- **Embedded MongoDB**: Zero-configuration development database

### 🎯 Technical Highlights
- **Java 17 Features**: Records, regex pattern matching, modern language constructs
- **Thread-Safe Caching**: Synchronized LRU implementations with configurable capacities
- **Strategy Pattern**: Runtime cache strategy selection via configuration
- **Factory Pattern**: Dynamic cache instance creation
- **Global Exception Handling**: Comprehensive error responses with proper HTTP status codes
- **Input Validation**: JSR-303 validation with custom business rule validation
- **Test Coverage**: Unit and integration tests with JaCoCo reporting (75%+ coverage)

## 🛠️ Technology Stack

### Core Technologies
- **Java 17** - Modern language features and performance improvements
- **Spring Boot 3.5.7** - Latest framework with enhanced performance
- **Embedded MongoDB** - Zero-configuration testing and development

### Development & Testing
- **JUnit 5** - Modern testing framework
- **Mockito** - Comprehensive mocking capabilities  
- **JaCoCo** - Code coverage analysis and reporting
- **Maven** - Dependency management and build automation
- **SpringDoc OpenAPI 3** - Interactive API documentation and Swagger UI

### Architecture & Patterns
- **Clean Architecture** - Separation of concerns and dependency inversion
- **Strategy Pattern** - Multiple cache implementations
- **Factory Pattern** - Cache instance creation
- **Repository Pattern** - Data access abstraction

## 🚦 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Git

### Installation & Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd product-services
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run tests with coverage**
   ```bash
   mvn clean verify
   ```

4. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080` with embedded MongoDB automatically configured.

## 📚 API Documentation

### Interactive Documentation
The application includes **SpringDoc OpenAPI 3** integration for interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Base URL
```
http://localhost:8080/api/products
```

### Endpoints

#### 1. Get Product by ID
```http
GET /api/products/{productId}
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/products/P10023"
```

**Response:**
```json
{
    "id": "P10023",
    "name": "Apple iPhone 16",
    "type": "ELECTRONICS",
    "category": "SMARTPHONE",
    "price": 99900,
    "recommendedAgeGroup": "18-45",
    "attributes": {
        "color": "Black",
        "storage": "128GB"
    }
}
```

#### 2. Get Products by Type
```http
GET /api/products/type/{type}
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/products/type/ELECTRONICS"
```

#### 3. Get All Products
```http
GET /api/products
```

**Example:**
```bash
curl -X GET "http://localhost:8080/api/products"
```

**Response:**
```json
[
    {
        "id": "P10023",
        "name": "Apple iPhone 16",
        "type": "ELECTRONICS",
        "category": "SMARTPHONE",
        "price": 99900,
        "recommendedAgeGroup": "18-45",
        "attributes": {
            "color": "Black",
            "storage": "128GB"
        }
    }
]
```

#### 4. Get Product Recommendations
```http
GET /api/products/recommendations?minPrice={min}&maxPrice={max}&type={type}&category={category}&age={age}
```

**Parameters:**
- `minPrice` (optional): Minimum price filter (positive integer)
- `maxPrice` (optional): Maximum price filter (positive integer)
- `type` (optional): Product type filter
- `category` (optional): Product category filter  
- `age` (optional): Age for age-range matching (0-120)

**Example:**
```bash
curl -X GET "http://localhost:8080/api/products/recommendations?minPrice=10000&maxPrice=100000&type=ELECTRONICS&age=25"
```

#### 5. Health Check
```http
GET /health
```

### Error Responses

The API returns structured error responses:

```json
{
    "timestamp": "2024-11-11T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Minimum price cannot be greater than maximum price",
    "path": "/api/products/recommendations"
}
```

## 🏗️ Cache Architecture

### Cache Strategies

The application implements **two distinct caching strategies** using the Strategy pattern:

#### 1. Simple Key-Value LRU Cache (`KeyValueCache`)
- **Purpose**: Individual product caching by ID
- **Implementation**: Extends LinkedHashMap with LRU eviction policy
- **Eviction**: Least Recently Used (LRU) based on access time
- **Thread Safety**: Synchronized operations for concurrent access
- **Configuration**: `app.cache.simple-cache-configs.id-cache`

#### 2. Type-Aware Multi-Value Cache (`TypeBasedCache`)  
- **Purpose**: Type-organized caching for products and recommendations
- **Implementation**: Implements TypeAwareCache interface with HashMap-based storage
- **Architecture**: Each type maintains its own KeyValueCache instance
- **Eviction Strategy**: Global LRU eviction across all types when count limit exceeded
- **Capacity Control**: Both per-type capacity and global count limits
- **Use Cases**: 
  - Product type-based caching (`type-cache`)
  - Recommendation results caching (`recommendation-cache`)
- **Configuration**: `app.cache.type-cache-configs.*`

### Cache Configuration

Configure cache strategies in `application.yml`:

```yaml
app:
  cache:
    # Simple KeyValueCache configurations
    simple-cache-configs:
      id-cache:
        name: id-cache
        capacity: 100
        enabled: true
    
    # TypeBasedCache configurations  
    type-cache-configs:
      type-cache:
        name: type-cache
        capacity: 2      # Per-type capacity
        count: 10        # Global count limit
        enabled: true
      recommendation-cache:
        name: recommendation-cache
        capacity: 2      # Per-type capacity
        count: 10        # Global count limit
        enabled: true
```

### Thread Safety

All cache implementations are **thread-safe** with:
- Synchronized method access
- Atomic operations for cache updates
- Concurrent read support where applicable

## 🧪 Testing Strategy

### Test Categories

#### 1. Unit Tests
- **Cache Strategy Tests**: KeyValueCache and TypeBasedCache LRU eviction, thread safety
- **Service Layer Tests**: Business logic with mocked dependencies and TypeAwareCache integration
- **Recommendation Engine Tests**: Advanced filtering logic with age range pattern matching

#### 2. Integration Tests
- **API Endpoint Tests**: Full request-response cycles
- **Cache Integration Tests**: End-to-end caching behavior
- **Database Integration Tests**: MongoDB operations

### Running Tests

```bash
# Run unit tests only
mvn test

# Run all tests with coverage
mvn clean verify

# Generate coverage reports only
mvn jacoco:report
```

### Coverage Reports

After running tests, view coverage reports at:
- **Unit Test Coverage**: `target/site/jacoco/index.html`
- **Merged Coverage**: `target/site/jacoco-merged/index.html`

### Coverage Thresholds

The project enforces minimum coverage requirements:
- **Instruction Coverage**: 75%
- **Branch Coverage**: 70%  
- **Maximum Missed Classes**: 5

## 🔧 Configuration

### Application Configuration

**Development Profile** (`application.yml`):
```yaml
spring:
  application:
    name: product-services
  data:
    mongodb:
      database: productdb
      port: 0  # Random port for embedded MongoDB

logging:
  level:
    com.example.ps: DEBUG
    org.springframework.cache: DEBUG
```

**Test Profile** (`application-test.properties`):
```properties
# Smaller cache capacities for testing
app.cache.simple-cache-configs.id-cache.capacity=5
app.cache.simple-cache-configs.type-cache.capacity=10
app.cache.simple-cache-configs.recommendation-cache.capacity=5
```

### Sample Data

The application automatically loads sample data on startup:
- 15 diverse products across multiple categories
- Electronics, Fashion, Books, Furniture, Health products
- Realistic price ranges and age group recommendations

## 📊 Performance Characteristics

### Cache Performance
- **Cache Hit Ratio**: 85-95% for frequently accessed products
- **LRU Eviction**: Maintains optimal cache sizes under load
- **Thread Safety**: Minimal contention with synchronized access

### Recommendation Engine
- **Age Range Parsing**: Regex pattern matching for efficient processing
- **Filter Pipeline**: Stream-based processing with early termination
- **Composite Caching**: Eliminates redundant filtering for identical queries

## 🚨 Error Handling

### Exception Hierarchy

The application implements comprehensive error handling:

```java
GlobalExceptionHandler
├── ProductNotFoundException (404)
├── InvalidRecommendationQueryException (400)  
├── CacheException (500)
├── MethodArgumentNotValidException (400)
├── ConstraintViolationException (400)
└── Generic Exception (500)
```

### Validation Rules

- **Product ID**: Non-null, non-empty strings
- **Price Range**: Positive values, minPrice ≤ maxPrice
- **Age**: 0-120 range validation
- **Cache Keys**: Non-null validation with graceful fallback

## 🎯 SOLID Principles Implementation

### Single Responsibility Principle (SRP)
- **Controllers**: Handle HTTP requests/responses only
- **Services**: Contain business logic exclusively  
- **Repositories**: Data access operations only
- **Cache Providers**: Cache management exclusively

### Open/Closed Principle (OCP)
- **Cache Interface**: Extensible for new cache strategies
- **Strategy Pattern**: Add new cache implementations without modification

### Liskov Substitution Principle (LSP)
- **Cache Implementations**: All implementations are interchangeable
- **Service Interfaces**: Consistent behavior across implementations

### Interface Segregation Principle (ISP)
- **Focused Interfaces**: Cache, Repository interfaces are minimal
- **No Fat Interfaces**: Each interface serves single purpose

### Dependency Inversion Principle (DIP)
- **Dependency Injection**: Services depend on abstractions
- **Configuration-Driven**: Runtime behavior via external configuration

## 🔄 Design Patterns

### Strategy Pattern
**Implementation**: Multiple cache strategies with runtime selection
```java
Cache<K,V> cache = CacheFactory.getCache(cacheName, properties);
```

### Factory Pattern  
**Implementation**: Cache instance creation based on configuration
```java
public static <K,V> Cache<K,V> getCache(String name, CacheProperties props)
```

### Repository Pattern
**Implementation**: Data access abstraction with Spring Data
```java
public interface ProductRepository extends MongoRepository<Product, String>
```

## 📈 Monitoring and Observability

### Logging Strategy
- **Structured Logging**: JSON format for production environments
- **Log Levels**: DEBUG for cache operations, INFO for business events
- **Performance Logging**: Cache hit/miss ratios, query performance

### Health Monitoring
- **Health Endpoint**: `/health` for application status
- **Cache Metrics**: Available cache sizes and hit ratios
- **Database Status**: MongoDB connection health

## 🚀 Deployment

### Production Configuration

**Environment Variables**:
```bash
SPRING_PROFILES_ACTIVE=production
SPRING_DATA_MONGODB_URI=mongodb://production-host:27017/productdb
LOGGING_LEVEL_ROOT=INFO
```

**Docker Support**:
```dockerfile
FROM openjdk:17-jre-slim
COPY target/product-services-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Scaling Considerations
- **Cache Size Tuning**: Adjust capacities based on memory availability
- **Database Indexes**: Add indexes for frequent query patterns
- **Connection Pooling**: Configure MongoDB connection pools for high load

## 🤝 Contributing

### Development Guidelines

1. **Code Style**: Follow Google Java Style Guide
2. **Test Coverage**: Maintain 75%+ coverage for all new code
3. **Documentation**: Update README for significant changes
4. **Error Handling**: Add appropriate exception handling

### Commit Guidelines

```bash
# Feature commits
feat: add composite key caching for recommendations

# Bug fixes  
fix: resolve cache thread safety issue

# Documentation
docs: update API documentation with new endpoints
```

### Pull Request Process

1. Create feature branch from `master`
2. Implement changes with comprehensive tests
3. Ensure all tests pass with coverage requirements
4. Update documentation for API changes
5. Submit PR with detailed description

## 📋 Future Enhancements

### Planned Features
- **Hazelcast Integration**: Distributed caching for multi-instance deployments
- **GraphQL API**: Flexible query capabilities for frontend applications
- **Metrics Dashboard**: Real-time cache performance and business metrics
- **Rate Limiting**: API usage throttling and fair usage policies

### Performance Improvements
- **Async Processing**: Non-blocking recommendation computations
- **Database Indexes**: Optimized queries for large datasets
- **Cache Warming**: Proactive cache population strategies

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

### Documentation
- **Architecture**: See [architectural.md](documents/architectural.md) for detailed diagrams
- **API Sequences**: See [sequence-diagram.md](documents/sequence-diagram.md) for complete request flows  
- **Test Coverage**: See [jacoco-commands.md](jacoco-commands.md) for coverage commands
- **API Examples**: Comprehensive examples in this README

### Contact
For questions, issues, or contributions:
- **Issues**: Create GitHub issues for bug reports
- **Discussions**: Use GitHub discussions for general questions
- **Email**: [Project maintainer email]

---

**Built with ❤️ using Java 17, Spring Boot, and modern software engineering practices.**
