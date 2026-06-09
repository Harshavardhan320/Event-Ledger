# Event Ledger - Microservices System

A distributed financial transaction event processing system built with Spring Boot, demonstrating microservices architecture, idempotency, out-of-order event handling, and resilience patterns.

## 📋 Table of Contents

- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
- [Running the Services](#running-the-services)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Resiliency Pattern](#resiliency-pattern)
- [Observability & Logging](#observability--logging)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Architecture

### System Design

```
┌──────────────────────────────────────────────────────────────┐
│                     Client/Browser                           │
└─────────────────────────┬──────────────────────────────────┘
                          │
                          ▼
        ┌─────────────────────────────────────┐
        │   Event Gateway API                 │
        │   (Port 8081 / External)            │
        │                                     │
        │ - Receives transaction events       │
        │ - Validates & deduplicates (idempotent)
        │ - Stores events locally             │
        │ - Traces requests (OpenTelemetry)   │
        │ - Implements Circuit Breaker        │
        └────────────┬────────────────────────┘
                     │
                     │ REST + Trace Headers
                     │ (FeignClient)
                     ▼
        ┌─────────────────────────────────────┐
        │   Account Service                   │
        │   (Port 8080 / Internal)            │
        │                                     │
        │ - Manages account state/balance     │
        │ - Applies transactions in order     │
        │ - Returns balance calculations      │
        └─────────────────────────────────────┘
```

### Service Responsibilities

#### **Event Gateway Service** (Port 8081)
- **Public API** for receiving transaction events
- Enforces **idempotency** — duplicate `eventId` submissions are safely rejected
- Stores all events in local database
- Calls Account Service to apply transactions
- Implements **Circuit Breaker** for resilience
- Propagates **trace IDs** to Account Service
- Returns meaningful HTTP status codes for all scenarios

#### **Account Service** (Port 8080)
- **Private API** — only called by Gateway
- Manages account balances and transaction history
- Maintains correct balance regardless of event arrival order
- Logs all operations with trace context
- Health check endpoint for monitoring

---

## 📦 Prerequisites

### **System Requirements**
- Docker & Docker Compose (recommended)
- OR Java 21+, Maven 3.8+

### **Technology Stack**
- **Language:** Java 21
- **Framework:** Spring Boot 3.5.14
- **Build Tool:** Maven 3.9.6
- **Database:** H2 (in-memory)
- **Communication:** REST + FeignClient
- **Observability:** Spring Cloud Sleuth (trace propagation)
- **Resilience:** Resilience4j (Circuit Breaker)
- **Logging:** SLF4J with JSON formatting

---

## 📁 Project Structure

```
Event Ledger/
├── docker-compose.yml                 # Docker Compose config
├── README.md                          # This file
│
├── account-service/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── src/main/java/com/account/
│   │   ├── AccountServiceApplication.java
│   │   ├── controller/
│   │   │   └── AccountController.java
│   │   ├── service/
│   │   │   ├── AccountService.java
│   │   │   └── TransactionService.java
│   │   ├── entity/
│   │   │   ├── Account.java
│   │   │   └── Transaction.java
│   │   ├── repository/
│   │   │   ├── AccountRepository.java
│   │   │   └── TransactionRepository.java
│   │   ├── dto/
│   │   │   ├── TransactionRequest.java
│   │   │   └── AccountResponse.java
│   │   └── config/
│   │       └── DatabaseConfig.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── src/test/java/
│       └── com/account/
│           └── AccountServiceTests.java
│
└── event-gateway-service/
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
    │   │   └── AccountServiceClient.java  (FeignClient)
    │   ├── config/
    │   │   ├── DatabaseConfig.java
    │   │   ├── FeignConfig.java
    │   │   └── CircuitBreakerConfig.java
    │   └── exception/
    │       └── EventValidationException.java
    ├── src/main/resources/
    │   └── application.properties
    └── src/test/java/
        └── com/eventgateway/
            ├── EventGatewayControllerTests.java
            └── CircuitBreakerResiliencyTests.java
```

---

## 🚀 Setup & Installation

### **Option 1: Docker Compose (Recommended)**

#### **1. Prerequisites**
```bash
docker --version        # Docker 20.10+
docker-compose --version  # Docker Compose 2.0+
```

#### **2. Clone Repository**
```bash
git clone <your-repo-url>
cd Event\ Ledger
```

#### **3. Build & Start Services**
```bash
docker-compose up --build
```

**Expected Output:**
```
account-service        | 2026-06-09T... INFO ... Started AccountServiceApplication in X.XXX seconds
event-gateway-service  | 2026-06-09T... INFO ... Started EventGatewayApplication in X.XXX seconds
```

#### **4. Verify Services Running**
```bash
curl http://localhost:8080/health      # Account Service
curl http://localhost:8081/health      # Event Gateway
```

Expected response:
```json
{"status":"UP"}
```

---

### **Option 2: Manual Setup (Local)**

#### **1. Prerequisites**
```bash
java -version          # Java 21+
mvn -version          # Maven 3.8+
```

#### **2. Clone Repository**
```bash
git clone <your-repo-url>
cd Event\ Ledger
```

#### **3. Build Both Services**
```bash
# Account Service
cd account-service
mvn clean install

# Event Gateway Service
cd ../event-gateway-service
mvn clean install
cd ..
```

#### **4. Start Account Service (Terminal 1)**
```bash
cd account-service
mvn spring-boot:run
```

Wait for:
```
Started AccountServiceApplication in X seconds
Tomcat initialized with port 8012
```

#### **5. Start Event Gateway Service (Terminal 2)**
```bash
cd event-gateway-service
mvn spring-boot:run
```

Wait for:
```
Started EventGatewayApplication in X seconds
Tomcat initialized with port 8011
```

#### **6. Verify Services**
```bash
# Account Service Health
curl http://localhost:8080/health

# Event Gateway Health
curl http://localhost:8081/health
```

---

## 📡 Running the Services

### **Docker Compose Commands**

```bash
# Start services (foreground, view logs)
docker-compose up --build

# Start services (background)
docker-compose up -d --build

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f account-service
docker-compose logs -f event-gateway-service

# Stop services
docker-compose down

# Stop & remove volumes
docker-compose down -v
```

### **Local Execution Commands**

```bash
# Terminal 1: Account Service
cd account-service
mvn spring-boot:run

# Terminal 2: Event Gateway Service
cd event-gateway-service
mvn spring-boot:run
```

### **Access Points**

| Service | URL | Purpose |
|---------|-----|---------|
| Account Service | `http://localhost:8080` | Health checks, balance queries |
| Event Gateway | `http://localhost:8081` | Event submission, event queries |
| H2 Console (Account) | `http://localhost:8080/h2-console` | Database admin |
| H2 Console (Gateway) | `http://localhost:8081/h2-console` | Database admin |

---

## 📚 API Documentation

### **Event Gateway Service** (External API)

#### **1. Submit Event**
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
  "status": "PROCESSED",
  "processedAt": "2026-06-09T07:25:00Z"
}
```

**Response (409 Conflict - Duplicate):**
```json
{
  "eventId": "evt-001",
  "message": "Event already processed",
  "status": "DUPLICATE"
}
```

#### **2. Get Event by ID**
```http
GET /events/{eventId}
```

**Response (200 OK):**
```json
{
  "eventId": "evt-001",
  "accountId": "acct-123",
  "type": "CREDIT",
  "amount": 150.00,
  "eventTimestamp": "2026-05-15T14:02:11Z",
  "status": "PROCESSED"
}
```

#### **3. List Events by Account**
```http
GET /events?account={accountId}
```

**Response (200 OK):**
```json
[
  {
    "eventId": "evt-001",
    "accountId": "acct-123",
    "type": "CREDIT",
    "amount": 150.00,
    "eventTimestamp": "2026-05-15T14:02:11Z"
  },
  {
    "eventId": "evt-002",
    "accountId": "acct-123",
    "type": "DEBIT",
    "amount": 50.00,
    "eventTimestamp": "2026-05-15T14:05:00Z"
  }
]
```

#### **4. Health Check**
```http
GET /health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "accountService": "UP",
    "database": "UP"
  }
}
```

---

### **Account Service** (Internal API)

#### **1. Apply Transaction**
```http
POST /accounts/{accountId}/transactions
Content-Type: application/json

{
  "eventId": "evt-001",
  "type": "CREDIT",
  "amount": 150.00,
  "eventTimestamp": "2026-05-15T14:02:11Z"
}
```

**Response (200 OK):**
```json
{
  "transactionId": "txn-abc123",
  "accountId": "acct-123",
  "amount": 150.00,
  "balanceAfter": 300.00
}
```

#### **2. Get Account Balance**
```http
GET /accounts/{accountId}/balance
```

**Response (200 OK):**
```json
{
  "accountId": "acct-123",
  "balance": 300.00,
  "currency": "USD",
  "lastUpdated": "2026-06-09T07:25:00Z"
}
```

#### **3. Get Account Details**
```http
GET /accounts/{accountId}
```

**Response (200 OK):**
```json
{
  "accountId": "acct-123",
  "balance": 300.00,
  "totalCredits": 500.00,
  "totalDebits": 200.00,
  "transactionCount": 5,
  "lastTransaction": "2026-06-09T07:25:00Z"
}
```

#### **4. Health Check**
```http
GET /health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "database": "connected"
}
```

---

## 🧪 Testing

### **Run All Tests**

#### **Docker Environment**
```bash
# Build and run tests in containers
docker-compose up --build
docker-compose exec account-service mvn test
docker-compose exec event-gateway-service mvn test
```

#### **Local Environment**
```bash
# Account Service Tests
cd account-service
mvn test

# Event Gateway Service Tests
cd ../event-gateway-service
mvn test
```

### **Test Coverage**

Tests validate:

1. **Core Functionality**
   - ✅ Idempotency — duplicate `eventId` submissions
   - ✅ Out-of-order events — events arrive in random order, balance is correct
   - ✅ Balance calculation — sum of credits - sum of debits
   - ✅ Input validation — missing fields, invalid amounts, unknown types

2. **Resiliency**
   - ✅ Circuit Breaker opens after Account Service failures
   - ✅ Graceful degradation — meaningful error messages
   - ✅ Timeout handling — requests don't hang

3. **Trace Propagation**
   - ✅ Trace ID generated at Gateway
   - ✅ Trace ID propagated to Account Service via headers
   - ✅ Both services log trace IDs

4. **Integration Tests**
   - ✅ Full event flow — Gateway → Account Service
   - ✅ End-to-end scenarios with multiple events

### **Example Test Commands**

```bash
# Run specific test class
mvn test -Dtest=EventGatewayControllerTests

# Run tests with specific pattern
mvn test -Dtest=*Idempotency*

# Run tests with logging
mvn test -X

# Run tests and generate coverage report
mvn test jacoco:report
```

---

## 🛡️ Resiliency Pattern

### **Pattern Chosen: Circuit Breaker** (via Resilience4j)

#### **Why Circuit Breaker?**

| Pattern | Why Chosen | Why Not Others |
|---------|-----------|-----------------|
| **Circuit Breaker** ✅ | Prevents cascading failures; stops calling failing service | |
| Timeout + Retry | Good for transient failures, but repeated calls waste resources | Doesn't prevent cascading failures |
| Bulkhead | Good for resource isolation, but requires thread pool config | Doesn't prevent cascading failures |

#### **How It Works**

```
CLOSED → (failure threshold reached) → OPEN
↑                                         ↓
│                    (wait time elapsed)
└─────────────────── HALF_OPEN ←────────┘
                          ↓
               (single request test)
                   /          \
            Success        Failure
              /                \
         CLOSED              OPEN
```

#### **Configuration** (application.properties)

```properties
# Circuit Breaker Settings
resilience4j.circuitbreaker.instances.account-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.account-service.wait-duration-in-open-state=30000
resilience4j.circuitbreaker.instances.account-service.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.account-service.automatic-transition-from-open-to-half-open-enabled=true
```

#### **Behavior**

1. **Normal Operation** (CLOSED)
   - Calls to Account Service pass through
   - Failures are counted

2. **Too Many Failures** (OPEN)
   - Circuit breaker opens
   - Further requests immediately return error (no actual call made)
   - Client receives: `503 Service Unavailable`

3. **Recovery Attempt** (HALF_OPEN)
   - After 30 seconds, one test request is allowed
   - If it succeeds → Circuit closes, normal operation resumes
   - If it fails → Circuit stays open

#### **Client Experience**

```
Request Timeline:
1. Requests 1-4 → Account Service fails
2. Request 5 → Circuit OPENS
3. Requests 6-100 → Immediate 503 (no actual calls to Account Service)
4. After 30 seconds: One test request allowed
5. If test succeeds → Circuit CLOSES, back to normal
```

#### **Code Example**

```java
@FeignClient(name = "account-service", url = "http://account-service:8012")
@CircuitBreaker(name = "account-service", fallback = AccountServiceFallback.class)
public interface AccountServiceClient {
    @PostMapping("/accounts/{accountId}/transactions")
    void createTransaction(@PathVariable String accountId, @RequestBody TransactionRequest req);
}

// Fallback when circuit is open
public class AccountServiceFallback implements AccountServiceClient {
    @Override
    public void createTransaction(String accountId, TransactionRequest req) {
        throw new ServiceUnavailableException("Account Service is temporarily unavailable");
    }
}
```

---

## 📊 Observability & Logging

### **Structured Logging**

All logs are JSON-formatted with:
- **timestamp** — ISO 8601 format
- **trace_id** — Unique request identifier
- **service** — Service name (account-service / event-gateway-service)
- **level** — Log level (INFO, DEBUG, ERROR, etc.)
- **message** — Log message

**Example Log Output:**
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

### **Trace Propagation** (Spring Cloud Sleuth)

- **Trace ID** generated at Gateway for each incoming request
- **Propagated** to Account Service via HTTP headers: `X-Trace-Id`
- **Logged** by both services for request correlation

**How to Find All Logs for a Request:**
```bash
# Example trace ID: f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d
docker-compose logs | grep "f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d"
```

### **Health Metrics**

#### **Endpoint**
```http
GET /health
```

**Response includes:**
- Service status (UP/DOWN)
- Database connectivity
- Account Service availability (for Gateway)
- Circuit breaker status

### **Custom Metrics**

Exposed via logs:
- **request_count** — Total requests per endpoint
- **error_rate** — Percentage of failed requests
- **response_time_ms** — Request latency

---

## 🔧 Troubleshooting

### **Problem: Connection Refused on localhost:8012**

**Symptom:**
```
Connection refused executing POST http://localhost:8012/accounts/...
```

**Root Cause:**
Inside Docker, `localhost` refers to the container itself, not the host.

**Solution:**
Update FeignClient URL in `event-gateway-service`:
```java
@FeignClient(name = "account-service", url = "http://account-service:8012")
```

Use **service name** (`account-service`), not `localhost`.

---

### **Problem: H2 Console - Remote Connections Disabled**

**Symptom:**
```
Sorry, remote connections ('webAllowOthers') are disabled on this server.
```

**Solution:**
Add to `application.properties`:
```properties
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true
```

Then access:
```
http://localhost:8080/h2-console  (Account Service)
http://localhost:8081/h2-console  (Event Gateway)
```

**H2 Login:**
- Driver Class: `org.h2.Driver`
- JDBC URL: `jdbc:h2:mem:testdb`
- User Name: `sa`
- Password: (empty)

---

### **Problem: Port Already in Use**

**Symptom:**
```
Address already in use: bind
```

**Solution:**
Change ports in `docker-compose.yml`:
```yaml
account-service:
  ports:
    - "9080:8012"  # Changed from 8080
event-gateway-service:
  ports:
    - "9081:8011"  # Changed from 8081
```

Or kill process using port:
```bash
# macOS/Linux
lsof -ti :8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

---

### **Problem: Services Won't Start**

**Debug Steps:**
```bash
# View full logs
docker-compose logs -f

# Check service status
docker-compose ps

# Rebuild everything
docker-compose down -v
docker-compose up --build

# Check if Docker is running
docker ps
```

---

### **Problem: 404 Not Found in API Requests**

**Symptom:**
```json
{"error": "404 Not Found"}
```

**Cause:**
Endpoint path is incorrect.

**Verify Endpoints:**
```bash
# Account Service
curl http://localhost:8080/health

# Event Gateway
curl http://localhost:8081/events
curl http://localhost:8081/health
```

**Check Controller Mapping:**
Look at `@RequestMapping` and `@PostMapping` annotations in Controller classes.

---

### **Problem: Circuit Breaker Always Open**

**Check Status:**
```bash
docker-compose logs event-gateway-service | grep "circuit"
```

**Common Causes:**
- Account Service is down
- Wrong URL in FeignClient
- Network connectivity issues

**Reset Circuit Breaker:**
```bash
# Restart Event Gateway Service
docker-compose restart event-gateway-service
```

---

## 📝 Key Features

✅ **Idempotency** — Duplicate events are safely handled  
✅ **Out-of-Order Tolerance** — Events processed in correct chronological order  
✅ **Distributed Tracing** — Full request path visible across services  
✅ **Structured Logging** — JSON logs with trace IDs  
✅ **Circuit Breaker** — Prevents cascading failures  
✅ **Graceful Degradation** — Meaningful errors when services fail  
✅ **Health Checks** — Monitor service status  
✅ **Docker Compose** — One-command deployment  
✅ **Comprehensive Tests** — Unit + integration test coverage  

---

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Open Pull Request

---

## 📞 Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review logs: `docker-compose logs -f`
3. Open an issue on GitHub

---

## 📄 License

This project is provided as-is for educational and evaluation purposes.

---

**Last Updated:** 2026-06-09  
**Java Version:** 21  
**Spring Boot Version:** 3.5.14  
**Docker Compose Version:** 3.8
