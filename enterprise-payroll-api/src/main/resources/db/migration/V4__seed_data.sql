-- V4__seed_data.sql

-- 1. Tenant
INSERT INTO tenant (id, name, slug, is_active)
VALUES (1, 'Acme Global', 'acme-global', true)
ON CONFLICT DO NOTHING;

-- 2. Countries
INSERT INTO country (id, tenant_id, name, iso_code, currency_code)
VALUES 
    (1, 1, 'India', 'IN', 'INR'),
    (2, 1, 'United States', 'US', 'USD')
ON CONFLICT DO NOTHING;

-- 3. Companies
INSERT INTO company (id, country_id, name, registration_number, is_active)
VALUES 
    (1, 1, 'Acme India Pvt Ltd', 'REG-IN-12345', true),
    (2, 2, 'Acme Corp USA', 'REG-US-12345', true)
ON CONFLICT DO NOTHING;

-- 4. Departments
INSERT INTO department (id, company_id, name, code, is_active)
VALUES 
    (1, 1, 'Engineering', 'ENG', true),
    (2, 1, 'Sales', 'SALES', true),
    (3, 1, 'Human Resources', 'HR', true),
    (4, 1, 'Finance', 'FIN', true)
ON CONFLICT DO NOTHING;

-- 5. System Users (AppUser)
-- password123 -> $2a$10$7leuY7VKAzWTBSaNlCIXo.fcS0a6nuKId09ywFDzee4VpLVAM7rvm
INSERT INTO app_user (id, tenant_id, username, email, password_hash, full_name, role, status)
VALUES 
    (1, 1, 'admin', 'admin@acme.com', '$2a$10$7leuY7VKAzWTBSaNlCIXo.fcS0a6nuKId09ywFDzee4VpLVAM7rvm', 'Super Admin', 'SUPER_ADMIN', 'ACTIVE'),
    (2, 1, 'hr_india', 'hr_india@acme.com', '$2a$10$7leuY7VKAzWTBSaNlCIXo.fcS0a6nuKId09ywFDzee4VpLVAM7rvm', 'HR Manager India', 'COMPANY_HR', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- HR Scoped to India Company
INSERT INTO user_company_access (id, user_id, company_id)
VALUES (1, 2, 1)
ON CONFLICT DO NOTHING;

-- 6. Employees
INSERT INTO employee (id, company_id, department_id, employee_code, first_name, last_name, email, base_salary, tax_id, employment_status, hire_date)
VALUES 
    (1, 1, 1, 'EMP001', 'Rahul', 'Sharma', 'rahul.sharma@acme.com', 120000.00, 'PAN1234567', 'ACTIVE', '2023-01-10'),
    (2, 1, 2, 'EMP002', 'Priya', 'Patel', 'priya.patel@acme.com', 85000.00, 'PAN2345678', 'ACTIVE', '2023-03-01'),
    (3, 1, 3, 'EMP003', 'Amit', 'Singh', 'amit.singh@acme.com', 75000.00, 'PAN3456789', 'ACTIVE', '2022-06-15'),
    (4, 1, 4, 'EMP004', 'Sneha', 'Gupta', 'sneha.gupta@acme.com', 50000.00, 'PAN4567890', 'ACTIVE', '2024-01-05'),
    (5, 1, 1, 'EMP005', 'Vikram', 'Rao', 'vikram.rao@acme.com', 110000.00, 'PAN5678901', 'ON_LEAVE', '2023-11-20')
ON CONFLICT DO NOTHING;

-- 7. Salary Components
INSERT INTO salary_component (id, tenant_id, country_id, code, name, type, calculation_type, formula_expression, default_value, is_taxable, is_statutory, is_active)
VALUES 
    (1, 1, 1, 'EAR-IND-BP', 'Basic Pay', 'EARNING', 'PERCENTAGE_OF_BASE', '#BASE_SALARY * 0.40', 0.00, true, false, true),
    (2, 1, 1, 'EAR-IND-HRA', 'House Rent Allowance', 'EARNING', 'CUSTOM_FORMULA', '(#BASE_SALARY * 0.40) * 0.50', 0.00, true, false, true),
    (3, 1, 1, 'DED-IND-PF', 'Provident Fund', 'DEDUCTION', 'CUSTOM_FORMULA', '(#BASE_SALARY * 0.40) * 0.12', 0.00, false, true, true)
ON CONFLICT DO NOTHING;

-- 8. Employee Salary Structures (Assigning components to employees)
-- Employee 1 (EMP001)
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (1, 1, 1, '2023-01-10', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (2, 1, 2, '2023-01-10', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (3, 1, 3, '2023-01-10', true) ON CONFLICT DO NOTHING;

-- Employee 2 (EMP002)
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (4, 2, 1, '2023-03-01', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (5, 2, 2, '2023-03-01', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (6, 2, 3, '2023-03-01', true) ON CONFLICT DO NOTHING;

-- Employee 3 (EMP003)
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (7, 3, 1, '2022-06-15', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (8, 3, 2, '2022-06-15', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (9, 3, 3, '2022-06-15', true) ON CONFLICT DO NOTHING;

-- Employee 4 (EMP004)
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (10, 4, 1, '2024-01-05', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (11, 4, 2, '2024-01-05', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (12, 4, 3, '2024-01-05', true) ON CONFLICT DO NOTHING;

-- Employee 5 (EMP005)
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (13, 5, 1, '2023-11-20', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (14, 5, 2, '2023-11-20', true) ON CONFLICT DO NOTHING;
INSERT INTO employee_salary_structure (id, employee_id, component_id, effective_from, is_active) VALUES (15, 5, 3, '2023-11-20', true) ON CONFLICT DO NOTHING;

-- Update sequences for future inserts
SELECT setval('tenant_id_seq', (SELECT COALESCE(MAX(id), 1) FROM tenant));
SELECT setval('country_id_seq', (SELECT COALESCE(MAX(id), 1) FROM country));
SELECT setval('company_id_seq', (SELECT COALESCE(MAX(id), 1) FROM company));
SELECT setval('department_id_seq', (SELECT COALESCE(MAX(id), 1) FROM department));
SELECT setval('app_user_id_seq', (SELECT COALESCE(MAX(id), 1) FROM app_user));
SELECT setval('user_company_access_id_seq', (SELECT COALESCE(MAX(id), 1) FROM user_company_access));
SELECT setval('employee_id_seq', (SELECT COALESCE(MAX(id), 1) FROM employee));
SELECT setval('salary_component_id_seq', (SELECT COALESCE(MAX(id), 1) FROM salary_component));
SELECT setval('employee_salary_structure_id_seq', (SELECT COALESCE(MAX(id), 1) FROM employee_salary_structure));
