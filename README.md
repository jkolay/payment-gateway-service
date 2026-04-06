# Payment Gateway Service

## Project Context

This is a **Secure Event-Driven Payment Gateway** built with Spring Boot 3.3.x / Java 21. It follows the same layered architecture, coding conventions, and testing patterns as the companion Recipe Management Service project.

The project is a technical assessment deliverable. It must demonstrate:
- Event-driven async processing
- Multi-tenant security
- Financial data integrity (idempotency, optimistic locking)
- Immutable audit trails
- Clean architecture with comprehensive tests

---

## Coding Conventions (MUST follow)

### General Style
- **Java 21** — use modern language features (records for simple data, pattern matching, switch expressions, text blocks where appropriate)
- **Lombok** — use `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Slf4j` for entities and DTOs
- **Constructor injection** via `@AllArgsConstructor` on services/controllers (no `@Autowired` on fields)
- **Concise methods** — keep methods short; extract filters into small private methods
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
- Use `@PrePersist`/`@PreUpdate` for timestamps
- Use `@Version` for optimistic locking on mutable entities
- Use `@Builder.Default` for collection defaults
- Use `@EntityGraph` on repository methods to avoid N+1

### DTO Conventions
- Use Bean Validation annotations: `@NotBlank`, `@NotNull`, `@DecimalMin`, `@Size`, etc.
- DTOs are POJOs with Lombok — no business logic
- Use `@Builder` on response DTOs

### Service Conventions
- `@Transactional` on write methods
- `@Transactional(readOnly = true)` on read methods
- Use `@Slf4j` for logging
- Mask sensitive data using `MaskingUtil` before logging

### Controller Conventions
- Use `@Valid @RequestBody` for input validation
- Extract client ID from `Authentication.getName()`
- Return `202 Accepted` for async ingestion, `200` for reads, `204` for deletes
- Use OpenAPI `@Operation` and `@Tag` annotations
- Clamp page size to prevent unbounded queries

### Exception Handling
- Custom runtime exceptions for domain errors
- `@RestControllerAdvice` with `GlobalExceptionHandler`
- Consistent `ErrorResponse` with timestamp, status, error, message, path
- Never leak stack traces or internal details in error responses

### Mapper Conventions
- `@Component` Spring beans (not MapStruct)
- `toDTO()`, `toEntityForCreate()`, `updateEntity()` method naming
- Repository lookups NOT done in mappers — they stay in the service layer



---

## Architecture Rules

### Security
1. All `/api/**` endpoints require authentication
2. Client ID is extracted from authentication principal (JWT `sub` claim or dev token mapping)
3. Every payment query filters by `clientId` — no cross-tenant access
4. Dev profile supports multiple test tokens: `client-a-token` → `client-a`, `client-b-token` → `client-b`
5. Production uses OAuth2 Resource Server with JWT validation

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

When extending or reviewing this code, keep these trade-offs in mind:

1. **Spring ApplicationEvent vs Kafka**: Chose in-process events for simplicity. In production, use a durable broker with retry/DLQ.
2. **H2 vs PostgreSQL**: In-memory H2 for zero-setup demo. Schema is SQL-standard and portable.
3. **Manual mappers vs MapStruct**: Manual for transparency and fewer build dependencies.
4. **Thread.sleep in BankSimulator**: Simulates real network latency. In production, this would be an HTTP client call.
5. **Random fraud/bank outcomes**: Demonstrates resilience. Production would use real ML models and bank APIs.

---

## How to Extend

### Adding a new payment state
1. Add value to `PaymentStatus` enum
2. Update `PaymentProcessingService` pipeline logic
3. Update state diagram in README and ARCHITECTURE docs

### Adding a new API endpoint
1. Add DTO (request/response) in `dto/`
2. Add service method in `PaymentService`
3. Add controller method in `PaymentController`
4. Add tests (unit + integration)

### Adding a new tenant
1. Insert into `clients` table (or add to `data.sql`)
2. If using dev profile, add token mapping in `SecurityConfig.DEV_TOKENS`

### Replacing fraud service with external call
1. Create a REST client (WebClient/RestTemplate) in `FraudAssessmentService`
2. Keep the same `FraudCheckResult` return type
3. Add circuit breaker (Resilience4j) for fault tolerance

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
  ```yaml
  management.endpoints.web.exposure.include= health, metrics, loggers, info
  management.endpoint.health.show-details= always


### OpenAPI Documentation

The project includes **OpenAPI Documentation** for API testing and exploration using Swagger.

#### Key Features:
- **Swagger UI**: Accessible at `/swagger-ui.html` (enabled in `dev` profile).
- **OpenAPI JSON**: Available at `/v3/api-docs`.

#### Configuration:
- OpenAPI is enabled in the `dev` profile for testing and development.
