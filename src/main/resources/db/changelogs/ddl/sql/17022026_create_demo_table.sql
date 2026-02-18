--liquibase formatted sql
--changeset owner:17022026_create_demo_table
--comment: Create demo table (DDL)
CREATE TABLE IF NOT EXISTS demo (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--rollback: DROP TABLE IF EXISTS demo;
