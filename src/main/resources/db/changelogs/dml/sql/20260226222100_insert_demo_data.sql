--liquibase formatted sql
--changeset owner:20260226222100_insert_dummy_data_into_schema
--comment: Insert initial sample demo data (DML)
INSERT INTO hospitals (hospital_id, name, subdomain, address, contact_email, contact_phone)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Eye Hospital', 'eye-hospital', '123 Main Street, Mumbai', 'contact@eye.com', '+91-22-12345678'),
    ('b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', 'City Eye Clinic', 'apollo-eye', '456 Park Avenue, Delhi', 'info@cityeye.com', '+91-11-87654321');

INSERT INTO medicines (name, generic_name, category, standard_dosage)
VALUES 
    ('Timolol Eye Drops', 'Timolol Maleate', 'Glaucoma Treatment', '0.5% - 1 drop twice daily'),
    ('Latanoprost', 'Latanoprost', 'Glaucoma Treatment', '0.005% - 1 drop once daily'),
    ('Moxifloxacin Eye Drops', 'Moxifloxacin', 'Antibiotic', '0.5% - 1 drop 3 times daily'),
    ('Artificial Tears', 'Carboxymethylcellulose', 'Lubricant', '1-2 drops as needed'),
    ('Prednisolone Eye Drops', 'Prednisolone Acetate', 'Anti-inflammatory', '1% - 1 drop 4 times daily');

INSERT INTO users (hospital_id, username, password, full_name, email, role)
VALUES 
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', '$2a$12$zSZx3tDdddFV48WWq22UOuf5ZvOWrP3VKQphDNlTEoH335HsyzybC', 'Admin User', 'admin@apolloeye.com', 'ADMIN'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'receptionist1', '$2a$12$zSZx3tDdddFV48WWq22UOuf5ZvOWrP3VKQphDNlTEoH335HsyzybC', 'Priya Sharma', 'priya@apolloeye.com', 'RECEPTIONIST'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'assistant1', '$2a$12$zSZx3tDdddFV48WWq22UOuf5ZvOWrP3VKQphDNlTEoH335HsyzybC', 'Rahul Kumar', 'rahul@apolloeye.com', 'ASSISTANT'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'drkumar', '$2a$12$zSZx3tDdddFV48WWq22UOuf5ZvOWrP3VKQphDNlTEoH335HsyzybC', 'Dr. Suresh Kumar', 'drkumar@apolloeye.com', 'DOCTOR');

