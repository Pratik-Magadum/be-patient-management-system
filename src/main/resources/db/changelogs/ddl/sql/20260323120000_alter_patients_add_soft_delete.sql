--liquibase formatted sql
--changeset owner:20260323120000_alter_patients_add_soft_delete
--comment: Add soft-delete columns (is_deleted, deleted_at) to patients table

ALTER TABLE patients ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE patients ADD COLUMN deleted_at TIMESTAMP NULL;

-- Partial index to speed up "active patients only" queries
CREATE INDEX idx_patients_not_deleted ON patients(hospital_id) WHERE is_deleted = false;
