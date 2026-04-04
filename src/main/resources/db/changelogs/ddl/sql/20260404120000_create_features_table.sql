--liquibase formatted sql
--changeset owner-pratik:20260404120000_create_features_table
--comment: Create features table for role-based feature flags (DDL)

CREATE TABLE features (
    feature_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_key VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('RECEPTIONIST', 'ASSISTANT', 'DOCTOR', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL,
    CONSTRAINT uq_feature_key_role UNIQUE (feature_key, role)
);

CREATE INDEX idx_features_role ON features(role);
CREATE INDEX idx_features_key ON features(feature_key);

CREATE TRIGGER set_updated_at_features
    BEFORE UPDATE ON features
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE features IS 'Role-based feature flags - each role has its own feature configuration';
COMMENT ON COLUMN features.feature_key IS 'Feature identifier (e.g., SEARCH_PATIENTS, ADD_NEW_PATIENT)';
COMMENT ON COLUMN features.role IS 'User role: RECEPTIONIST, ASSISTANT, DOCTOR, ADMIN';
COMMENT ON COLUMN features.enabled IS 'Whether the feature is enabled for this role';
