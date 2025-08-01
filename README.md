# Golden Raspberry Awards API - Implementation Guide

## Overview
This is a RESTful API built with Spring Boot 3.3.0 and Java 23 that manages Golden Raspberry Awards data, specifically focusing on producer award intervals. The API reads movie data from a CSV file and provides endpoints to analyze producer award patterns.

## Architecture

The project follows Clean Architecture principles with clear separation of concerns:

```
src/
├── main/
│   ├── java/com/emikaelsilveira/goldenraspberry/
│   │   ├── GoldenRaspberryApplication.java     # Main application entry point
│   │   ├── controller/
│   │   │   └── ProducerController.java         # REST API endpoints
│   │   ├── service/
│   │   │   ├── MovieService.java               # Business logic
│   │   │   └── CsvReaderService.java           # CSV data processing
│   │   ├── repository/
│   │   │   └── MovieRepository.java            # Data access layer
│   │   ├── entity/
│   │   │   └── Movie.java                      # JPA entity
│   │   ├── dto/
│   │   │   ├── ProducerIntervalDto.java        # Data transfer objects
│   │   │   └── ProducerInterval.java           # Producer interval data
│   │   ├── config/
│   │   │   ├── SecurityConfig.java             # Security configuration
│   │   │   └── filter/
│   │   │       ├── ApiKeyAuthFilter.java       # API key authentication filter
│   │   │       └── ApiKeyAuthentication.java   # API key authentication object
│   │   ├── exception/
│   │   │   ├── CSVReaderException.java         # CSV processing exceptions
│   │   │   └── H2DatabaseException.java        # Database exceptions
│   │   └── utilities/
│   │       └── Utils.java                      # Utility functions
│   └── resources/
│       ├── application.yml                     # Application configuration
│       ├── application-dev.yml                 # Development profile
│       ├── application-prod.yml                # Production profile
│       ├── application-test.yml                # Test profile
│       └── movielist.csv                       # Sample movie data
└── test/
    └── java/com/emikaelsilveira/goldenraspberry/
        └── integration/
            ├── ProducerControllerIntegrationTest.java  # API endpoint tests
            ├── ApiContractIntegrationTest.java         # API contract tests
            ├── BusinessLogicIntegrationTest.java       # Business logic tests
            ├── CsvProcessingIntegrationTest.java       # CSV processing tests
            ├── DataLayerIntegrationTest.java           # Data layer tests
            ├── ErrorHandlingIntegrationTest.java       # Error handling tests
            ├── SecurityIntegrationTest.java            # Security tests
            └── TransactionIntegrationTest.java         # Transaction tests
```

## Getting Started

### Prerequisites
- Java 23 (JDK 23)
- Maven 3.9+ (or use included wrapper)

### Running the Application

#### Using Maven Wrapper (Recommended)
```bash
# Build and run tests
./mvnw clean test

# Run the application
./mvnw spring-boot:run
```

#### Using Docker
```bash
# Build Docker image
docker build -t golden-raspberry-api .

# Run container
docker run -p 8080:8080 golden-raspberry-api
```

### API Endpoints

#### Get Producer Intervals
Returns producers with minimum and maximum intervals between consecutive awards.

**Endpoint:** `GET /api/producers/intervals`

**Response Format:**
```json
{
  "min": [
    {
      "producer": "Producer Name",
      "interval": 1,
      "previousWin": 1990,
      "followingWin": 1991
    }
  ],
  "max": [
    {
      "producer": "Another Producer",
      "interval": 13,
      "previousWin": 1980,
      "followingWin": 1993
    }
  ]
}
```

**Example Usage:**
```bash
curl -X GET "http://localhost:8080/api/producers/intervals" \
     -H "Accept: application/json" \
     -H "x-api-key: {{$X_API_KEY}}"
```

#### Health Check
**Endpoint:** `GET /actuator/health`

Returns application health status.

## Configuration

### Application Properties
Key configuration options in `application.yml`:

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:h2:mem:goldenraspberry
    driver-class-name: org.h2.Driver
    username: {{$DB_USERNAME}}
    password: {{$DB_PASSWORD}}

# Server Configuration
server:
  port: 8080

# CSV Data Configuration
app:
  csv:
    file-path: movielist.csv
    encoding: UTF-8
```

### Environment Profiles
- **Development** (`dev`): Enhanced logging, H2 console enabled
- **Test** (`test`): Minimal logging, separate test database
- **Production** (`prod`): Optimized for production, security hardened

## Data Processing

### CSV Format
The application expects CSV data with the following structure:
```csv
year;title;studios;producers;winner
1980;Movie Title;Studio Name;Producer Name;yes
```

### Producer Name Parsing
The system intelligently parses producer names that may be:
- Single producer: `John Doe`
- Multiple producers: `John Doe, Jane Smith`
- Complex formats: `John Doe and Jane Smith & Bob Wilson`

### Interval Calculation
1. Extracts all winning movies and their producers
2. Groups movies by producer name
3. Calculates intervals between consecutive wins
4. Identifies minimum and maximum intervals
5. Returns all producers with min/max intervals

## Security Features

### API Key Authentication
- **Authentication Method**: Custom API key via `x-api-key` header
- **Required for**: All producer endpoints (`/api/producers/*`)
- **Public Access**: Only actuator endpoints (`/actuator/*`)
- **Filter Implementation**: Custom `ApiKeyAuthFilter` with Spring Security integration

### Spring Security Configuration
- **CSRF Protection**: Disabled for API usage
- **Headers Security**: Frame options disabled for H2 console access
- **Stateless Sessions**: No server-side sessions for REST API
- **Authentication Filter**: Custom API key authentication before standard filters
- **Actuator Security**: Health endpoints are public, producer endpoints require API key

### Security Headers
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Strict-Transport-Security`
- `Referrer-Policy: strict-origin-when-cross-origin`

## Testing

### Integration Tests
Comprehensive integration tests cover:
- **API Contract Tests**: Endpoint functionality and response validation
- **Business Logic Tests**: Core producer interval calculation logic
- **CSV Processing Tests**: Data import and parsing validation
- **Data Layer Tests**: Repository and entity persistence
- **Error Handling Tests**: Exception scenarios and error responses
- **Security Tests**: Authentication and authorization validation
- **Transaction Tests**: Database transaction integrity
- **Producer Controller Tests**: Complete API endpoint integration

**Running Tests:**
```bash
./mvnw test
```

**Test Coverage:**
```bash
./mvnw jacoco:report
# View report at target/site/jacoco/index.html
```

### Test Data
The test suite uses sample data with known producers and expected intervals:
- Allan Carr (1980, 1984) - 4-year interval
- Joel Silver (1989, 1990) - 1-year interval
- Other producers with various intervals

## Performance Considerations

### Database Optimization
- **Indexing**: Strategic indexes on year, winner status
- **Connection Pooling**: HikariCP for efficient connections
- **JPA Optimization**: Lazy loading, query optimization

### Memory Management
- **Java 23 Features**: Modern GC algorithms, container support
- **JVM Tuning**: Optimized heap sizes for containerized environments
- **Connection Limits**: Appropriate database connection pooling

### Caching Strategy
- **In-Memory Caching**: Spring Cache abstraction ready
- **HTTP Caching**: Cache headers for static responses
- **Application-Level**: Cacheable service methods

## Monitoring & Observability

### Spring Boot Actuator
Available endpoints:
- `/actuator/health` - Application health
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Logging
Structured logging with configurable levels:
- **Console Output**: Development-friendly format
- **File Output**: Production-ready with rotation
- **Levels**: DEBUG, INFO, WARN, ERROR per package

## Docker Support

### Multi-Stage Build
```dockerfile
# Build stage with JDK 23
FROM eclipse-temurin:23-jdk as builder
# ... build process

# Runtime stage with JRE 23
FROM eclipse-temurin:23-jdk
# ... runtime setup
```

### Security Features
- **Non-root user**: Application runs as `appuser`
- **Health checks**: Built-in Docker health monitoring
- **Minimal base image**: Reduced attack surface

### Environment Variables
```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
SPRING_PROFILES_ACTIVE=prod
```

## Development Workflow

### Building
```bash
# Clean build
./mvnw clean compile

# Run tests
./mvnw test

# Package application
./mvnw package

# Skip tests (development only)
./mvnw package -DskipTests
```

### Development Profile
```bash
# Run with development profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Features enabled in development:
- H2 Console at `/h2-console`
- Enhanced SQL logging
- Debug-level logging
- Hot reload support

## 📋 API Specification

### Richardson Maturity Model - Level 2
- **Resources**: Clear resource-based URLs
- **HTTP Methods**: Proper use of GET, POST, PUT, DELETE
- **Status Codes**: Appropriate HTTP status codes
- **Content Types**: JSON request/response format

### HTTP Status Codes
- `200 OK`: Successful GET requests
- `403 Forbiddeen`: Request isn't authorized
- `405 Method Not Allowed`: Unsupported HTTP methods
- `500 Internal Server Error`: Server-side errors

### Content Negotiation
- **Accept**: `application/json`
- **Content-Type**: `application/json`

## 🏭 Production Considerations

### Deployment Checklist
- [ ] Java 23 runtime available
- [ ] Environment-specific configuration
- [ ] Health check endpoints configured
- [ ] Monitoring and logging setup
- [ ] Security headers verified
- [ ] Database connections tested

### Scaling Considerations
- **Stateless Design**: Easy horizontal scaling
- **Database**: Consider read replicas for high load
- **Caching**: Redis/Hazelcast for distributed caching
- **Load Balancing**: Multiple instance support

### Monitoring
- **APM Tools**: Integrate with New Relic, DataDog, etc.
- **Log Aggregation**: ELK Stack, Splunk integration
- **Metrics**: Prometheus + Grafana dashboards
- **Alerting**: Health check failures, error rate spikes

## Troubleshooting

### Common Issues

#### Java Version Mismatch
```bash
# Check Java version
java -version
# Should show Java 23

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java23
```

#### Port Already in Use
```bash
# Change port in application.yml
server:
  port: 8081

# Or use environment variable
SERVER_PORT=8081 ./mvnw spring-boot:run
```

#### CSV Data Not Loading
- Verify `movielist.csv` exists in `src/main/resources/`
- Check CSV format uses semicolon (;) separators, not commas
- Review application logs for parsing errors

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Reference](https://spring.io/projects/spring-security)
- [H2 Database Documentation](http://h2database.com/html/main.html)
- [Java 23 Documentation](https://docs.oracle.com/en/java/javase/23/)
- [Richardson Maturity Model](https://martinfowler.com/articles/richardsonMaturityModel.html)
