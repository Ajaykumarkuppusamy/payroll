-- ===========================================================
-- V6: Add "AJ Pvt Ltd" Company (India) + Seed Employees
-- ===========================================================

-- 1. Add AJ Pvt Ltd as Company 3 under India (country_id = 1)
INSERT INTO company (id, country_id, name, registration_number, is_active)
VALUES (3, 1, 'AJ Pvt Ltd', 'REG-IN-99999', true)
ON CONFLICT DO NOTHING;

-- 2. Add Departments for AJ Pvt Ltd
INSERT INTO department (id, company_id, name, code, is_active)
VALUES
    (9,  3, 'Engineering',      'ENG',   true),
    (10, 3, 'Finance',          'FIN',   true),
    (11, 3, 'Human Resources',  'HR',    true),
    (12, 3, 'Product',          'PROD',  true)
ON CONFLICT (company_id, code) DO NOTHING;

-- 3. Grant Super Admin access to AJ Pvt Ltd
INSERT INTO user_company_access (id, user_id, company_id)
VALUES (4, 1, 3)
ON CONFLICT DO NOTHING;

-- 4. Grant HR Manager India access to AJ Pvt Ltd (optional — remove if not wanted)
-- INSERT INTO user_company_access (id, user_id, company_id) VALUES (5, 2, 3) ON CONFLICT DO NOTHING;

-- 5. Seed 10 Employees for AJ Pvt Ltd
INSERT INTO employee (id, company_id, department_id, employee_code, first_name, last_name,
    email, base_salary, bank_account_number, bank_name, bank_ifsc_code,
    tax_id, employment_status, hire_date, termination_date)
VALUES
    (21, 3,  9, 'AJ001', 'Aditya',    'Joshi',      'aditya.joshi@ajpvt.com',     95000.00,  '11223344551', 'HDFC Bank',              'HDFC0009001', 'PANAJ00001', 'ACTIVE',    '2024-01-15', null),
    (22, 3,  9, 'AJ002', 'Kavitha',   'Nair',       'kavitha.nair@ajpvt.com',     88000.00,  '11223344552', 'State Bank of India',    'SBIN0009002', 'PANAJ00002', 'ACTIVE',    '2024-02-10', null),
    (23, 3, 10, 'AJ003', 'Suresh',    'Babu',       'suresh.babu@ajpvt.com',      72000.00,  '11223344553', 'ICICI Bank',             'ICIC0009003', 'PANAJ00003', 'ACTIVE',    '2023-11-01', null),
    (24, 3, 11, 'AJ004', 'Meenakshi', 'Krishnan',   'meenakshi.k@ajpvt.com',     65000.00,  '11223344554', 'Axis Bank',              'UTIB0009004', 'PANAJ00004', 'ACTIVE',    '2024-03-20', null),
    (25, 3, 12, 'AJ005', 'Deepak',    'Menon',      'deepak.menon@ajpvt.com',    105000.00, '11223344555', 'Kotak Mahindra Bank',    'KKBK0009005', 'PANAJ00005', 'ACTIVE',    '2023-09-05', null),
    (26, 3,  9, 'AJ006', 'Preethi',   'Sundaram',   'preethi.s@ajpvt.com',        78000.00,  '11223344556', 'HDFC Bank',              'HDFC0009006', 'PANAJ00006', 'ON_LEAVE',  '2024-01-30', null),
    (27, 3, 10, 'AJ007', 'Ganesh',    'Iyer',       'ganesh.iyer@ajpvt.com',      82000.00,  '11223344557', 'Canara Bank',            'CNRB0009007', 'PANAJ00007', 'ACTIVE',    '2023-07-12', null),
    (28, 3, 11, 'AJ008', 'Lavanya',   'Chandrasekar','lavanya.c@ajpvt.com',        60000.00, '11223344558', 'State Bank of India',    'SBIN0009008', 'PANAJ00008', 'ACTIVE',    '2024-04-01', null),
    (29, 3, 12, 'AJ009', 'Ravi',      'Shankar',    'ravi.shankar@ajpvt.com',    115000.00, '11223344559', 'HDFC Bank',              'HDFC0009009', 'PANAJ00009', 'ACTIVE',    '2022-12-15', null),
    (30, 3,  9, 'AJ010', 'Anusha',    'Pillai',     'anusha.pillai@ajpvt.com',    92000.00,  '11223344560', 'ICICI Bank',             'ICIC0009010', 'PANAJ00010', 'ACTIVE',    '2024-05-20', null)
ON CONFLICT DO NOTHING;

-- 6. Assign all standard salary components to AJ Pvt Ltd employees
-- Components 1 (Basic Pay), 2 (HRA), 3 (PF), 8 (PT), 9 (ESI), 10 (TDS)
DO $$
DECLARE
    emp_ids INT[] := ARRAY[21, 22, 23, 24, 25, 26, 27, 28, 29, 30];
    comp_ids INT[] := ARRAY[1, 2, 3, 8, 9, 10];
    emp_id INT;
    comp_id INT;
    base_id INT := 100; -- Start ESS IDs from 100 to avoid conflict with V4/V5
    hire_dates DATE[] := ARRAY[
        '2024-01-15'::DATE, '2024-02-10'::DATE, '2023-11-01'::DATE,
        '2024-03-20'::DATE, '2023-09-05'::DATE, '2024-01-30'::DATE,
        '2023-07-12'::DATE, '2024-04-01'::DATE, '2022-12-15'::DATE, '2024-05-20'::DATE
    ];
    i INT := 1;
    j INT;
BEGIN
    FOREACH emp_id IN ARRAY emp_ids LOOP
        j := 1;
        FOREACH comp_id IN ARRAY comp_ids LOOP
            INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active)
            VALUES (base_id, emp_id, comp_id, hire_dates[i], true)
            ON CONFLICT DO NOTHING;
            base_id := base_id + 1;
            j := j + 1;
        END LOOP;
        i := i + 1;
    END LOOP;
END $$;

-- 7. Update sequences
SELECT setval('company_id_seq',    (SELECT COALESCE(MAX(id), 3) FROM company));
SELECT setval('department_id_seq', (SELECT COALESCE(MAX(id), 12) FROM department));
SELECT setval('user_company_access_id_seq', (SELECT COALESCE(MAX(id), 4) FROM user_company_access));
SELECT setval('employee_id_seq',   (SELECT COALESCE(MAX(id), 30) FROM employee));
SELECT setval('employee_salary_structure_id_seq', (SELECT COALESCE(MAX(id), 160) FROM employee_salary_structure));
