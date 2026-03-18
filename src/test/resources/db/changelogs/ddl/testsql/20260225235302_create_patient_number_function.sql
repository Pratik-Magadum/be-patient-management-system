--liquibase formatted sql
--changeset owner:25022026235300_create_patient_number_function splitStatements:false
--comment: Create patient number generation function

CREATE OR REPLACE FUNCTION generate_patient_number(p_hospital_id UUID)
RETURNS VARCHAR(50) AS $function$
DECLARE
    v_year VARCHAR(4);
    v_count INTEGER;
    v_patient_number VARCHAR(50);
BEGIN
    v_year := TO_CHAR(NOW(), 'YYYY');
    
    SELECT COUNT(*) + 1 INTO v_count
    FROM patients
    WHERE EXTRACT(YEAR FROM created_at) = EXTRACT(YEAR FROM NOW());
    
    v_patient_number := 'PT-' || v_year || '-' || LPAD(v_count::TEXT, 5, '0');
    
    RETURN v_patient_number;
END;
$function$ LANGUAGE plpgsql;
