-- ===========================================================
-- V2: Salary Components & Employee Salary Structure
-- ===========================================================

-- 1. Salary Component (reusable template per tenant/country)
CREATE TABLE salary_component (
    id                      BIGSERIAL           PRIMARY KEY,
    tenant_id               BIGINT              NOT NULL,
    country_id              BIGINT              NOT NULL,
    code                    VARCHAR(50)         NOT NULL,
    name                    VARCHAR(255)        NOT NULL,
    type                    VARCHAR(20)         NOT NULL,
    calculation_type        VARCHAR(30)         NOT NULL,
    formula_expression      TEXT,
    default_value           NUMERIC(19, 2)      NOT NULL DEFAULT 0.00,
    is_taxable              BOOLEAN             NOT NULL DEFAULT TRUE,
    is_statutory            BOOLEAN             NOT NULL DEFAULT FALSE,
    is_active               BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sc_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sc_country
        FOREIGN KEY (country_id) REFERENCES country(id) ON DELETE RESTRICT,
    CONSTRAINT uq_sc_tenant_code
        UNIQUE (tenant_id, code)
);
CREATE INDEX idx_sc_tenant_id  ON salary_component(tenant_id);
CREATE INDEX idx_sc_country_id ON salary_component(country_id);
CREATE INDEX idx_sc_type       ON salary_component(type);

-- 2. Employee Salary Structure (per-employee component assignment)
CREATE TABLE employee_salary_structure (
    id                  BIGSERIAL           PRIMARY KEY,
    employee_id         BIGINT              NOT NULL,
    component_id        BIGINT              NOT NULL,
    override_amount     NUMERIC(19, 2),
    effective_from      DATE                NOT NULL,
    effective_to        DATE,
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ess_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ess_component
        FOREIGN KEY (component_id) REFERENCES salary_component(id) ON DELETE RESTRICT,
    CONSTRAINT uq_ess_emp_comp_from
        UNIQUE (employee_id, component_id, effective_from)
);
CREATE INDEX idx_ess_employee_id  ON employee_salary_structure(employee_id);
CREATE INDEX idx_ess_component_id ON employee_salary_structure(component_id);
