# Requirements Analysis and Architecture

## Project Overview
This project requires developing a RESTful API to read and manage Golden Raspberry Awards data, specifically focusing on worst movie winners and their producers' award intervals.

## Core Requirements Analysis

### Functional Requirements
1. **CSV Data Processing**: Read CSV file containing movie awards data on application startup
2. **Database Integration**: Store data in an embedded in-memory database
3. **Producer Analytics**: Calculate intervals between consecutive awards for producers
4. **REST API**: Provide endpoint to retrieve min/max interval data in specified JSON format

### Non-Functional Requirements
1. **Richardson Maturity Model Level 2**: Proper HTTP methods, status codes, and resource-based URLs
2. **Integration Testing**: Comprehensive tests validating data accuracy
3. **Embedded Database**: H2 in-memory database (no external dependencies)
4. **Security**: Protection against web injections and common attacks
5. **Clean Code**: Following 12-factor principles and clean architecture

## Recommended Technology Stack

### Core Framework: **Spring Boot 3.x**
**Rationale**: 
- Excellent for RESTful services with built-in Richardson Level 2 support
- Strong security features with Spring Security
- Comprehensive testing support
- Mature ecosystem with extensive documentation
- Built-in dependency injection and configuration management

### Database Layer
- **H2 Database**: In-memory embedded database as specified
- **Spring Data JPA**: For clean data access patterns and reduced boilerplate
- **Hibernate**: As JPA implementation for robust ORM capabilities

### Security Framework: **Spring Security 6.x**
**Features**:
- CSRF protection
- SQL injection prevention through JPA
- Input validation and sanitization
- Security headers configuration
- Rate limiting capabilities

### Testing Framework
- **Spring Boot Test**: For integration testing
- **TestContainers**: For more realistic testing scenarios if needed
- **WireMock**: For external service mocking
- **JUnit 5**: Modern testing framework

### Build Tool: **Maven** or **Gradle**
**Recommendation**: Maven for simplicity and wide adoption

## Architecture Design

### Clean Architecture Layers

```
┌─────────────────────────────────────┐
│           Controller Layer          │ ← REST endpoints
├─────────────────────────────────────┤
│            Service Layer            │ ← Business logic
├─────────────────────────────────────┤
│          Repository Layer           │ ← Data access
├─────────────────────────────────────┤
│             Entity Layer            │ ← Domain models
└─────────────────────────────────────┘
```

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/company/goldenraspberry/
│   │       ├── GoldenRaspberryApplication.java
│   │       ├── controller/
│   │       │   └── ProducerController.java
│   │       ├── service/
│   │       │   ├── MovieService.java
│   │       │   └── CsvReaderService.java
│   │       ├── repository/
│   │       │   └── MovieRepository.java
│   │       ├── entity/
│   │       │   └── Movie.java
│   │       ├── dto/
│   │       │   └── ProducerIntervalDto.java
│   │       └── config/
│   │           └── SecurityConfig.java
│   └── resources/
│       ├── application.yml
│       └── data.csv
└── test/
    └── java/
        └── integration/
            └── ProducerControllerIntegrationTest.java
```

## 12-Factor App Compliance

### 1. Codebase
- Single codebase in Git repository
- Multiple deployment environments support

### 2. Dependencies
- Maven/Gradle for explicit dependency management
- No system-wide packages dependencies

### 3. Config
- Environment-specific configuration via `application.yml`
- Externalized configuration support

### 4. Backing Services
- H2 database treated as attached resource
- Easy to swap between environments

### 5. Build, Release, Run
- Clear separation of build and run stages
- Spring Boot executable JAR

### 6. Processes
- Stateless application design
- Session data stored externally if needed

### 7. Port Binding
- Self-contained HTTP service
- Embedded Tomcat server

### 8. Concurrency
- Thread-safe service layer
- Stateless request handling

### 9. Disposability
- Fast startup and graceful shutdown
- Spring Boot's lifecycle management

### 10. Dev/Prod Parity
- Same H2 database in all environments
- Docker containerization for consistency

### 11. Logs
- Structured logging with SLF4J/Logback
- Log streaming capabilities

### 12. Admin Processes
- Spring Boot Actuator for health checks
- Database initialization as startup process

## Security Measures

### Input Validation
- Bean Validation (JSR-303) annotations
- Custom validators for business rules
- Request body size limits

### SQL Injection Prevention
- JPA/Hibernate parameterized queries
- Input sanitization
- Repository pattern abstraction

### General Security
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .headers(headers -> headers.frameOptions().deny())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .build();
    }
}
```

## Performance Considerations

### Database Optimization
- Proper indexing on query fields
- Connection pooling (HikariCP)
- Lazy loading configuration

### Caching Strategy
- Spring Cache abstraction
- In-memory caching for static data
- HTTP caching headers

### Monitoring
- Spring Boot Actuator metrics
- Application performance monitoring
- Health check endpoints

## Testing Strategy

### Integration Testing Focus
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
class ProducerControllerIntegrationTest {
    
    @Test
    void shouldReturnCorrectProducerIntervals() {
        // Test implementation following given data specification
    }
}
```

### Test Coverage Areas
- Data loading and CSV parsing
- Producer interval calculations
- API response format validation
- Error handling scenarios
- Security vulnerability tests

## Development Best Practices

### Clean Code Principles
- Single Responsibility Principle
- Dependency Inversion
- Clear method and variable naming
- Comprehensive documentation
- SOLID principles adherence

### Error Handling
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("Data processing error"));
    }
}
```

## Deployment Considerations

### Containerization
- Docker support with multi-stage builds
- Health check endpoints
- Graceful shutdown handling

### Environment Configuration
- Profile-based configuration
- External configuration support
- Secret management integration

## Recommended Implementation Order

1. **Setup Project Structure**: Maven/Gradle + Spring Boot
2. **Entity & Repository Layer**: Movie entity and JPA repository
3. **CSV Reading Service**: Data loading on startup
4. **Business Logic Service**: Producer interval calculations
5. **REST Controller**: API endpoint implementation
6. **Security Configuration**: Basic security setup
7. **Integration Tests**: Comprehensive test coverage
8. **Documentation**: README and API documentation

This architecture ensures scalability, maintainability, security, and adherence to industry best practices while meeting all specified requirements.