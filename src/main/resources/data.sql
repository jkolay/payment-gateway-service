-- Seed clients (tenants)
MERGE INTO clients (client_id, name, active) KEY(client_id) VALUES ('client-a', 'Acme Corp', TRUE);
MERGE INTO clients (client_id, name, active) KEY(client_id) VALUES ('client-b', 'Beta Industries', TRUE);