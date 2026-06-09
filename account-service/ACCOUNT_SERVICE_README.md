# Account Service

Internal microservice that manages account state, balances, and transactions for the Event Ledger system.

## 📋 Overview

The Account Service is a private API that maintains account balances and transaction history. It receives transaction requests from the Event Gateway Service and applies them in chronological order to ensure correct balance calculations regardless of event arrival order.

**Port:** 8012 (internal)  
**Database:** H2 (in-memory)  
**Framework:** Spring Boot 3.5.14

---

## 🏗️ Architecture

```
Event Gateway Service
        │
        │ REST Call
        │ (with trace header)
        ▼
Account Service
├── TransactionController
├── AccountService
├── H2 Database
│   ├── Account (balance, metadata)
│   └── Transaction (history, amounts)
```

### Responsibilities

- ✅ Apply transactions to accounts
- ✅ Calculate balances (credits - debits)
- ✅ Maintain transaction history
- ✅ Return account details
- ✅ Process events in chronological order
- ✅ Propagate trace IDs in logs

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
docker-compose up --build account-service
```

**Expected Output:**
```
account-service | 2026-06-09T07:04:12.750Z  INFO [...] Started AccountServiceApplication
account-service | Tomcat initialized with port 8012 (http)
```

### **Option 2: Local (Maven)**

```bash
cd account-service
mvn clean install
mvn spring-boot:run
```

**Expected Output:**
```
Started AccountServiceApplication in X seconds
Tomcat initialized with port 8012 (http)
```

### **Verify Running**

```bash
curl http://localhost:8080/health
```

**Response:**
```json
{"status":"UP"}
```

---

## 📚 API Endpoints

### **1. Apply Transaction**

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
  "type": "CREDIT",
  "amount": 150.00,
  "balanceAfter": 300.00,
  "processedAt": "2026-06-09T07:25:00Z"
}
```

**Error Cases:**
```json
// Invalid account
{
  "error": "Account not found",
  "status": 404
}

// Invalid amount
{
  "error": "Amount must be greater than 0",
  "status": 400
}
```

---

### **2. Get Account Balance**

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

---

### **3. Get Account Details**

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
  "createdAt": "2026-05-15T10:00:00Z",
  "lastTransaction": "2026-06-09T07:25:00Z",
  "transactions": [
    {
      "transactionId": "txn-001",
      "type": "CREDIT",
      "amount": 100.00,
      "eventTimestamp": "2026-05-15T14:00:00Z"
    },
    {
      "transactionId": "txn-002",
      "type": "DEBIT",
      "amount": 50.00,
      "eventTimestamp": "2026-05-15T14:05:00Z"
    }
  ]
}
```

---

### **4. Health Check**

```http
GET /health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "database": "connected",
  "timestamp": "2026-06-09T07:25:00Z"
}
```

---

## 🧪 Testing

### **Run Tests**

```bash
cd account-service
mvn test
```

### **Test Coverage**

- ✅ Transaction application
- ✅ Balance calculation
- ✅ Out-of-order event handling
- ✅ Input validation
- ✅ Account creation
- ✅ Transaction history

### **Run Specific Test**

```bash
mvn test -Dtest=AccountServiceTests
mvn test -Dtest=TransactionTests
```

---

## 🗄️ Database

### **H2 Console Access**

```
URL: http://localhost:8080/h2-console
Driver: org.h2.Driver
JDBC URL: jdbc:h2:mem:testdb
Username: sa
Password: (empty)
```

### **Tables**

**accounts**
```sql
CREATE TABLE accounts (
  id BIGINT PRIMARY KEY,
  account_id VARCHAR(50) UNIQUE NOT NULL,
  balance DECIMAL(19,2) DEFAULT 0,
  currency VARCHAR(3) DEFAULT 'USD',
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

**transactions**
```sql
CREATE TABLE transactions (
  id BIGINT PRIMARY KEY,
  account_id VARCHAR(50) NOT NULL,
  event_id VARCHAR(50) UNIQUE,
  type VARCHAR(10) NOT NULL,
  amount DECIMAL(19,2) NOT NULL,
  event_timestamp TIMESTAMP NOT NULL,
  processed_at TIMESTAMP,
  FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);
```

---

## 📊 Configuration

### **application.properties**

```properties
# Server
server.port=8012
spring.application.name=account-service

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Logging
logging.level.root=INFO
logging.level.com.account=DEBUG

# Actuator
management.endpoints.web.exposure.include=health,info
```

---

## 📝 Logging

All logs include trace ID for request correlation:

```json
{
  "timestamp": "2026-06-09T07:25:06.352Z",
  "level": "INFO",
  "service": "account-service",
  "trace_id": "f9ef8c3d-2024-4620-8fb7-1b5c7e2f1f8d",
  "message": "Transaction applied successfully",
  "accountId": "acct-123",
  "transactionId": "txn-abc123"
}
```

### **View Logs**

```bash
# Docker
docker-compose logs -f account-service

# Local
# Logs appear in terminal where mvn spring-boot:run is running
```

---

## 🔍 Project Structure

```
account-service/
├── Dockerfile
├── pom.xml
├── src/main/java/com/account/
│   ├── AccountServiceApplication.java
│   ├── controller/
│   │   └── AccountController.java
│   ├── service/
│   │   ├── AccountService.java
│   │   └── TransactionService.java
│   ├── entity/
│   │   ├── Account.java
│   │   └── Transaction.java
│   ├── repository/
│   │   ├── AccountRepository.java
│   │   └── TransactionRepository.java
│   ├── dto/
│   │   ├── TransactionRequest.java
│   │   ├── TransactionResponse.java
│   │   └── AccountResponse.java
│   └── config/
│       └── DatabaseConfig.java
├── src/main/resources/
│   └── application.properties
└── src/test/java/
    └── com/account/
        ├── AccountServiceTests.java
        └── TransactionTests.java
```

---

## 🐛 Troubleshooting

### **Port 8012 Already in Use**

```bash
# Find and kill process
lsof -ti :8012 | xargs kill -9  # macOS/Linux
netstat -ano | findstr :8012    # Windows
```

### **H2 Console Not Accessible**

Make sure these are in `application.properties`:
```properties
spring.h2.console.enabled=true
spring.h2.console.settings.web-allow-others=true
```

Then restart service.

### **Trace ID Not Appearing in Logs**

Add Spring Cloud Sleuth dependency:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

### **Transaction Not Applied**

Check:
1. Account exists: `GET /accounts/{accountId}`
2. Event timestamp is valid ISO 8601 format
3. Amount is greater than 0
4. Type is "CREDIT" or "DEBIT"

---

## 📞 Integration with Event Gateway

This service is called by Event Gateway Service via FeignClient:

```java
@FeignClient(name = "account-service", url = "http://account-service:8012")
public interface AccountServiceClient {
    @PostMapping("/accounts/{accountId}/transactions")
    void createTransaction(@PathVariable String accountId, @RequestBody TransactionRequest req);
}
```

**URL Format:**
- Docker: `http://account-service:8012`
- Local: `http://localhost:8080`

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
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
```

---

**Last Updated:** 2026-06-09  
**Java Version:** 21  
**Spring Boot Version:** 3.5.14
