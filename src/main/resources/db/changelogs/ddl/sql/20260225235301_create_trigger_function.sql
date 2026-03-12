--liquibase formatted sql
--changeset owner:20260225235301_create_trigger_function splitStatements:false
--comment: Create trigger function for updated_at

CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $function$
BEGIN
    IF NEW IS DISTINCT FROM OLD THEN
        NEW.updated_at = NOW();
    END IF;
    RETURN NEW;
END;
$function$ LANGUAGE plpgsql;

COMMENT ON FUNCTION trigger_set_updated_at IS 'Automatically sets updated_at timestamp on UPDATE operations';
