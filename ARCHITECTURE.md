# Architecture & Design Documentation

## System Overview

The Payment Gateway Service is a secure, event-driven backend engine for payment processing. It provides high-throughput payment ingestion, asynchronous processing through fraud assessment and bank simulation phases, immutable audit trails, and multi-tenant client isolation.

## High-level Design

### Payment Processing Pipeline

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌───────────┐
│   Client     │────►│ REST API     │────►│ Event Publisher   │────►│ Fraud Assessment │────►│ Bank Sim  │
│   (Tenant)   │◄────│ (Immediate   │     │ (Async dispatch)  │     │ (Rule engine)    │     │ (Latency  │
│              │ 202 │  ACK)        │     │                  │     │                  │     │  + random │
└─────────────┘     └──────────────┘     └──────────────────┘     └──────────────────┘     │  outcome) │
                                                                                           └───────────┘
```

### Layered Architecture

```
┌───────────────────────────────────────┐
│      API Layer (Controller)           │
│   - HTTP Request/Response             │
│   - Input Validation                  │
│   - Tenant extraction from auth       │
│   - OpenAPI Annotations               │
└──────────────────────┬────────────────┘
                       │
┌──────────────────────▼────────────────┐
│    Business Logic Layer (Service)     │
│   - Payment Ingestion                 │
│   - Status Inquiry                    │
│   - State Transitions + Audit         │
│   - Event Publishing                  │
│   - Transaction Management            │
└──────────────────────┬────────────────┘
                       │
┌──────────────────────▼────────────────┐
│  Event / Async Layer                  │
│   - PaymentEvent + Listener           │
│   - PaymentProcessingService          │
│   - FraudAssessmentService            │
│   - BankSimulatorService              │
└──────────────────────┬────────────────┘
                       │
┌──────────────────────▼────────────────┐
│   Data Access Layer (Repository)      │
│   - Spring Data JPA                   │
│   - Tenant-scoped queries             │
└──────────────────────┬────────────────┘
                       │
┌──────────────────────▼────────────────┐
│      Database (H2 In-Memory)          │
│   - payments                          │
│   - payment_audit_logs                │
│   - clients                           │
└───────────────────────────────────────┘
```

## Architecture Decisions

### ADR-01: Stateless API for horizontal scaling
**Decision**: Controllers/services remain stateless. Sessions are disabled.

### ADR-02: DTO boundary for API contracts
**Decision**: Public API uses DTOs; persistence entities are internal. PaymentMapper handles conversions.

### ADR-03: Event-driven async processing
**Decision**: Spring ApplicationEvent + @Async for decoupled payment processing.
**Scale-out path**: Replace with Kafka/RabbitMQ for durable messaging with retry and DLQ.

### ADR-04: Idempotency at the database level
**Decision**: Unique constraint on (client_id, idempotency_key) prevents duplicate payments atomically.

### ADR-05: Multi-tenant isolation
**Decision**: Client ID extracted from JWT claims. All queries filter by client ID. No cross-tenant data access possible.

### ADR-06: Immutable audit trail
**Decision**: PaymentAuditLog entries are append-only. Every state change recorded for compliance.

### ADR-07: Optimistic locking
**Decision**: @Version on Payment entity prevents lost updates under concurrent access.

### ADR-08: Defense-in-depth security
**Decision**: Stateless sessions, JWT auth, tenant isolation, input validation, masked logging, generic error responses.

### ADR-09: Pluggable fraud assessment
**Decision**: FraudAssessmentService has configurable rules. Easy to swap for external microservice call.

### ADR-10: Resilient bank simulation
**Decision**: Variable latency + random outcomes. System handles both success and failure paths gracefully.

### ADR-11: Connection pooling and bounded resources
**Decision**: Use HikariCP defaults with bounded async thread pool (4 core, 8 max, 100 queue).

### ADR-12: Health checks
**Decision**: Actuator health/info endpoints for liveness/readiness probes.

## Design Patterns

### 1. Event-Driven Architecture
- `PaymentEvent` published on ingestion
- `PaymentEventListener` processes asynchronously
- Clean separation of request handling from processing

### 2. Pipeline / Chain
- `PaymentProcessingService` orchestrates: Fraud → Bank → Final status
- Each phase is a distinct service with clear contracts

### 3. Strategy Pattern (Fraud Rules)
- `FraudAssessmentService` encapsulates pluggable fraud rules
- Easy to extend with new rules or swap for external service

### 4. DTO Pattern
- `PaymentInitiationRequest/Response`: Ingestion contract
- `PaymentStatusResponse`: Status inquiry contract
- `PaymentAuditLogDTO`: Audit trail entries

### 5. Repository Pattern
- `PaymentRepository`: Tenant-scoped queries
- Spring Data JPA abstracts data access

### 6. Global Exception Handler
- Centralized error handling with consistent response format
- No stack traces leaked to clients

## Data Model

### Entity Relationships

```
clients (1) ──────── (*) payments (1) ──── (*) payment_audit_logs
```

### Payment States

```
INITIATED ──► FRAUD_CHECK_PENDING ──► FRAUD_CHECK_PASSED ──► PROCESSING ──► COMPLETED
                                  └──► FRAUD_CHECK_FAILED                └──► FAILED
```

## API Design

### RESTful Endpoints

| Operation | Method | Endpoint | Status Code |
|-----------|--------|----------|-------------|
| Initiate Payment | POST | `/api/v1/payments` | 202 (Accepted) |
| Get Status | GET | `/api/v1/payments/{trackingId}` | 200 |
| List Payments | GET | `/api/v1/payments` | 200 |

### Security

- All `/api/**` endpoints require authentication
- Swagger UI, actuator, and H2 console are publicly accessible
- Dev profile: Bearer tokens mapped to client IDs
- Production: JWT with `client_id` claim

## Testing Strategy

- **Unit tests**: Service layer with Mockito (deterministic, fast)
- **Integration tests**: Full Spring context + MockMvc (end-to-end verification)
- **Repository tests**: @DataJpaTest with TestEntityManager (isolated DB tests)
- **Coverage**: JaCoCo reports generated on `mvn test`

## Developer Conventions

### Mapping (Entity ↔ DTO)
- `com.payment.mapper.PaymentMapper` handles all conversions
- Repository lookups and business logic remain in the service layer

### Logging
- Use `MaskingUtil.maskTrackingId()` for tracking IDs in logs
- Never log full payment amounts or client details at INFO level
- Use SLF4J with `@Slf4j` annotation

### Naming
- Entities use full names (Payment, PaymentAuditLog, Client)
- DTOs suffixed with intent (PaymentInitiationRequest, PaymentStatusResponse)
- Services suffixed with Service
- Repositories suffixed with Repository
 