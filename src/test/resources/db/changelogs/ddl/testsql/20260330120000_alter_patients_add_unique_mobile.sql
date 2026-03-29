--liquibase formatted sql

--changeset owner-pratik:20260330120000_alter_patients_add_unique_mobile
--comment Add unique constraint on (hospital_id, mobile_number) for patients table

-- Drop the existing non-unique index
DROP INDEX IF EXISTS idx_patients_mobile;

-- Add unique constraint (per hospital, mobile_number must be unique)
ALTER TABLE patients
    ADD CONSTRAINT uq_patients_hospital_mobile UNIQUE (hospital_id, mobile_number);
