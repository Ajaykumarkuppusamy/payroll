-- ===========================================================
-- V3: Payroll Execution Ledger (Immutable Snapshots)
-- ===========================================================

-- 1. Payroll Batch (one per company per pay period)
CREATE TABLE payroll_batch (
    id                  BIGSERIAL           PRIMARY KEY,
    tenant_id           BIGINT              NOT NULL,
    company_id          BIGINT              NOT NULL,
    year                INT                 NOT NULL,
    month               INT                 NOT NULL CHECK (month >= 1 AND month <= 12),
    status              VARCHAR(20)         NOT NULL DEFAULT 'DRAFT',
    executed_by         BIGINT              NOT NULL,
    total_employees     INT                 NOT NULL DEFAULT 0,
    total_gross_pay     NUMERIC(19, 2)      NOT NULL DEFAULT 0.00,
    total_deductions    NUMERIC(19, 2)      NOT NULL DEFAULT 0.00,
    total_net_pay       NUMERIC(19, 2)      NOT NULL DEFAULT 0.00,
    error_message       TEXT,
    executed_at         TIMESTAMP,
    created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pb_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pb_company
        FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pb_executed_by
        FOREIGN KEY (executed_by) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT uq_pb_company_year_month
        UNIQUE (company_id, year, month)
);
CREATE INDEX idx_pb_tenant_id  ON payroll_batch(tenant_id);
CREATE INDEX idx_pb_company_id ON payroll_batch(company_id);
CREATE INDEX idx_pb_status     ON payroll_batch(status);

-- 2. Payroll Record (immutable snapshot per employee)
CREATE TABLE payroll_record (
    id                  BIGSERIAL           PRIMARY KEY,
    batch_id            BIGINT              NOT NULL,
    employee_id         BIGINT              NOT NULL,
    base_salary         NUMERIC(19, 2)      NOT NULL,
    gross_earnings      NUMERIC(19, 2)      NOT NULL,
    total_deductions    NUMERIC(19, 2)      NOT NULL,
    net_salary          NUMERIC(19, 2)      NOT NULL,
    payslip_pdf_url     VARCHAR(500),
    pdf_status          VARCHAR(20)         NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pr_batch
        FOREIGN KEY (batch_id) REFERENCES payroll_batch(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pr_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE RESTRICT,
    CONSTRAINT uq_pr_batch_employee
        UNIQUE (batch_id, employee_id)
);
CREATE INDEX idx_pr_batch_id    ON payroll_record(batch_id);
CREATE INDEX idx_pr_employee_id ON payroll_record(employee_id);
CREATE INDEX idx_pr_pdf_status  ON payroll_record(pdf_status);

-- 3. Payroll Record Detail (immutable line-item component snapshot)
CREATE TABLE payroll_record_detail (
    id                  BIGSERIAL           PRIMARY KEY,
    record_id           BIGINT              NOT NULL,
    component_code      VARCHAR(50)         NOT NULL,
    component_name      VARCHAR(255)        NOT NULL,
    component_type      VARCHAR(20)         NOT NULL,
    amount              NUMERIC(19, 2)      NOT NULL,

    CONSTRAINT fk_prd_record
        FOREIGN KEY (record_id) REFERENCES payroll_record(id) ON DELETE CASCADE
);
CREATE INDEX idx_prd_record_id ON payroll_record_detail(record_id);
