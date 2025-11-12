# Product Services - Architecture Documentation

This document provides comprehensive architectural diagrams for the Product Services application, demonstrating clean architecture principles, design patterns, and caching strategies.

## 1. Overall System Architecture

```mermaid
graph TB
    subgraph "Presentation Layer"
        PC[ProductController]
        HC[HealthController]
    end
    
    subgraph "Service Layer"
        PS[ProductService]
        RS[RecommendationService]
    end
    
    subgraph "Cache Layer"
        CP[CacheProvider]
        CF[CacheFactory]
        subgraph "Cache Strategies"
            KVC[KeyValueCache<br/>Simple LRU Strategy]
            TBC[TypeBasedCache<br/>Type-Aware LRU]
            TAC[TypeAwareCache Interface<br/>Multi-Type Storage]
        end
    end
    
    subgraph "Data Access Layer"
        PR[ProductRepository]
    end
    
    subgraph "Database Layer"
        MongoDB[(Embedded MongoDB)]
    end
    
    subgraph "Configuration Layer"
        CC[CacheConfig]
        TCC[TypeCacheConfig]
        CProp[CacheProperties]
        DL[DataLoader]
        AppConfig[application.yml]
        SwaggerConfig[SwaggerConfig]
    end
    
    subgraph "API Documentation"
        OpenAPI[OpenAPI/Swagger UI]
        APIDoc[API Documentation]
    end
    
    subgraph "Domain Layer"
        P[Product Record]
        RQ[RecommendationQuery Record]
    end

    PC --> PS
    PC --> RS
    PS --> CP
    RS --> CP
    PS --> PR
    CP --> CF
    CF --> KVC
    CF --> TBC
    TBC --> TAC
    PR --> MongoDB
    CC --> CProp
    TCC --> CProp
    CProp --> AppConfig
    DL --> PR
    PS --> P
    RS --> P
    RS --> RQ
    SwaggerConfig --> OpenAPI
    PC --> OpenAPI
    APIDoc --> OpenAPI
    
    classDef controller fill:#000000,stroke:#ffffff,stroke-width:2px,color:#ffffff
    classDef service fill:#333333,stroke:#ffffff,stroke-width:2px,color:#ffffff
    classDef cache fill:#666666,stroke:#ffffff,stroke-width:2px,color:#ffffff
    classDef repository fill:#999999,stroke:#000000,stroke-width:2px,color:#000000
    classDef database fill:#ffffff,stroke:#000000,stroke-width:2px,color:#000000
    classDef config fill:#cccccc,stroke:#000000,stroke-width:2px,color:#000000
    classDef domain fill:#444444,stroke:#ffffff,stroke-width:2px,color:#ffffff
    
    class PC,HC controller
    class PS,RS service
    class CP,CF,KVC,TBC,TAC cache
    class PR repository
    class MongoDB database
    class CC,TCC,CProp,DL,AppConfig,SwaggerConfig config
    class P,RQ domain
    class OpenAPI,APIDoc config
```

## 2. Cache Strategy Pattern Implementation

```mermaid
classDiagram
    class Cache~K,V~ {
        <<interface>>
        +save(K k, V v) void
        +fetch(K k) Optional~V~
        +size() int
    }
    
    class TypeAwareCache~T,K~ {
        <<interface>>
        +save(T type, Iterable~K~ items) void
        +fetch(T type) Iterable~K~
        +totalSize() int
        +typeSize(T type) int
    }
    
    class KeyValueCache~K,V~ {
        -CacheConfig cacheConfig
        +save(K k, V v) void
        +fetch(K k) Optional~V~
        +size() int
        +removeEldestEntry(Entry eldest) boolean
    }
    
    class TypeBasedCache~T,K~ {
        -TypeCacheConfig cacheConfig
        -Map~T,KeyValueCache~K,Long~~ typeCaches
        -int totalItems
        +save(T type, Iterable~K~ items) void
        +fetch(T type) Iterable~K~
        +totalSize() int
        +typeSize(T type) int
        -evictGloballyIfNeeded() void
    }
    
    class CacheFactory {
        <<static>>
        +getCache(String name, CacheProperties props)$ Cache~K,V~
        +getTypeCache(String name, CacheProperties props)$ TypeAwareCache~T,K~
    }
    
    class CacheProvider {
        -CacheProperties cacheProperties
        -Cache~String,Product~ productIdCache
        -TypeAwareCache~String,Product~ typeCache
        -TypeAwareCache~String,Product~ recommendationCache
        +getProductIdCache() Cache~String,Product~
        +getTypeCache() TypeAwareCache~String,Product~
        +getRecommendationCache() TypeAwareCache~String,Product~
    }
    
    class CacheConfig {
        <<record>>
        +String name
        +int capacity
        +boolean enabled
    }
    
    class TypeCacheConfig {
        <<record>>
        +String name
        +int capacity
        +int count
        +boolean enabled
    }

    Cache~K,V~ <|.. KeyValueCache~K,V~
    TypeAwareCache~T,K~ <|.. TypeBasedCache~T,K~
    CacheFactory ..> Cache~K,V~ : creates
    CacheFactory ..> TypeAwareCache~T,K~ : creates
    CacheFactory ..> CacheConfig : uses
    CacheFactory ..> TypeCacheConfig : uses
    CacheProvider --> CacheFactory : uses
    KeyValueCache~K,V~ --> CacheConfig
    TypeBasedCache~T,K~ --> TypeCacheConfig
    TypeBasedCache~T,K~ --> KeyValueCache : contains
```

## 3. Data Flow Architecture

```mermaid
flowchart TD
    Client[Client Request]
    
    subgraph "API Layer"
        GetById["GET /api/products/{id}"]
        GetByType["GET /api/products/type/{type}"]
        GetAll["GET /api/products"]
        GetRec["GET /api/products/recommendations"]
    end
    
    subgraph "Service Processing"
        PS[ProductService]
        RS[RecommendationService]
    end
    
    subgraph "Cache Strategies"
        IDCache[ID Cache<br/>KeyValueCache]
        TypeCache[Type Cache<br/>TypeBasedCache]
        RecCache[Recommendation Cache<br/>TypeBasedCache]
    end
    
    subgraph "Database"
        MongoDB[(MongoDB)]
        Repository[ProductRepository]
    end
    
    subgraph "Filtering Pipeline"
        AllProducts[Get All Products]
        FilterType[Type Filter]
        FilterCategory[Category Filter]
        FilterPrice[Price Range Filter]
        FilterAge[Age Range Filter]
    end

    %% Client to API Layer
    Client --> GetById
    Client --> GetByType
    Client --> GetAll
    Client --> GetRec
    
    %% API to Services
    GetById --> PS
    GetByType --> PS
    GetAll --> PS
    GetRec --> RS
    
    %% Get By ID Flow
    PS --> IDCache
    IDCache -->|Cache Hit| Client
    IDCache -->|Cache Miss| Repository
    Repository --> MongoDB
    Repository -->|Product Found| IDCache
    Repository --> Client
    
    %% Get By Type Flow  
    PS --> TypeCache
    TypeCache -->|Cache Hit| Client
    TypeCache -->|Cache Miss| Repository
    Repository -->|Products by Type| TypeCache
    
    %% Get All Flow (No Cache)
    PS -->|findAll| Repository
    
    %% Recommendation Flow
    RS --> RecCache
    RecCache -->|Cache Hit| Client
    RecCache -->|Cache Miss| AllProducts
    AllProducts --> Repository
    AllProducts --> FilterType
    FilterType --> FilterCategory
    FilterCategory --> FilterPrice
    FilterPrice --> FilterAge
    FilterAge --> RecCache
    FilterAge --> Client

    %% Styling
    classDef api fill:#000000,stroke:#ffffff,stroke-width:3px,color:#ffffff
    classDef service fill:#333333,stroke:#ffffff,stroke-width:3px,color:#ffffff
    classDef cache fill:#666666,stroke:#ffffff,stroke-width:3px,color:#ffffff
    classDef database fill:#ffffff,stroke:#000000,stroke-width:3px,color:#000000
    classDef filter fill:#999999,stroke:#000000,stroke-width:3px,color:#000000
    
    class GetById,GetByType,GetAll,GetRec api
    class PS,RS service
    class IDCache,TypeCache,RecCache cache
    class MongoDB,Repository database
    class AllProducts,FilterType,FilterCategory,FilterPrice,FilterAge filter
```

## 4. Component Interaction Sequence

```mermaid
sequenceDiagram
    participant Client
    participant ProductController
    participant ProductService
    participant CacheProvider
    participant Cache
    participant ProductRepository
    participant MongoDB

    Note over Client, MongoDB: Product By ID Flow
    Client->>ProductController: GET /api/products/{id}
    ProductController->>ProductService: findById(productId)
    ProductService->>CacheProvider: getProductIdCache()
    CacheProvider-->>ProductService: Cache<String, Product>
    ProductService->>Cache: fetch(productId)
    
    alt Cache Hit
        Cache-->>ProductService: Optional.of(product)
        ProductService-->>ProductController: Optional.of(product)
        ProductController-->>Client: 200 OK + Product
    else Cache Miss
        Cache-->>ProductService: Optional.empty()
        ProductService->>ProductRepository: findById(productId)
        ProductRepository->>MongoDB: Query by ID
        MongoDB-->>ProductRepository: Product Document
        ProductRepository-->>ProductService: Optional.of(product)
        ProductService->>Cache: save(productId, product)
        ProductService-->>ProductController: Optional.of(product)
        ProductController-->>Client: 200 OK + Product
    end
```

## 5. Recommendation Engine Flow

```mermaid
flowchart TD
    ReqStart[Recommendation Request]
    
    subgraph "Input Validation"
        ParseParams[Parse Query Parameters]
        ValidateAge[Validate Age Range]
        ValidatePrice[Validate Price Range]
    end
    
    subgraph "Cache Strategy"
        BuildKey[Build Composite Cache Key]
        CheckCache{Check Recommendation Cache}
    end
    
    subgraph "Filtering Pipeline"
        GetAllProducts[Get All Products]
        TypeFilter[Type Filter]
        CategoryFilter[Category Filter]
        PriceFilter[Price Range Filter]
        AgeFilter[Age Range Filter<br/>Java 17 Pattern Matching]
    end
    
    subgraph "Age Range Processing"
        ParseAgeRange[Parse Age Range<br/>Regex: \d+-\d+]
        AgeValidation{Age in Range?}
    end
    
    subgraph "Response Processing"
        CacheResult[Cache Filtered Results]
        ReturnResults[Return Product List]
    end

    ReqStart --> ParseParams
    ParseParams --> ValidateAge
    ValidateAge --> ValidatePrice
    ValidatePrice --> BuildKey
    BuildKey --> CheckCache
    
    CheckCache -->|Cache Hit| ReturnResults
    CheckCache -->|Cache Miss| GetAllProducts
    
    GetAllProducts --> TypeFilter
    TypeFilter --> CategoryFilter
    CategoryFilter --> PriceFilter
    PriceFilter --> AgeFilter
    
    AgeFilter --> ParseAgeRange
    ParseAgeRange --> AgeValidation
    AgeValidation -->|Yes| CacheResult
    AgeValidation -->|No| CacheResult
    
    CacheResult --> ReturnResults
```

## 6. LRU Cache Eviction Strategy

```mermaid
graph TD
    subgraph "KeyValueCache (Simple LRU)"
        KV_Insert[Insert Key-Value]
        KV_Check{Size > Capacity?}
        KV_Evict[Remove Eldest Entry]
        KV_Store[Store in LinkedHashMap]
    end
    
    subgraph "MultiValueCache (Type-Based LRU)"
        MV_Insert[Insert Key-Values]
        MV_Count[Update Total Value Count]
        MV_Check{Total Values > Capacity?}
        MV_EvictKey[Remove Entire Key<br/>with All Values]
        MV_Store[Store in Partitioned Cache]
    end
    
    subgraph "Recommendation Cache (Composite-Key LRU)"
        RC_BuildKey[Build Composite Key<br/>from Query Parameters]
        RC_Insert[Insert Filtered Results]
        RC_Evict[LRU Eviction by Query Key]
    end

    KV_Insert --> KV_Check
    KV_Check -->|Yes| KV_Evict
    KV_Check -->|No| KV_Store
    KV_Evict --> KV_Store
    
    MV_Insert --> MV_Count
    MV_Count --> MV_Check
    MV_Check -->|Yes| MV_EvictKey
    MV_Check -->|No| MV_Store
    MV_EvictKey --> MV_Store
    
    RC_BuildKey --> RC_Insert
    RC_Insert --> RC_Evict
```

## 7. SOLID Principles Implementation

```mermaid
graph TD
    Root["SOLID Principles"]
    
    S["S - Single Responsibility"]
    O["O - Open/Closed"]
    L["L - Liskov Substitution"]
    I["I - Interface Segregation"]
    D["D - Dependency Inversion"]
    
    PC["Controllers handle HTTP"]
    PS["Services handle business logic"]
    CP["Manages cache instances"]
    PR["Data access only"]
    
    CI1["Cache interface for extension"]
    SP["New cache strategies without modification"]
    
    CImpl["All cache implementations interchangeable"]
    
    CI2["Focused cache interface"]
    RI["Specific repository methods"]
    
    SD["Services depend on abstractions"]
    CInj["Configuration through interfaces"]

    Root --> S
    Root --> O
    Root --> L
    Root --> I
    Root --> D
    
    S --> PC
    S --> PS
    S --> CP
    S --> PR
    
    O --> CI1
    O --> SP
    
    L --> CImpl
    
    I --> CI2
    I --> RI
    
    D --> SD
    D --> CInj

    %% Styling
    classDef root fill:#000000,stroke:#ffffff,stroke-width:3px,color:#ffffff
    classDef principle fill:#333333,stroke:#ffffff,stroke-width:2px,color:#ffffff
    classDef implementation fill:#666666,stroke:#ffffff,stroke-width:2px,color:#ffffff
    
    class Root root
    class S,O,L,I,D principle
    class PC,PS,CP,PR,CI1,SP,CImpl,CI2,RI,SD,CInj implementation
```

## Key Architectural Decisions

### 1. **Layered Architecture**
- Clear separation of concerns
- Each layer has single responsibility
- Dependencies flow downward only

### 2. **Dual Cache Strategy Pattern**
- **KeyValueCache**: Simple LinkedHashMap-based LRU for individual items
- **TypeBasedCache**: HashMap-based multi-type storage with global eviction
- Factory creates appropriate cache implementation based on configuration
- Runtime cache strategy selection via configuration

### 3. **TypeAware Caching Interface**
- Abstraction for type-organized caching operations
- Support for bulk operations with Iterable inputs/outputs
- Flexible eviction policies across type boundaries
- Per-type capacity and global count management

### 4. **Java 17 Features**
- Records for immutable domain models (Product, RecommendationQuery, CacheConfig)
- Pattern matching for age range parsing in recommendation engine
- Modern language constructs for cleaner, more expressive code

### 5. **Thread Safety**
- Synchronized cache operations for concurrent access
- HashMap usage with synchronization in TypeBasedCache
- Thread-safe LRU implementations

### 6. **Interactive API Documentation**
- SpringDoc OpenAPI 3 integration
- Swagger UI for real-time API testing
- Comprehensive endpoint documentation with examples

### 7. **Configuration-Driven Design**
- Separate configuration structures for different cache types
- Environment-specific settings via application.yml
- Runtime behavior modification without code changes
