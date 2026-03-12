--liquibase formatted sql
--changeset owner:20260311120000_alter_appointments_add_visit_type
--comment: Add visit_type and parent_appointment_id to appointments for tracking new vs follow-up visits

-- Add visit_type column: NEW_VISIT (fresh case) or FOLLOW_UP (returning for same issue)
ALTER TABLE appointments
    ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'NEW_VISIT'
        CHECK (visit_type IN ('NEW_VISIT', 'FOLLOW_UP'));

-- Add self-referential FK to link follow-up appointments to their original appointment
ALTER TABLE appointments
    ADD COLUMN parent_appointment_id UUID REFERENCES appointments(appointment_id) ON DELETE SET NULL;

-- Cross-column constraint: NEW_VISIT must have NULL parent, FOLLOW_UP must have a parent
ALTER TABLE appointments
    ADD CONSTRAINT chk_visit_type_parent
        CHECK (
            (visit_type = 'NEW_VISIT' AND parent_appointment_id IS NULL)
            OR
            (visit_type = 'FOLLOW_UP' AND parent_appointment_id IS NOT NULL)
        );

-- Index for follow-up chain lookups (find all follow-ups of an appointment)
CREATE INDEX idx_appointments_parent_id ON appointments(parent_appointment_id)
    WHERE parent_appointment_id IS NOT NULL;

-- Index for filtering appointments by visit type per hospital
CREATE INDEX idx_appointments_visit_type ON appointments(hospital_id, visit_type);

COMMENT ON COLUMN appointments.visit_type IS 'Visit type: NEW_VISIT (fresh case or new issue), FOLLOW_UP (returning for prior appointment)';
COMMENT ON COLUMN appointments.parent_appointment_id IS 'References the original appointment for follow-up visits, NULL for new visits';
