--liquibase formatted sql
--changeset owner-pratik:20261203120000_insert_patient_dashboard_demo_data
--comment: Insert demo patients and appointments for patient dashboard testing

-- ============================================================================
-- Patients for  Eye Hospital (hospital_id = a0eebc99-...)
-- ============================================================================
INSERT INTO patients (patient_id, hospital_id, patient_number, full_name, mobile_number, email, date_of_birth, age, gender, address)
VALUES
    ('d1aab001-1111-4aaa-b111-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00001', 'Amit Patel', '+91-9876543001', 'amit.patel@email.com', '1985-06-15', 40, 'MALE', '12 MG Road, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000002', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00002', 'Sunita Devi', '+91-9876543002', 'sunita.devi@email.com', '1990-03-22', 35, 'FEMALE', '45 Park Street, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000003', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00003', 'Rajesh Sharma', '+91-9876543003', 'rajesh.sharma@email.com', '1978-11-10', 47, 'MALE', '78 Lal Bagh, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000004', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00004', 'Meena Kumari', '+91-9876543004', 'meena.kumari@email.com', '1995-01-30', 31, 'FEMALE', '23 Anna Nagar, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000005', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00005', 'Vikram Singh', '+91-9876543005', 'vikram.singh@email.com', '1982-09-05', 43, 'MALE', '56 Civil Lines, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000006', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00006', 'Priya Nair', '+91-9876543006', 'priya.nair@email.com', '1992-07-18', 33, 'FEMALE', '89 Marine Drive, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000007', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00007', 'Ravi Verma', '+91-9876543007', 'ravi.verma@email.com', '1988-12-25', 37, 'MALE', '34 Juhu Beach, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000008', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00008', 'Anita Desai', '+91-9876543008', 'anita.desai@email.com', '1975-04-12', 50, 'FEMALE', '67 Bandra West, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000009', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00009', 'Suresh Reddy', '+91-9876543009', 'suresh.reddy@email.com', '1998-08-20', 27, 'MALE', '90 Worli Sea Face, Mumbai'),
    ('d1aab001-1111-4aaa-b111-000000000010', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PT-2026-00010', 'Kavita Joshi', '+91-9876543010', 'kavita.joshi@email.com', '1980-02-14', 46, 'FEMALE', '12 Powai Lake, Mumbai');

-- ============================================================================
-- Today's Appointments for  Eye Hospital
-- Using CURRENT_DATE so they are always "today's" appointments for testing
-- ============================================================================

-- 6 New Visit appointments (visit_type = NEW_VISIT)
-- 3 of these are COMPLETED, 2 are IN_PROGRESS, 1 is REGISTERED
INSERT INTO appointments (appointment_id, hospital_id, patient_id, doctor_id, appointment_date, appointment_time, status, visit_type, notes, created_by)
VALUES
    -- New visits
    ('e2bbc001-2222-4bbb-c222-000000000001', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000001',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '09:00:00', 'COMPLETED', 'NEW_VISIT', 'Routine eye checkup', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000002', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000002',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '09:30:00', 'COMPLETED', 'NEW_VISIT', 'Blurry vision complaint', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000003', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000003',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '10:00:00', 'COMPLETED', 'NEW_VISIT', 'Eye redness and irritation', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000004', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000004',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '10:30:00', 'IN_PROGRESS', 'NEW_VISIT', 'Glaucoma screening', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000005', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000005',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '11:00:00', 'IN_PROGRESS', 'NEW_VISIT', 'Contact lens fitting', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000006', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000006',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '11:30:00', 'REGISTERED', 'NEW_VISIT', 'First consultation', (SELECT user_id FROM users WHERE username = 'receptionist1'));

-- Create a past appointment to serve as parent for follow-ups
INSERT INTO appointments (appointment_id, hospital_id, patient_id, doctor_id, appointment_date, appointment_time, status, visit_type, notes, created_by)
VALUES
    ('e2bbc001-2222-4bbb-c222-000000000099', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000007',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE - INTERVAL '14 days', '09:00:00', 'COMPLETED', 'NEW_VISIT', 'Initial glaucoma diagnosis', (SELECT user_id FROM users WHERE username = 'receptionist1')),
    ('e2bbc001-2222-4bbb-c222-000000000098', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000008',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE - INTERVAL '7 days', '10:00:00', 'COMPLETED', 'NEW_VISIT', 'Cataract evaluation', (SELECT user_id FROM users WHERE username = 'receptionist1')),
    ('e2bbc001-2222-4bbb-c222-000000000097', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000009',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE - INTERVAL '10 days', '11:00:00', 'COMPLETED', 'NEW_VISIT', 'Retina check', (SELECT user_id FROM users WHERE username = 'receptionist1')),
    ('e2bbc001-2222-4bbb-c222-000000000096', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000010',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE - INTERVAL '5 days', '14:00:00', 'COMPLETED', 'NEW_VISIT', 'Dry eyes treatment', (SELECT user_id FROM users WHERE username = 'receptionist1'));

-- 4 Follow-up appointments today (visit_type = FOLLOW_UP)
-- 2 COMPLETED, 1 IN_PROGRESS, 1 REGISTERED
INSERT INTO appointments (appointment_id, hospital_id, patient_id, doctor_id, appointment_date, appointment_time, status, visit_type, parent_appointment_id, notes, created_by)
VALUES
    ('e2bbc001-2222-4bbb-c222-000000000007', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000007',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '14:00:00', 'COMPLETED', 'FOLLOW_UP', 'e2bbc001-2222-4bbb-c222-000000000099', 'Glaucoma follow-up - pressure check', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000008', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000008',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '14:30:00', 'COMPLETED', 'FOLLOW_UP', 'e2bbc001-2222-4bbb-c222-000000000098', 'Post-cataract surgery review', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000009', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000009',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '15:00:00', 'IN_PROGRESS', 'FOLLOW_UP', 'e2bbc001-2222-4bbb-c222-000000000097', 'Retina follow-up imaging', (SELECT user_id FROM users WHERE username = 'receptionist1')),

    ('e2bbc001-2222-4bbb-c222-000000000010', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd1aab001-1111-4aaa-b111-000000000010',
     (SELECT user_id FROM users WHERE username = 'drkumar'), CURRENT_DATE, '15:30:00', 'REGISTERED', 'FOLLOW_UP', 'e2bbc001-2222-4bbb-c222-000000000096', 'Dry eyes treatment follow-up', (SELECT user_id FROM users WHERE username = 'receptionist1'));

-- ============================================================================
-- Expected Dashboard Results for  Eye Hospital (today):
-- totalPatients         = 10 (6 new + 4 follow-up)
-- newPatients           = 6
-- followUpPatients      = 4
-- completedPatients     = 5  (3 new completed + 2 follow-up completed)
-- totalRegisteredPatients = 10
-- ============================================================================
