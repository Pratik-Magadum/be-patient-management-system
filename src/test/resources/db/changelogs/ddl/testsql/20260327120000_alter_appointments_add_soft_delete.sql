--liquibase formatted sql
--changeset owner:20260327120000_alter_appointments_add_soft_delete
--comment: Add soft-delete columns (is_deleted, deleted_at) to appointments table

ALTER TABLE appointments ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE appointments ADD COLUMN deleted_at TIMESTAMP NULL;

-- Partial index to speed up "active appointments only" queries
CREATE INDEX idx_appointments_not_deleted ON appointments(hospital_id) WHERE is_deleted = false;
