--liquibase formatted sql
--changeset owner:26022026222100_insert_dummy_data_into_schema
--comment: Insert initial sample demo data (DML)
INSERT INTO hospitals (hospital_id, name, subdomain, address, contact_email, contact_phone)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Apollo Eye Hospital', 'apollo-eye', '123 Main Street, Mumbai', 'contact@apolloeye.com', '+91-22-12345678'),
    ('b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', 'City Eye Clinic', 'city-eye', '456 Park Avenue, Delhi', 'info@cityeye.com', '+91-11-87654321');

INSERT INTO medicines (name, generic_name, category, standard_dosage)
VALUES 
    ('Timolol Eye Drops', 'Timolol Maleate', 'Glaucoma Treatment', '0.5% - 1 drop twice daily'),
    ('Latanoprost', 'Latanoprost', 'Glaucoma Treatment', '0.005% - 1 drop once daily'),
    ('Moxifloxacin Eye Drops', 'Moxifloxacin', 'Antibiotic', '0.5% - 1 drop 3 times daily'),
    ('Artificial Tears', 'Carboxymethylcellulose', 'Lubricant', '1-2 drops as needed'),
    ('Prednisolone Eye Drops', 'Prednisolone Acetate', 'Anti-inflammatory', '1% - 1 drop 4 times daily');

INSERT INTO users (hospital_id, username, password, full_name, email, role)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpSRelgyG', 'Admin User', 'admin@apolloeye.com', 'ADMIN'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'receptionist1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpSRelgyG', 'Priya Sharma', 'priya@apolloeye.com', 'RECEPTIONIST'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'assistant1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpSRelgyG', 'Rahul Kumar', 'rahul@apolloeye.com', 'ASSISTANT'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'drkumar', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIpSRelgyG', 'Dr. Suresh Kumar', 'drkumar@apolloeye.com', 'DOCTOR');

