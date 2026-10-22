-- ===========================================================
-- V5: Rich Real-World Seed Data
-- Adds comprehensive Indian enterprise payroll dataset:
-- - Additional departments
-- - Full Indian statutory & allowance salary components
-- - 20 realistic employees across departments & statuses
-- - Full employee salary structure assignments
-- - Admin user company scoping
-- ===========================================================

-- 1. Additional Departments for Company 1 (Acme India)
INSERT INTO department (id, company_id, name, code, is_active)
VALUES
    (5, 1, 'Operations', 'OPS', true),
    (6, 1, 'Product Management', 'PROD', true),
    (7, 1, 'Marketing', 'MKTG', true),
    (8, 1, 'Quality Assurance', 'QA', true)
ON CONFLICT (company_id, code) DO NOTHING;

-- 2. Ensure Super Admin (id=1) has explicit company access rows
INSERT INTO user_company_access (id, user_id, company_id)
VALUES 
    (2, 1, 1),
    (3, 1, 2)
ON CONFLICT DO NOTHING;

-- 3. Fix / Align Existing Salary Components in V4
UPDATE salary_component 
SET default_value = 40.00 
WHERE id = 1 AND code = 'EAR-IND-BP';

-- 4. Additional Real-World Indian Salary Components (Tenant 1, Country 1)
INSERT INTO salary_component (id, tenant_id, country_id, code, name, type, calculation_type, formula_expression, default_value, is_taxable, is_statutory, is_active)
VALUES
    (4, 1, 1, 'EAR-IND-SA', 'Special Allowance', 'EARNING', 'FLAT_AMOUNT', null, 5000.00, true, false, true),
    (5, 1, 1, 'EAR-IND-LTA', 'Leave Travel Allowance', 'EARNING', 'FLAT_AMOUNT', null, 3000.00, true, false, true),
    (6, 1, 1, 'EAR-IND-MED', 'Medical Allowance', 'EARNING', 'FLAT_AMOUNT', null, 1250.00, false, false, true),
    (7, 1, 1, 'EAR-IND-CONV', 'Conveyance Allowance', 'EARNING', 'FLAT_AMOUNT', null, 1600.00, false, false, true),
    (8, 1, 1, 'DED-IND-PT', 'Professional Tax', 'DEDUCTION', 'FLAT_AMOUNT', null, 200.00, false, true, true),
    (9, 1, 1, 'DED-IND-ESI', 'ESI Contribution', 'DEDUCTION', 'FLAT_AMOUNT', null, 750.00, false, true, true),
    (10, 1, 1, 'DED-IND-TDS', 'Income Tax (TDS)', 'DEDUCTION', 'CUSTOM_FORMULA', '#BASE_SALARY * 0.05', 0.00, false, true, true)
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 5. Additional Realistic Employees (IDs 6 to 20 for Company 1)
INSERT INTO employee (id, company_id, department_id, employee_code, first_name, last_name, email, base_salary, bank_account_number, bank_name, bank_ifsc_code, tax_id, employment_status, hire_date, termination_date)
VALUES
    (6, 1, 6, 'EMP006', 'Neha', 'Verma', 'neha.verma@acme.com', 140000.00, '98765432101', 'HDFC Bank', 'HDFC0001234', 'PAN6789012', 'ACTIVE', '2022-04-10', null),
    (7, 1, 1, 'EMP007', 'Rajesh', 'Kumar', 'rajesh.kumar@acme.com', 130000.00, '98765432102', 'State Bank of India', 'SBIN0004567', 'PAN7890123', 'ACTIVE', '2021-08-15', null),
    (8, 1, 4, 'EMP008', 'Ananya', 'Deshmukh', 'ananya.deshmukh@acme.com', 92000.00, '98765432103', 'ICICI Bank', 'ICIC0007890', 'PAN8901234', 'ACTIVE', '2023-05-12', null),
    (9, 1, 2, 'EMP009', 'Rohan', 'Kulkarni', 'rohan.kulkarni@acme.com', 68000.00, '98765432104', 'Axis Bank', 'UTIB0001122', 'PAN9012345', 'ACTIVE', '2024-02-01', null),
    (10, 1, 3, 'EMP010', 'Kavita', 'Nair', 'kavita.nair@acme.com', 78000.00, '98765432105', 'Kotak Mahindra Bank', 'KKBK0003344', 'PAN0123456', 'ACTIVE', '2022-11-20', null),
    (11, 1, 1, 'EMP011', 'Aditya', 'Joshi', 'aditya.joshi@acme.com', 105000.00, '98765432106', 'HDFC Bank', 'HDFC0001234', 'PAN1122334', 'ACTIVE', '2023-07-01', null),
    (12, 1, 5, 'EMP012', 'Pooja', 'Sundaram', 'pooja.sundaram@acme.com', 62000.00, '98765432107', 'State Bank of India', 'SBIN0004567', 'PAN2233445', 'ACTIVE', '2023-09-15', null),
    (13, 1, 7, 'EMP013', 'Manish', 'Tiwari', 'manish.tiwari@acme.com', 88000.00, '98765432108', 'ICICI Bank', 'ICIC0007890', 'PAN3344556', 'ACTIVE', '2022-03-25', null),
    (14, 1, 8, 'EMP014', 'Swati', 'Mukherjee', 'swati.mukherjee@acme.com', 72000.00, '98765432109', 'Axis Bank', 'UTIB0001122', 'PAN4455667', 'ACTIVE', '2024-01-15', null),
    (15, 1, 1, 'EMP015', 'Karthik', 'Subramanian', 'karthik.subramanian@acme.com', 155000.00, '98765432110', 'HDFC Bank', 'HDFC0001234', 'PAN5566778', 'ACTIVE', '2021-01-20', null),
    (16, 1, 6, 'EMP016', 'Divya', 'Reddy', 'divya.reddy@acme.com', 125000.00, '98765432111', 'Kotak Mahindra Bank', 'KKBK0003344', 'PAN6677889', 'ACTIVE', '2023-04-18', null),
    (17, 1, 4, 'EMP017', 'Sandeep', 'Bansal', 'sandeep.bansal@acme.com', 98000.00, '98765432112', 'State Bank of India', 'SBIN0004567', 'PAN7788990', 'ACTIVE', '2022-09-10', null),
    (18, 1, 8, 'EMP018', 'Meera', 'Iyer', 'meera.iyer@acme.com', 65000.00, '98765432113', 'HDFC Bank', 'HDFC0001234', 'PAN8899001', 'PROBATION', '2026-06-01', null),
    (19, 1, 5, 'EMP019', 'Harish', 'Menon', 'harish.menon@acme.com', 58000.00, '98765432114', 'ICICI Bank', 'ICIC0007890', 'PAN9900112', 'ACTIVE', '2023-12-01', null),
    (20, 1, 2, 'EMP020', 'Ritu', 'Malhotra', 'ritu.malhotra@acme.com', 82000.00, '98765432115', 'Axis Bank', 'UTIB0001122', 'PAN1011121', 'RESIGNED', '2022-05-15', '2026-08-31')
ON CONFLICT (email) DO NOTHING;

-- 6. Assign Components to All Employees (1 through 20)
-- Standard package for active employees:
-- Earnings: Basic Pay (1), HRA (2), Special Allowance (4), Conveyance (7)
-- Deductions: PF (3), Professional Tax (8), TDS (10)

DO $$
DECLARE
    emp RECORD;
BEGIN
    FOR emp IN SELECT id, hire_date FROM employee WHERE id BETWEEN 1 AND 20 LOOP
        -- Component 1: Basic Pay
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 1, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;

        -- Component 2: HRA
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 2, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;

        -- Component 3: PF
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 3, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;

        -- Component 4: Special Allowance
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 4, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;

        -- Component 7: Conveyance Allowance
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 7, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;

        -- Component 8: Professional Tax
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 8, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;

        -- Component 10: TDS
        INSERT INTO employee_salary_structure (employee_id, component_id, effective_from, is_active)
        VALUES (emp.id, 10, emp.hire_date, true)
        ON CONFLICT (employee_id, component_id, effective_from) DO NOTHING;
    END LOOP;
END $$;

-- 7. Reset sequences to prevent duplicate key errors on future inserts
SELECT setval('department_id_seq', (SELECT COALESCE(MAX(id), 1) FROM department));
SELECT setval('user_company_access_id_seq', (SELECT COALESCE(MAX(id), 1) FROM user_company_access));
SELECT setval('salary_component_id_seq', (SELECT COALESCE(MAX(id), 1) FROM salary_component));
SELECT setval('employee_id_seq', (SELECT COALESCE(MAX(id), 1) FROM employee));
SELECT setval('employee_salary_structure_id_seq', (SELECT COALESCE(MAX(id), 1) FROM employee_salary_structure));
