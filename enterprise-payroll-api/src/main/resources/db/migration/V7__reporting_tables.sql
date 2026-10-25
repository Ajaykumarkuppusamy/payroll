-- ===========================================================
-- V7: Reporting Staging Engine & Stored Procedures
-- ===========================================================

-- 1. Report Staging Data Table
CREATE TABLE IF NOT EXISTS report_staging_data (
    id                  BIGSERIAL           PRIMARY KEY,
    job_id              UUID                NOT NULL,
    report_type         VARCHAR(50)         NOT NULL,
    section             VARCHAR(50)         NOT NULL DEFAULT 'BODY',
    col1                VARCHAR(255),       -- e.g. Component Name / Employee Code / Dept Name
    col2                VARCHAR(255),       -- e.g. Component Type / Employee Name / Dept Code
    col3                VARCHAR(255),       -- e.g. Department Name / Designation / Status
    col4                VARCHAR(255),       -- e.g. Pay Period / PAN / Bank Account
    col5                VARCHAR(255),       -- e.g. Company Name / Tenant Name
    num1                NUMERIC(19, 2),     -- e.g. Amount / Gross / Base
    num2                NUMERIC(19, 2),     -- e.g. Total Deductions / Statutory
    num3                NUMERIC(19, 2),     -- e.g. Net Salary / Total Cost
    created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rsd_job_id ON report_staging_data(job_id);
CREATE INDEX IF NOT EXISTS idx_rsd_report_type ON report_staging_data(report_type);
CREATE INDEX IF NOT EXISTS idx_rsd_created_at ON report_staging_data(created_at);

-- 2. Stored Procedure: Populate Payslip Staging
CREATE OR REPLACE PROCEDURE sp_populate_payslip_staging(
    p_job_id UUID,
    p_batch_id BIGINT,
    p_employee_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_record_id BIGINT;
    v_gross NUMERIC(19, 2);
    v_deductions NUMERIC(19, 2);
    v_net NUMERIC(19, 2);
    v_emp_code VARCHAR(50);
    v_emp_name VARCHAR(255);
    v_dept_name VARCHAR(255);
    v_company_name VARCHAR(255);
    v_tenant_name VARCHAR(255);
    v_pay_period VARCHAR(50);
BEGIN
    -- Locate payroll record
    SELECT pr.id, pr.gross_earnings, pr.total_deductions, pr.net_salary,
           e.employee_code, (e.first_name || ' ' || e.last_name),
           COALESCE(d.name, 'N/A'), c.name, t.name,
           (LPAD(pb.month::text, 2, '0') || '/' || pb.year::text)
    INTO v_record_id, v_gross, v_deductions, v_net,
         v_emp_code, v_emp_name,
         v_dept_name, v_company_name, v_tenant_name,
         v_pay_period
    FROM payroll_record pr
    JOIN payroll_batch pb ON pr.batch_id = pb.id
    JOIN employee e ON pr.employee_id = e.id
    LEFT JOIN department d ON e.department_id = d.id
    JOIN company c ON pb.company_id = c.id
    JOIN tenant t ON pb.tenant_id = t.id
    WHERE pr.batch_id = p_batch_id AND pr.employee_id = p_employee_id;

    IF v_record_id IS NULL THEN
        RAISE EXCEPTION 'Payroll record not found for batch % and employee %', p_batch_id, p_employee_id;
    END IF;

    -- Insert Summary / Header Row
    INSERT INTO report_staging_data(job_id, report_type, section, col1, col2, col3, col4, col5, num1, num2, num3)
    VALUES (
        p_job_id, 'PAYSLIP', 'HEADER',
        v_emp_code, v_emp_name, v_dept_name, v_pay_period, (v_tenant_name || ' - ' || v_company_name),
        v_gross, v_deductions, v_net
    );

    -- Insert Component Line Items
    INSERT INTO report_staging_data(job_id, report_type, section, col1, col2, col3, col4, col5, num1, num2, num3)
    SELECT
        p_job_id,
        'PAYSLIP',
        prd.component_type,               -- 'EARNING' or 'DEDUCTION'
        prd.component_name,
        prd.component_code,
        prd.component_type,
        v_pay_period,
        v_emp_code,
        prd.amount,
        0.00,
        0.00
    FROM payroll_record_detail prd
    WHERE prd.record_id = v_record_id
    ORDER BY prd.component_type DESC, prd.amount DESC;

END;
$$;

-- 3. Stored Procedure: Populate Payroll Master Summary Staging
CREATE OR REPLACE PROCEDURE sp_populate_payroll_summary_staging(
    p_job_id UUID,
    p_batch_id BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Insert Header
    INSERT INTO report_staging_data(job_id, report_type, section, col1, col2, col3, col4, col5, num1, num2, num3)
    SELECT
        p_job_id, 'PAYROLL_SUMMARY', 'HEADER',
        c.name,
        t.name,
        pb.status,
        (LPAD(pb.month::text, 2, '0') || '/' || pb.year::text),
        u.full_name,
        pb.total_gross_pay,
        pb.total_deductions,
        pb.total_net_pay
    FROM payroll_batch pb
    JOIN company c ON pb.company_id = c.id
    JOIN tenant t ON pb.tenant_id = t.id
    JOIN app_user u ON pb.executed_by = u.id
    WHERE pb.id = p_batch_id;

    -- Insert Employee Summary Lines
    INSERT INTO report_staging_data(job_id, report_type, section, col1, col2, col3, col4, col5, num1, num2, num3)
    SELECT
        p_job_id,
        'PAYROLL_SUMMARY',
        'ROW',
        e.employee_code,
        (e.first_name || ' ' || e.last_name),
        COALESCE(d.name, 'General'),
        COALESCE(e.tax_id, 'N/A'),
        COALESCE(e.bank_account_number, 'N/A'),
        pr.gross_earnings,
        pr.total_deductions,
        pr.net_salary
    FROM payroll_record pr
    JOIN employee e ON pr.employee_id = e.id
    LEFT JOIN department d ON e.department_id = d.id
    WHERE pr.batch_id = p_batch_id
    ORDER BY d.name NULLS LAST, e.employee_code ASC;

END;
$$;

-- 4. Stored Procedure: Populate Department Headcount & Cost Staging
CREATE OR REPLACE PROCEDURE sp_populate_headcount_cost_staging(
    p_job_id UUID,
    p_company_id BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO report_staging_data(job_id, report_type, section, col1, col2, col3, col4, col5, num1, num2, num3)
    SELECT
        p_job_id,
        'HEADCOUNT_COST',
        'ROW',
        d.name,
        d.code,
        c.name,
        COUNT(e.id)::text,
        d.is_active::text,
        COALESCE(SUM(e.base_salary), 0.00),
        COALESCE(AVG(e.base_salary), 0.00),
        0.00
    FROM department d
    JOIN company c ON d.company_id = c.id
    LEFT JOIN employee e ON e.department_id = d.id AND e.employment_status = 'ACTIVE'
    WHERE d.company_id = p_company_id
    GROUP BY d.id, d.name, d.code, c.name, d.is_active
    ORDER BY SUM(e.base_salary) DESC NULLS LAST;
END;
$$;

-- 5. Maintenance: Cleanup old staging entries
CREATE OR REPLACE PROCEDURE sp_cleanup_old_report_staging(p_hours INT)
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM report_staging_data
    WHERE created_at < (CURRENT_TIMESTAMP - (p_hours || ' hours')::INTERVAL);
END;
$$;
