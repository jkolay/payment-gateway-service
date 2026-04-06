# Payment Gateway Service

## Overview
The Payment Gateway Service is a Spring Boot application designed to handle payment processing, fraud assessment, and bank integration. It provides a secure and scalable solution for managing payments with a robust state machine, audit logging, and tenant isolation.

---

## Features

### Core Features
- **Payment Processing**: Handles payment initiation, fraud checks, and bank processing.
- **State Machine**: Tracks payment status transitions (e.g., `INITIATED → COMPLETED`).
- **Tenant Isolation**: Ensures client-specific data access.
- **Audit Logging**: Records all status transitions for financial integrity.

### Additional Features
- **Spring Boot Actuator**: Provides health, metrics, and logging endpoints.
- **OpenAPI Documentation**: Swagger UI for API testing and exploration.
- **Async Processing**: Event-driven architecture for fraud and bank processing.
- **Security**: OAuth2 Resource Server with JWT validation (production) and dev token mapping (development).
- **Database Support**: H2 for testing and PostgreSQL for production.

---

## Architecture

### Security
- All `/api/**` endpoints require authentication.
- Client ID is extracted from the JWT `sub` claim or dev token mapping.
- Dev profile supports multiple test tokens for simplified testing.

### Event-Driven Processing
- Payments are processed asynchronously using `@Async` and Spring Application Events.
- Stages: Fraud Assessment → Bank Processing → Final Status.

### Financial Integrity
- **Idempotency**: Prevents duplicate payment creation.
- **Optimistic Locking**: Ensures data consistency during concurrent updates.
- **Audit Trail**: Immutable logs for all status transitions.

---

## Endpoints

### Payment Endpoints
- **POST /payments**: Initiate a new payment.
- **GET /payments/{id}**: Retrieve payment details.
- **GET /payments**: List all payments for the authenticated client.

### Actuator Endpoints
- `/actuator/health`: Application health status.
- `/actuator/metrics`: Application metrics.
- `/actuator/loggers`: Manage log levels.
- `/actuator/info`: Application-specific information.

---

## Configuration

### Profiles
- **dev**: Simplified authentication with token mappings.
- **prod**: OAuth2 Resource Server with JWT validation.

### Async Configuration
- Thread pool for async processing:
  ```yaml
  spring:
    task:
      execution:
        pool:
          core-size: 5
          max-size: 10
          queue-capacity: 50ment State Machine


### Additional Features

- **Spring Boot Actuator**: Provides health, metrics, and logging endpoints.
- **OpenAPI Documentation**: Swagger UI for API testing and exploration.
    - **Swagger UI**: Accessible at `/swagger-ui.html` (enabled in `dev` profile).
    - **OpenAPI JSON**: Available at `/v3/api-docs`.
    - **Configuration**:
        - OpenAPI is enabled in the `dev` profile for testing and development.
        