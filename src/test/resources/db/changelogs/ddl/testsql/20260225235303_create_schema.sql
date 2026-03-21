--liquibase formatted sql
--changeset owner:25022026235300_create_schema
--comment: Create schema (DDL)

CREATE EXTENSION IF NOT EXISTS "pg_trgm";


CREATE TABLE hospitals (
    hospital_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    subdomain VARCHAR(50) UNIQUE NOT NULL,
    address TEXT,
    contact_email VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL
);

CREATE INDEX idx_hospitals_subdomain ON hospitals(subdomain);
CREATE INDEX idx_hospitals_active ON hospitals(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_hospitals_updated_at ON hospitals(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_hospitals
    BEFORE UPDATE ON hospitals
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE hospitals IS 'Master tenant table - each hospital is a separate tenant';
COMMENT ON COLUMN hospitals.subdomain IS 'Unique subdomain for hospital (e.g., apollo-eye)';
COMMENT ON COLUMN hospitals.updated_at IS 'Auto-updated on any modification, NULL if never modified';


CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(50) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('RECEPTIONIST', 'ASSISTANT', 'DOCTOR', 'ADMIN')),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL  
);
ALTER TABLE users ADD CONSTRAINT uq_users_hospital_username UNIQUE (hospital_id, username);
CREATE INDEX idx_users_hospital_id ON users(hospital_id);
CREATE INDEX idx_users_username ON users(hospital_id, username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(hospital_id, role);
CREATE INDEX idx_users_active ON users(hospital_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_users_updated_at ON users(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_users
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE users IS 'System users with role-based access';
COMMENT ON COLUMN users.role IS 'User role: RECEPTIONIST, ASSISTANT, DOCTOR, ADMIN';


CREATE TABLE patients (
    patient_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    patient_number VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    age INTEGER,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    email VARCHAR(100),
    date_of_birth DATE,
    address TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL 
);

CREATE INDEX idx_patients_hospital_id ON patients(hospital_id);
CREATE INDEX idx_patients_hospital_created ON patients(hospital_id, created_at DESC);
CREATE INDEX idx_patients_number ON patients(patient_number);
CREATE INDEX idx_patients_mobile ON patients(hospital_id, mobile_number);
CREATE INDEX idx_patients_updated_at ON patients(updated_at) WHERE updated_at IS NOT NULL;
CREATE INDEX idx_patients_name_trgm ON patients USING gin(full_name gin_trgm_ops); 

CREATE TRIGGER set_updated_at_patients
    BEFORE UPDATE ON patients
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE patients IS 'Patient master records';
COMMENT ON COLUMN patients.patient_number IS 'Auto-generated unique patient number per hospital';


CREATE TABLE appointments (
    appointment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(patient_id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(50) DEFAULT 'REGISTERED' CHECK (status IN ('REGISTERED', 'IN_PROGRESS', 'COMPLETED')),
    notes TEXT,
    created_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL 
);

CREATE INDEX idx_appointments_hospital_id ON appointments(hospital_id);
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX idx_appointments_date ON appointments(hospital_id, appointment_date DESC);
CREATE INDEX idx_appointments_status ON appointments(hospital_id, status, appointment_date);
CREATE INDEX idx_appointments_doctor_date ON appointments(doctor_id, appointment_date, status);
CREATE INDEX idx_appointments_updated_at ON appointments(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_appointments
    BEFORE UPDATE ON appointments
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE appointments IS 'Patient appointment records';
COMMENT ON COLUMN appointments.status IS 'Status: REGISTERED, IN_PROGRESS, COMPLETED';

CREATE TABLE pre_diagnostic_tests (
    test_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    right_eye_vision VARCHAR(50),
    left_eye_vision VARCHAR(50),
    near_vision VARCHAR(50),
    distance_vision VARCHAR(50),
    eye_pressure_right DECIMAL(5,2),
    eye_pressure_left DECIMAL(5,2),
    observations TEXT,
    entered_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL 
);

CREATE INDEX idx_prediag_hospital_id ON pre_diagnostic_tests(hospital_id);
CREATE INDEX idx_prediag_appointment_id ON pre_diagnostic_tests(appointment_id);
CREATE INDEX idx_prediag_entered_by ON pre_diagnostic_tests(entered_by);
CREATE INDEX idx_prediag_updated_at ON pre_diagnostic_tests(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_prediag
    BEFORE UPDATE ON pre_diagnostic_tests
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE pre_diagnostic_tests IS 'Pre-diagnostic test results entered by assistants';


CREATE TABLE consultations (
    consultation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    diagnosis_notes TEXT,
    specs_sph_right DECIMAL(5,2),
    specs_sph_left DECIMAL(5,2),
    specs_cyl_right DECIMAL(5,2),
    specs_cyl_left DECIMAL(5,2),
    specs_axis_right INTEGER CHECK (specs_axis_right >= 0 AND specs_axis_right <= 180),
    specs_axis_left INTEGER CHECK (specs_axis_left >= 0 AND specs_axis_left <= 180),
    pupillary_distance DECIMAL(5,2),
    follow_up_date DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL 
);

CREATE INDEX idx_consultations_hospital_id ON consultations(hospital_id);
CREATE INDEX idx_consultations_appointment_id ON consultations(appointment_id);
CREATE INDEX idx_consultations_doctor_id ON consultations(doctor_id);
CREATE INDEX idx_consultations_doctor_date ON consultations(doctor_id, created_at DESC);
CREATE INDEX idx_consultations_updated_at ON consultations(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_consultations
    BEFORE UPDATE ON consultations
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE consultations IS 'Doctor consultation records with diagnosis and prescriptions';


CREATE TABLE medicines (
    medicine_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    category VARCHAR(100),
    standard_dosage VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL 
);

CREATE INDEX idx_medicines_name ON medicines(name);
CREATE INDEX idx_medicines_category ON medicines(category);
CREATE INDEX idx_medicines_active ON medicines(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_medicines_updated_at ON medicines(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_medicines
    BEFORE UPDATE ON medicines
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE medicines IS 'Master medicine catalog - shared across all hospitals';


CREATE TABLE prescriptions (
    prescription_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    consultation_id UUID NOT NULL REFERENCES consultations(consultation_id) ON DELETE CASCADE,
    medicine_id UUID NOT NULL REFERENCES medicines(medicine_id) ON DELETE RESTRICT,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    instructions TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP NULL  
);

CREATE INDEX idx_prescriptions_hospital_id ON prescriptions(hospital_id);
CREATE INDEX idx_prescriptions_consultation_id ON prescriptions(consultation_id);
CREATE INDEX idx_prescriptions_medicine_id ON prescriptions(medicine_id);
CREATE INDEX idx_prescriptions_updated_at ON prescriptions(updated_at) WHERE updated_at IS NOT NULL;

CREATE TRIGGER set_updated_at_prescriptions
    BEFORE UPDATE ON prescriptions
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

COMMENT ON TABLE prescriptions IS 'Medicine prescriptions linked to consultations';


CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(hospital_id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_hospital_id ON audit_logs(hospital_id);
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_hospital_date ON audit_logs(hospital_id, created_at DESC);

COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail - immutable, no updates allowed';


CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

COMMENT ON TABLE refresh_tokens IS 'Stores refresh tokens for JWT-based authentication';


ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE pre_diagnostic_tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE consultations ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_patients ON patients
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);

CREATE POLICY tenant_isolation_users ON users
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);

CREATE POLICY tenant_isolation_appointments ON appointments
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);

CREATE POLICY tenant_isolation_prediag ON pre_diagnostic_tests
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);

CREATE POLICY tenant_isolation_consultations ON consultations
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);

CREATE POLICY tenant_isolation_prescriptions ON prescriptions
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);

CREATE POLICY tenant_isolation_audit ON audit_logs
    FOR ALL USING (hospital_id = current_setting('app.hospital_id', true)::UUID);
