# Event Gateway Service

Public-facing API for the Event Ledger system. Receives financial transaction events, enforces idempotency, validates input, and delegates transaction processing to the Account Service with built-in resilience patterns.

## 📋 Overview

The Event Gateway Service is the entry point for all client requests. It handles:
- Event submission and validation
- Idempotency (duplicate detection)
- Event storage and querying
- Circuit breaker resilience
- Trace propagation to Account Service

**Port:** 8011 (public)  
**Database:** H2 (in-memory)  
**Framework:** Spring Boot 3.5.14  
**Resilience:** Resilience4j Circuit Breaker

---

## 🏗️ Architecture

```
Client / Browser
        │
        │ REST Request
        ▼
Event Gateway Service
├── EventController
├── EventService
├── EventRepository
├── Circuit Breaker
├── H2 Database (Events)
│   └── Event (id, eventId, accountId, type, amount, status)
        │
        │ REST Call (with Circuit Breaker)
        │ Trace ID propagation
        ▼
Account Service
(Balance calculation & transaction history)
```

### Responsibilities

- ✅ Receive and validate events
- ✅ Enforce idempotency (detect duplicates)
- ✅ Store events locally
- ✅ Call Account Service (with Circuit Breaker)
- ✅ Handle Account Service failures gracefully
- ✅ Query events by ID or account
- ✅ Propagate trace IDs
- ✅ Return meaningful HTTP status codes

---

## 📦 Prerequisites

```bash
# Check Java version (21 or higher required)
java -version

# Check Maven (3.8 or higher required)
mvn -version
```

---

## 🚀 Running the Service

### **Option 1: Docker (Recommended)**

```bash
cd Event\ Ledger
docker-compose up --build event-gateway-service
```

**Important:** Account Service must be running first!

```bash
# Start both services
docker-compose up --build
```

**Expected Output:**
```
account-service        | Started AccountServiceApplication
event-gateway-service  | Started EventGatewayApplication
event-gateway-service  | Tomcat initialized with port 8011 (http)
```

### **Option 2: Local (Maven)**

```bash
cd event-gateway-service
mvn clean install
mvn spring-boot:run
```

**Expected Output:**
```
Started EventGatewayApplication in X seconds
Tomcat initialized with port 8011 (http)
```

### **Verify Running**

```bash
curl http://localhost:8081/health
```

**Response:**
```json
{
  "status":"UP",
  "components":{
    "accountService":"UP",
    "database":"UP"
  }
}
```

---

## 📚 API Endpoints

### **1. Submit Event (POST /events)**

Submit a financial transaction event for processing.

```http
POST /events
Content-Type: application/json

{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "metadata": {
    "source": "mainframe-batch",
    "batchId": "B-9042"
  }
}
```

**Response (201 Created):**
```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "status": "PROCESSED",
  "processedAt": "2026-06-09T07:25:00Z",
  "traceId": "f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d"
}
```

**Response (409 Conflict - Duplicate):**
```json
{
  "eventId": "evt-001",
  "message": "Event already processed",
  "status": "DUPLICATE",
  "originalProcessedAt": "2026-06-09T07:20:00Z"
}
```

**Response (400 Bad Request - Validation Error):**
```json
{
  "error": "Validation failed",
  "details": [
    "Amount must be greater than 0",
    "Type must be CREDIT or DEBIT"
  ]
}
```

**Response (503 Service Unavailable - Circuit Open):**
```json
{
  "error": "Account Service is temporarily unavailable",
  "message": "Circuit breaker is open. Please try again later.",
  "status": 503
}
```

---

### **2. Get Event by ID (GET /events/{id})**

Retrieve a single event by its ID.

```http
GET /events/evt-001
```

**Response (200 OK):**
```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "currency": "USD",
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "status": "PROCESSED",
  "processedAt": "2026-06-09T07:25:00Z",
  "metadata": {
    "source": "mainframe-batch",
    "batchId": "B-9042"
  }
}
```

**Response (404 Not Found):**
```json
{
  "error": "Event not found",
  "eventId": "evt-999"
}
```

---

### **3. List Events by Account (GET /events?account={accountId})**

Retrieve all events for a specific account, ordered by timestamp.

```http
GET /events?account=acct-123
```

**Response (200 OK):**
```json
[
  {
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "eventTimestamp": "2026-05-15T14:02:11Z",
    "status": "PROCESSED"
  },
  {
    "eventId": "evt-002",
    "accountId": "acct-123",
    "type": "DEBIT",
    "amount": 50.00,
    "eventTimestamp": "2026-05-15T14:05:00Z",
    "status": "PROCESSED"
  }
]
```

**Note:** Events are returned in chronological order by `eventTimestamp`, regardless of submission order.

---

### **4. Health Check (GET /health)**

Check service status and Account Service connectivity.

```http
GET /health
```

**Response (200 OK - All Systems Up):**
```json
{
  "status": "UP",
  "components": {
    "accountService": {
      "status": "UP",
      "responseTime": "15ms"
    },
    "database": {
      "status": "UP"
    },
    "circuitBreaker": {
      "status": "CLOSED",
      "failureRate": "0%"
    }
  },
  "timestamp": "2026-06-09T07:25:00Z"
}
```

**Response (503 Service Unavailable - Account Service Down):**
```json
{
  "status": "DEGRADED",
  "components": {
    "accountService": {
      "status": "DOWN",
      "error": "Connection refused"
    },
    "database": {
      "status": "UP"
    },
    "circuitBreaker": {
      "status": "OPEN",
      "failureRate": "85%"
    }
  }
}
```

---

## 🛡️ Resilience: Circuit Breaker

The Event Gateway implements **Resilience4j Circuit Breaker** to handle Account Service failures gracefully.

### **States**

```
CLOSED (Normal)
  └─ Threshold failures reached (50% failure rate)
     └─ OPEN (Stop calling, return error immediately)
        └─ Wait 30 seconds
           └─ HALF_OPEN (Test one request)
              ├─ Success → CLOSED (Resume normal)
              └─ Failure → OPEN (Try again later)
```

### **Configuration**

```properties
resilience4j.circuitbreaker.instances.account-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.account-service.wait-duration-in-open-state=30000
resilience4j.circuitbreaker.instances.account-service.minimum-number-of-calls=5
```

### **Behavior Examples**

**Scenario 1: Account Service Healthy**
```
POST /events → OK (200)
Circuit State: CLOSED ✓
```

**Scenario 2: Account Service Failing**
```
POST /events (1-4) → Timeout/Error
POST /events (5+)  → Circuit OPENS
Response: 503 Service Unavailable
Circuit State: OPEN ⚠️
```

**Scenario 3: Recovery**
```
Wait 30 seconds → Circuit tries one test request
If test succeeds → Circuit CLOSES, normal operation resumes ✓
```

---

## 🧪 Testing

### **Run Tests**

```bash
cd event-gateway-service
mvn test
```

### **Test Coverage**

- ✅ Event submission and validation
- ✅ Idempotency (duplicate detection)
- ✅ Out-of-order event handling
- ✅ Circuit breaker behavior
- ✅ Graceful degradation when Account Service is down
- ✅ Trace ID propagation
- ✅ HTTP status codes

### **Run Specific Test**

```bash
mvn test -Dtest=EventGatewayControllerTests
mvn test -Dtest=CircuitBreakerResiliencyTests
mvn test -Dtest=IdempotencyTests
```

### **Test Circuit Breaker Behavior**

```bash
mvn test -Dtest=CircuitBreakerResiliencyTests -Dtest.method=shouldReturnServiceUnavailableWhenCircuitOpen
```

---

## 🗄️ Database

### **H2 Console Access**

```
URL: http://localhost:8081/h2-console
Driver: org.h2.Driver
JDBC URL: jdbc:h2:mem:eventdb
Username: EVENT
Password: (empty)
```

### **Tables**

**events**
```sql
CREATE TABLE events (
  id BIGINT PRIMARY KEY,
  event_id VARCHAR(50) UNIQUE NOT NULL,
  account_id VARCHAR(50) NOT NULL,
  type VARCHAR(10) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'USD',
  event_timestamp TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL,
  processed_at TIMESTAMP,
  metadata JSON,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

---

## 📊 Configuration

### **application.properties**

```properties
# Server
server.port=8011
spring.application.name=event-gateway-service

# H2 Database
spring.datasource.url=jdbc:h2:mem:eventdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=EVENT
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# FeignClient
feign.client.config.account-service.connectTimeout=5000
feign.client.config.account-service.readTimeout=5000

# Circuit Breaker
resilience4j.circuitbreaker.instances.account-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.account-service.wait-duration-in-open-state=30000
resilience4j.circuitbreaker.instances.account-service.minimum-number-of-calls=5

# Logging
logging.level.root=INFO
logging.level.com.eventgateway=DEBUG
logging.level.org.springframework.cloud.sleuth=DEBUG
```

---

## 📝 Logging & Tracing

### **Trace ID Propagation**

Every request gets a unique trace ID:

```
Client Request
  ├─ Gateway receives request
  │  └─ Generates trace_id: f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d
  │
  └─ Gateway calls Account Service
     └─ Passes X-Trace-Id header
        └─ Account Service logs with same trace ID
```

### **Structured Logs**

All logs include trace ID for correlation:

```json
{
  "timestamp": "2026-06-09T07:25:06.352Z",
  "level": "INFO",
  "service": "event-gateway-service",
  "trace_id": "f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d",
  "message": "Event submitted successfully",
  "eventId": "evt-001",
  "accountId": "acct-123"
}
```

### **View Logs**

```bash
# Docker
docker-compose logs -f event-gateway-service

# Find all logs for a trace
docker-compose logs | grep "f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d"

# Local
# Logs appear in terminal where mvn spring-boot:run is running
```

---

## 🔍 Project Structure

```
event-gateway-service/
├── Dockerfile
├── pom.xml
├── src/main/java/com/eventgateway/
│   ├── EventGatewayApplication.java
│   ├── controller/
│   │   └── EventGatewayController.java
│   ├── service/
│   │   ├── EventService.java
│   │   └── EventProcessingService.java
│   ├── entity/
│   │   └── Event.java
│   ├── repository/
│   │   └── EventRepository.java
│   ├── dto/
│   │   ├── EventRequest.java
│   │   └── EventResponse.java
│   ├── client/
│   │   └── AccountServiceClient.java (FeignClient)
│   ├── config/
│   │   ├── FeignConfig.java
│   │   └── CircuitBreakerConfig.java
│   └── exception/
│       ├── EventValidationException.java
│       └── ServiceUnavailableException.java
├── src/main/resources/
│   └── application.properties
└── src/test/java/
    └── com/eventgateway/
        ├── EventGatewayControllerTests.java
        ├── IdempotencyTests.java
        └── CircuitBreakerResiliencyTests.java
```

---

## 🐛 Troubleshooting

### **Connection Refused to Account Service**

**Error:**
```
Connection refused executing POST http://localhost:8012/accounts/...
```

**Fix:**
Update `AccountServiceClient` to use Docker service name:

```java
@FeignClient(name = "account-service", url = "http://account-service:8012")
public interface AccountServiceClient {
    @PostMapping("/accounts/{accountId}/transactions")
    void createTransaction(@PathVariable String accountId, @RequestBody TransactionRequest req);
}
```

### **H2 Console Not Accessible**

**Fix:**
Add to `application.properties`:
```properties
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true
```

Restart service and access: `http://localhost:8081/h2-console`

### **Port 8011 Already in Use**

```bash
# Find and kill process
lsof -ti :8011 | xargs kill -9  # macOS/Linux
netstat -ano | findstr :8011    # Windows
```

### **Circuit Breaker Always Open**

**Check Status:**
```bash
curl http://localhost:8081/health
```

**Reset:**
```bash
docker-compose restart event-gateway-service
```

**Common Causes:**
- Account Service is down
- Network connectivity issue
- FeignClient URL is wrong

### **Duplicate Event Not Detected**

Make sure `eventId` is unique in database. Check:

```bash
# H2 Console
SELECT * FROM events WHERE event_id = 'evt-001';
```

Should return only one row for duplicate submission.

---

## 📞 Integration with Account Service

This service calls Account Service via FeignClient:

```java
@FeignClient(name = "account-service", url = "http://account-service:8012")
@CircuitBreaker(name = "account-service")
public interface AccountServiceClient {
    
    @PostMapping("/accounts/{accountId}/transactions")
    void createTransaction(
        @PathVariable String accountId,
        @RequestBody TransactionRequest req
    );
    
    @GetMapping("/accounts/{accountId}/balance")
    BalanceResponse getBalance(@PathVariable String accountId);
}
```

**URL Format:**
- Docker: `http://account-service:8012`
- Local: `http://localhost:8080`

---

## 🚀 Example Workflow

```bash
# 1. Check health
curl http://localhost:8081/health

# 2. Submit event
curl -X POST http://localhost:8081/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "currency": "USD",
    "eventTimestamp": "2026-05-15T14:02:11Z"
  }'

# 3. Get event
curl http://localhost:8081/events/evt-001

# 4. List account events
curl "http://localhost:8081/events?account=acct-123"

# 5. Check balance (via Account Service)
curl http://localhost:8080/accounts/acct-123/balance
```

---

## 📄 Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

**Last Updated:** 2026-06-09  
**Java Version:** 21  
**Spring Boot Version:** 3.5.14  
**Resilience Pattern:** Circuit Breaker (Resilience4j)
