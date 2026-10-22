-- ===========================================================
-- Enterprise Payroll Platform — V1 Initial Schema
-- Database  : PostgreSQL (strict snake_case naming)
-- Convention: All FKs use ON DELETE RESTRICT (audit protection)
-- ===========================================================

-- 1. Tenant (SaaS Client — Top of Hierarchy)
CREATE TABLE tenant (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    slug            VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tenant_slug UNIQUE (slug)
);

-- 2. Country (Jurisdiction under Tenant)
CREATE TABLE country (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    iso_code        CHAR(2)         NOT NULL,
    currency_code   VARCHAR(3)      NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_country_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT uq_country_tenant_iso
        UNIQUE (tenant_id, iso_code)
);
CREATE INDEX idx_country_tenant_id ON country(tenant_id);

-- 3. Company (Legal Entity under Country — multiple per country)
CREATE TABLE company (
    id                      BIGSERIAL       PRIMARY KEY,
    country_id              BIGINT          NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    registration_number     VARCHAR(100),
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_company_country
        FOREIGN KEY (country_id) REFERENCES country(id) ON DELETE RESTRICT,
    CONSTRAINT uq_company_country_reg
        UNIQUE (country_id, registration_number)
);
CREATE INDEX idx_company_country_id ON company(country_id);

-- 4. Department (Logical Unit under Company)
CREATE TABLE department (
    id              BIGSERIAL       PRIMARY KEY,
    company_id      BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    code            VARCHAR(50)     NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_department_company
        FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE RESTRICT,
    CONSTRAINT uq_department_company_code
        UNIQUE (company_id, code)
);
CREATE INDEX idx_department_company_id ON department(company_id);

-- 5. Employee (Payroll Subject)
CREATE TABLE employee (
    id                      BIGSERIAL           PRIMARY KEY,
    company_id              BIGINT              NOT NULL,
    department_id           BIGINT              NOT NULL,
    employee_code           VARCHAR(50)         NOT NULL,
    first_name              VARCHAR(100)        NOT NULL,
    last_name               VARCHAR(100)        NOT NULL,
    email                   VARCHAR(255)        NOT NULL,
    base_salary             NUMERIC(19, 2)      NOT NULL,
    bank_account_number     VARCHAR(50),
    bank_name               VARCHAR(255),
    bank_ifsc_code          VARCHAR(20),
    tax_id                  VARCHAR(50),
    employment_status       VARCHAR(20)         NOT NULL DEFAULT 'ACTIVE',
    hire_date               DATE                NOT NULL,
    termination_date        DATE,
    created_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_employee_company
        FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE RESTRICT,
    CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE RESTRICT,
    CONSTRAINT uq_employee_company_code
        UNIQUE (company_id, employee_code),
    CONSTRAINT uq_employee_email
        UNIQUE (email)
);
CREATE INDEX idx_employee_company_id    ON employee(company_id);
CREATE INDEX idx_employee_department_id ON employee(department_id);
CREATE INDEX idx_employee_status        ON employee(employment_status);

-- 6. App User (System Operator — NOT an Employee)
CREATE TABLE app_user (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL,
    username        VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(255)    NOT NULL,
    role            VARCHAR(30)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_app_user_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT uq_app_user_username
        UNIQUE (username),
    CONSTRAINT uq_app_user_email
        UNIQUE (email)
);
CREATE INDEX idx_app_user_tenant_id ON app_user(tenant_id);

-- 7. User ↔ Country Access (RBAC Data Scoping)
CREATE TABLE user_country_access (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    country_id      BIGINT          NOT NULL,

    CONSTRAINT fk_uca_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_uca_country
        FOREIGN KEY (country_id) REFERENCES country(id) ON DELETE RESTRICT,
    CONSTRAINT uq_uca_user_country
        UNIQUE (user_id, country_id)
);
CREATE INDEX idx_uca_user_id    ON user_country_access(user_id);
CREATE INDEX idx_uca_country_id ON user_country_access(country_id);

-- 8. User ↔ Company Access (RBAC Data Scoping)
CREATE TABLE user_company_access (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    company_id      BIGINT          NOT NULL,

    CONSTRAINT fk_ucoa_user
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ucoa_company
        FOREIGN KEY (company_id) REFERENCES company(id) ON DELETE RESTRICT,
    CONSTRAINT uq_ucoa_user_company
        UNIQUE (user_id, company_id)
);
CREATE INDEX idx_ucoa_user_id    ON user_company_access(user_id);
CREATE INDEX idx_ucoa_company_id ON user_company_access(company_id);
