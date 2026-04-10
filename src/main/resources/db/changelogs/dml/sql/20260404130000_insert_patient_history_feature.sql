--liquibase formatted sql
--changeset owner-pratik:20260404130100_insert_default_features
--comment: Insert default role-based feature flags for testing (DML)

INSERT INTO features (feature_key, role, enabled, description) VALUES
    ('PATIENT_HISTORY',    'ADMIN', TRUE,  'View patient history'),
    ('PATIENT_HISTORY',    'RECEPTIONIST', TRUE,  'View patient history'),
    ('PATIENT_HISTORY',    'DOCTOR', TRUE,  'View patient history'),
    ('PATIENT_HISTORY',    'ASSISTANT', TRUE,  'View patient history');