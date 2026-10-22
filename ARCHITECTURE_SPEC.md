# Enterprise Multi-Tenant Payroll & Ledger Engine Specification

## 1. Tech Stack
- Backend: Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security 6, Spring Kafka, Spring Data Redis
- Database: PostgreSQL (strictly snake_case tables and columns)
- Messaging: Apache Kafka (KRaft mode) for async JasperReports PDF generation
- Object Storage: MinIO (S3-compatible) for generated payslips
- Engine: Spring Expression Language (SpEL) with BigDecimal for salary formula evaluation
- Frontend: React (Vite, TypeScript), Ant Design (antd), TanStack Query

## 2. Structural Hierarchy (Multi-Tenant Core)
Tenant (SaaS Client)
 └── Country (Jurisdiction, Currency, Tax rules)
      └── Company (Legal entity in that country - multiple per country)
           └── Department (Logical unit)
                └── Employee (Target of payroll calculations)

## 3. RBAC & Data Boundaries
- User (System Operators: Super Admin, Payroll Manager, Payroll Analyst) != Employee (Payroll Subject).
- Users have Roles (`SUPER_ADMIN`, `COUNTRY_PAYROLL_MGR`, `COMPANY_HR`).
- Data Scoping: A User is mapped to allowed `country_id`s and `company_id`s. Requests outside this scope throw `403 Forbidden`.
- Screen/Report Master: Fine-grained permissions mapping roles to specific screens and JasperReports.

## 4. Salary Component & Dynamic Formula Engine
- Code Standard: `EAR-{COUNTRY}-{CODE}` (Earnings) or `DED-{COUNTRY}-{CODE}` (Deductions).
- Calculation Types: `FLAT_AMOUNT`, `PERCENTAGE_OF_BASE`, `CUSTOM_FORMULA`.
- Evaluation: Formulas like `(#BASE_SALARY * 0.40) + #TRAVEL_ALLOWANCE` evaluated via SpEL into `BigDecimal` with `RoundingMode.HALF_UP`.

## 5. Payroll Execution Lifecycle (Manual Trigger)
1. User selects Company, Year, and Month via UI and clicks "Run Payroll".
2. System checks if a completed batch already exists for that Company/Month (idempotency check).
3. System fetches active employees, calculates all components, computes Net Pay, and writes an immutable snapshot to `payroll_record` and `payroll_record_detail`.
4. System publishes Kafka events (`payroll.payslip.generate`) for each record.
5. Kafka consumer generates the JasperReports PDF, uploads it to MinIO, and sets `payslip_pdf_url`.