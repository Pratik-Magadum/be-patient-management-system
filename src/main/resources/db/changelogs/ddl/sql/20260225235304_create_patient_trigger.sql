--liquibase formatted sql
--changeset owner:20260225235304_create_patient_trigger_function splitStatements:false
--comment: Create trigger function for auto-generating patient numbers

CREATE OR REPLACE FUNCTION trigger_generate_patient_number()
RETURNS TRIGGER AS $function$
BEGIN
    IF NEW.patient_number IS NULL OR NEW.patient_number = '' THEN
        NEW.patient_number := generate_patient_number(NEW.hospital_id);
    END IF;
    RETURN NEW;
END;
$function$ LANGUAGE plpgsql;

CREATE TRIGGER before_insert_patient_number
    BEFORE INSERT ON patients
    FOR EACH ROW
    EXECUTE FUNCTION trigger_generate_patient_number();
