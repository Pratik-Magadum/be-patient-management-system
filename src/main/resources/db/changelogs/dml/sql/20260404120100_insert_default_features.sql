--liquibase formatted sql
--changeset owner-pratik:20260404120100_insert_default_features
--comment: Insert default role-based feature flags (DML)

-- ADMIN: all features enabled
INSERT INTO features (feature_key, role, enabled, description) VALUES
    ('SEARCH_PATIENTS',    'ADMIN', TRUE,  'Search patients by name or phone'),
    ('DATE_RANGE_FILTER',  'ADMIN', TRUE,  'Filter records by date range'),
    ('STATS_CARDS',        'ADMIN', TRUE,  'Dashboard statistics cards'),
    ('ADD_NEW_PATIENT',    'ADMIN', TRUE,  'Register new patient'),
    ('EDIT_PATIENT',       'ADMIN', TRUE,  'Edit existing patient details'),
    ('DELETE_PATIENT',     'ADMIN', TRUE,  'Delete patient records'),
    ('FOLLOW_UP',          'ADMIN', TRUE,  'Follow-up appointment booking'),
    ('STATUS_MANAGEMENT',  'ADMIN', TRUE,  'Appointment status transitions'),
    ('PAGINATION',         'ADMIN', TRUE,  'Paginated list views');

-- RECEPTIONIST: patient & appointment management, no edit/delete
INSERT INTO features (feature_key, role, enabled, description) VALUES
    ('SEARCH_PATIENTS',    'RECEPTIONIST', TRUE,  'Search patients by name or phone'),
    ('DATE_RANGE_FILTER',  'RECEPTIONIST', TRUE,  'Filter records by date range'),
    ('STATS_CARDS',        'RECEPTIONIST', TRUE,  'Dashboard statistics cards'),
    ('ADD_NEW_PATIENT',    'RECEPTIONIST', TRUE,  'Register new patient'),
    ('EDIT_PATIENT',       'RECEPTIONIST', FALSE, 'Edit existing patient details'),
    ('DELETE_PATIENT',     'RECEPTIONIST', FALSE, 'Delete patient records'),
    ('FOLLOW_UP',          'RECEPTIONIST', TRUE,  'Follow-up appointment booking'),
    ('STATUS_MANAGEMENT',  'RECEPTIONIST', TRUE,  'Appointment status transitions'),
    ('PAGINATION',         'RECEPTIONIST', TRUE,  'Paginated list views');

-- DOCTOR: view-oriented, status management, no patient add/edit/delete
INSERT INTO features (feature_key, role, enabled, description) VALUES
    ('SEARCH_PATIENTS',    'DOCTOR', TRUE,  'Search patients by name or phone'),
    ('DATE_RANGE_FILTER',  'DOCTOR', TRUE,  'Filter records by date range'),
    ('STATS_CARDS',        'DOCTOR', TRUE,  'Dashboard statistics cards'),
    ('ADD_NEW_PATIENT',    'DOCTOR', FALSE, 'Register new patient'),
    ('EDIT_PATIENT',       'DOCTOR', FALSE, 'Edit existing patient details'),
    ('DELETE_PATIENT',     'DOCTOR', FALSE, 'Delete patient records'),
    ('FOLLOW_UP',          'DOCTOR', TRUE,  'Follow-up appointment booking'),
    ('STATUS_MANAGEMENT',  'DOCTOR', TRUE,  'Appointment status transitions'),
    ('PAGINATION',         'DOCTOR', TRUE,  'Paginated list views');

-- ASSISTANT: limited access, no patient CRUD or status management
INSERT INTO features (feature_key, role, enabled, description) VALUES
    ('SEARCH_PATIENTS',    'ASSISTANT', TRUE,  'Search patients by name or phone'),
    ('DATE_RANGE_FILTER',  'ASSISTANT', FALSE, 'Filter records by date range'),
    ('STATS_CARDS',        'ASSISTANT', FALSE, 'Dashboard statistics cards'),
    ('ADD_NEW_PATIENT',    'ASSISTANT', FALSE, 'Register new patient'),
    ('EDIT_PATIENT',       'ASSISTANT', FALSE, 'Edit existing patient details'),
    ('DELETE_PATIENT',     'ASSISTANT', FALSE, 'Delete patient records'),
    ('FOLLOW_UP',          'ASSISTANT', FALSE, 'Follow-up appointment booking'),
    ('STATUS_MANAGEMENT',  'ASSISTANT', TRUE,  'Appointment status transitions'),
    ('PAGINATION',         'ASSISTANT', TRUE,  'Paginated list views');
