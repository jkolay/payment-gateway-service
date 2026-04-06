# Payment Gateway Service

A Spring Boot–based payment gateway service that demonstrates a clean, extensible architecture for processing payments with fraud assessment and bank integration.

This project is designed as a **reference implementation**, focusing on service orchestration, state management, and testability rather than real payment provider integration.


## Overview

The Payment Gateway Service processes customer payments by orchestrating multiple bounded services:

- Payment lifecycle management
- Fraud risk assessment
- Bank authorization simulation

Each concern is isolated behind a clear service interface to support testability, modularity, and future extensibility.

## Project Context

This is a **Secure Event-Driven Payment Gateway** built with Spring Boot 3.3.x / Java 21. It follows the same layered architecture, coding conventions, and testing patterns as the companion Recipe Management Service project.

The project is a technical assessment deliverable. It must demonstrate:
- Event-driven async processing
- Multi-tenant security
- Financial data integrity (idempotency, optimistic locking)
- Immutable audit trails
- Clean architecture with comprehensive tests

---

## Coding Conventions 

### General Style
- **Java 21** — used modern language features (records for simple data, pattern matching, switch expressions, text blocks where appropriate)
- **Lombok** — used `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Slf4j` for entities and DTOs
- **Constructor injection** via `@AllArgsConstructor` on services/controllers (no `@Autowired` on fields)
- **Concise methods** — kept methods short; extract filters into small private methods
- **Filter + forEach style** for classifier/comparison logic (user preference)

### Package Structure
```
com.payment/
  config/          — Security, OpenAPI, Async thread pool configs
  controller/      — REST controllers (@RestController, @RequestMapping)
  service/         — Business logic (@Service, @Transactional)
  repository/      — Data access (Spring Data JPA interfaces)
  entity/          — JPA entities + enums
  dto/             — Request/Response DTOs with Bean Validation
  event/           — Spring ApplicationEvent + @EventListener
  exception/       — Custom exceptions + GlobalExceptionHandler
  mapper/          — Manual entity ↔ DTO mappers (@Component)
  util/            — Utility classes (MaskingUtil)
```

### Naming
- Entities: `Payment`, `PaymentAuditLog`, `Client`
- DTOs: Intent-based names — `PaymentInitiationRequest`, `PaymentStatusResponse`, `PaymentAuditLogDTO`
- Exceptions: `XxxNotFoundException`, `DuplicatePaymentException`
- Services: `XxxService`
- Repositories: `XxxRepository`
- Controller paths: `/api/v1/{resource}`

### Entity Conventions
- Used `@PrePersist`/`@PreUpdate` for timestamps
- Used `@Version` for optimistic locking on mutable entities
- Used `@Builder.Default` for collection defaults
- Usde `@EntityGraph` on repository methods to avoid N+1

### DTO Conventions
- Used Bean Validation annotations: `@NotBlank`, `@NotNull`, `@DecimalMin`, `@Size`, etc.
- DTOs are POJOs with Lombok — no business logic
- Used `@Builder` on response DTOs

### Service Conventions
- `@Transactional` on write methods
- `@Transactional(readOnly = true)` on read methods
- Used `@Slf4j` for logging
- Masked sensitive data using `MaskingUtil` before logging

### Controller Conventions
- Used `@Valid @RequestBody` for input validation
- Extracted client ID from `Authentication.getName()`
- Returned `202 Accepted` for async ingestion, `200` for reads, `204` for deletes
- Used OpenAPI `@Operation` and `@Tag` annotations
- Added page size to prevent unbounded queries

### Exception Handling
- Custom runtime exceptions for domain errors
- `@RestControllerAdvice` with `GlobalExceptionHandler`
- Consistent `ErrorResponse` with timestamp, status, error, message, path


### Mapper Conventions
- `@Component` Spring beans (not MapStruct)
- `toDTO()`, `toEntityForCreate()`, `updateEntity()` method naming
- Repository lookups NOT done in mappers — they stay in the service layer



---


## Architecture

The application follows a layered architecture with a dedicated orchestration layer.

Core components:

- **PaymentProcessingService**  
  Coordinates the end‑to‑end payment flow.

- **PaymentService**  
  Manages payment persistence and state transitions.

- **FraudAssessmentService**  
  Performs fraud checks based on payment attributes.

- **BankSimulatorService**  
  Abstracts external bank authorization.

The architecture deliberately avoids tight coupling between services to allow independent evolution of fraud rules, bank integrations, and persistence strategies.


## Payment Lifecycle

A payment progresses through a defined set of states:

1. INITIATED
2. FRAUD_CHECK_PASSED / FRAUD_CHECK_FAILED
3. COMPLETED or FAILED

All state transitions are controlled via the `PaymentService` to ensure consistency and auditability.


### Security
1. All `/api/**` endpoints are required authentication
2. Client ID is extracted from authentication principal (JWT `sub` claim or dev token mapping)
3. Every payment query filters by `clientId` — no cross-tenant access
4. Dev profile supports multiple test tokens: `client-a-token` → `client-a`, `client-b-token` → `client-b`


### Event-Driven Processing
1. Payment ingestion is synchronous: validate → save → publish event → return 202
2. Processing is async via `@Async("paymentProcessingExecutor")` on `@EventListener`
3. Pipeline stages: Fraud Assessment → Bank Processing
4. Each stage transitions the payment status and creates an audit log entry

### Financial Integrity
1. **Idempotency**: Duplicate `(client_id, idempotency_key)` returns existing payment, no new creation
2. **Optimistic locking**: `@Version` prevents concurrent update conflicts
3. **Audit trail**: Every status transition recorded in `payment_audit_logs` with previous/new status + timestamp
4. **Immutability**: Audit log entries are never updated or deleted (`updatable = false` on all columns)

### Payment State Machine
```
INITIATED → FRAUD_CHECK_PENDING → FRAUD_CHECK_PASSED → PROCESSING → COMPLETED
                                 → FRAUD_CHECK_FAILED              → FAILED
```

---

## Key Design Decisions to Defend
1. **Spring ApplicationEvent vs Kafka**: Chose in-process events for simplicity. In production, use a durable broker with retry/DLQ.
2. **H2 vs PostgreSQL**: Chose In-memory H2 for zero-setup demo. Schema is SQL-standard and portable.
3. **Manual mappers vs MapStruct**: Chose  manual for transparency and fewer build dependencies.
4. **Thread.sleep in BankSimulator**: Simulates real network latency. In production, this would be an HTTP client call.
5. **Random fraud/bank outcomes**: Demonstrated resilience. Production would use real ML models and bank APIs.

---



## Build & Run Commands

```bash
# Build and run tests
mvn clean test

# Run with dev profile (no real IdP needed)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Generate test coverage report
mvn clean test jacoco:report
# Report at: target/site/jacoco/index.html

# Package as JAR
mvn clean package -DskipTests
java -jar target/payment-gateway-service-1.0.0.jar --spring.profiles.active=dev
```

---

## File Reference

| File | Purpose |
|------|---------|
| `PaymentGatewayApplication.java` | Entry point with `@EnableAsync` |
| `SecurityConfig.java` | OAuth2 JWT (prod) + dev token filter (dev) |
| `AsyncConfig.java` | Bounded thread pool for async processing |
| `PaymentController.java` | REST endpoints: POST /payments, GET /payments/{id}, GET /payments |
| `PaymentService.java` | Ingestion, status inquiry, state transitions |
| `PaymentProcessingService.java` | Async pipeline: fraud → bank → final status |
| `FraudAssessmentService.java` | Pluggable fraud rules (amount threshold + random) |
| `BankSimulatorService.java` | Bank simulation (variable latency + random outcome) |
| `PaymentMapper.java` | Entity ↔ DTO conversions |
| `MaskingUtil.java` | Sensitive data masking for logs |
| `GlobalExceptionHandler.java` | Centralized error handling |
| `PaymentEvent.java` | Spring ApplicationEvent for async dispatch |
| `PaymentEventListener.java` | Picks up events, delegates to processing service |
| `schema.sql` | DDL for all tables |
| `data.sql` | Seed clients (tenants) |

### Spring Boot Actuator

The project includes **Spring Boot Actuator** for monitoring and managing the application. Actuator endpoints provide insights into the application's health, metrics, and other operational data.

#### Key Endpoints:
- `/actuator/health`: Displays the health status of the application.

#### Configuration:
- Actuator is enabled by default in all profiles.
- Sensitive endpoints are secured and require authentication in production.
- To customize endpoints, modify `application.properties`:
  
  management.endpoints.web.exposure.include= health, metrics, loggers, info
  management.endpoint.health.show-details= always


### OpenAPI Documentation

The project includes **OpenAPI Documentation** for API testing and exploration using Swagger.

#### Key Features:
- **Swagger UI**: Accessible at `/swagger-ui.html` (enabled in `dev` profile).
- **OpenAPI JSON**: Available at `/v3/api-docs`.

#### Configuration:
- OpenAPI is enabled in the `dev` profile for testing and development.



## Curl Commands for Testing

### ✅ Initiate a Payment

```bash
curl -X POST http://127.0.0.1:8085/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer client-a-token" \
  -d '{
    "idempotencyKey": "unique-key-123",
    "amount": 100.00,
    "currency": "USD",
    "description": "Payment for order #12345"
  }'
  ## Expected Response: 202 Accepted
```



### ✅ Get Payment Details by Tracking ID
```bash
curl -X GET http://127.0.0.1:8085/api/v1/payments/11843122-c4c5-4106-9a90-fac3a3ac0d7b \
  -H "Authorization: Bearer client-a-token" \
  -H "Accept: application/json"
# Expected Response: 200 OK with payment details in JSON format
```



### Conclusion
This project demonstrates a modular, testable approach to payment processing using Spring Boot.  
While simplified, the architecture reflects real-world payment system concerns such as orchestration, state management, and failure handling.

