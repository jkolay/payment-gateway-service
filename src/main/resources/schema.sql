CREATE TABLE clients (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         client_id VARCHAR(255) NOT NULL UNIQUE,
                         name VARCHAR(255) NOT NULL,
                         active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE payments (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          tracking_id VARCHAR(36) NOT NULL UNIQUE,
                          idempotency_key VARCHAR(255) NOT NULL,
                          client_id VARCHAR(255) NOT NULL,
                          amount DECIMAL(19,4) NOT NULL,
                          currency VARCHAR(3) NOT NULL,
                          description TEXT,
                          status VARCHAR(50) NOT NULL,
                          failure_reason TEXT,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL,
                          version BIGINT DEFAULT 0,
                          CONSTRAINT uk_payment_idempotency UNIQUE (client_id, idempotency_key)
);

CREATE INDEX idx_payments_tracking_id ON payments(tracking_id);
CREATE INDEX idx_payments_client_id ON payments(client_id);

CREATE TABLE payment_audit_logs (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    payment_id BIGINT NOT NULL,
                                    previous_status VARCHAR(50) NOT NULL,
                                    new_status VARCHAR(50) NOT NULL,
                                    detail TEXT,
                                    timestamp TIMESTAMP NOT NULL,
                                    CONSTRAINT fk_audit_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_payment_id ON payment_audit_logs(payment_id);
