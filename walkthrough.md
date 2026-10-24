# Phase 1 — Organizational Hierarchy & Identity Modeling

## Overview

Established the foundational domain model for the multi-tenant payroll platform, covering the entire structural hierarchy (`Tenant → Country → Company → Department → Employee`) and the identity/RBAC layer (`AppUser`, access-scoping tables).

---

## Package Structure

```
com.payroll.aj
├── common
│   ├── entity
│   │   └── BaseEntity.java              ← MappedSuperclass (id, audit timestamps)
│   └── enums
│       ├── CurrencyCode.java            ← ISO 4217 with display metadata
│       ├── EmploymentStatus.java        ← ACTIVE, ON_LEAVE, PROBATION, etc.
│       ├── UserRole.java                ← SUPER_ADMIN, COUNTRY_PAYROLL_MGR, COMPANY_HR
│       └── UserStatus.java             ← ACTIVE, INACTIVE, LOCKED, PENDING_ACTIVATION
├── tenant
│   └── entity
│       └── Tenant.java                  ← SaaS client (hierarchy root)
├── organization
│   └── entity
│       ├── Country.java                 ← Jurisdiction with currency
│       ├── Company.java                 ← Legal entity (multiple per country)
│       └── Department.java             ← Logical unit within company
├── employee
│   └── entity
│       └── Employee.java                ← Payroll subject (BigDecimal base_salary)
└── identity
    └── entity
        ├── AppUser.java                 ← System operator (≠ Employee)
        ├── UserCountryAccess.java       ← RBAC country-scope mapping
        └── UserCompanyAccess.java       ← RBAC company-scope mapping
```

---

## Files Created

### DDL Migration
| File | Purpose |
|------|---------|
| [`V1__init_schema.sql`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/resources/db/migration/V1__init_schema.sql) | 8 tables, all FKs `ON DELETE RESTRICT`, explicit indexes on all FK columns |

### Common Layer
| File | Purpose |
|------|---------|
| [`BaseEntity.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/entity/BaseEntity.java) | `@MappedSuperclass` — `id`, `createdAt`, `updatedAt` via JPA callbacks |
| [`CurrencyCode.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/CurrencyCode.java) | 15 ISO 4217 codes with symbol + `decimalPlaces` for BigDecimal scale |
| [`EmploymentStatus.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/EmploymentStatus.java) | 5 lifecycle states; only `ACTIVE` is payroll-eligible |
| [`UserRole.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/UserRole.java) | 3 RBAC roles per spec §3 |
| [`UserStatus.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/UserStatus.java) | 4 account states; only `ACTIVE` can authenticate |

### Domain Entities
| File | Table | Key Design Decisions |
|------|-------|---------------------|
| [`Tenant.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/tenant/entity/Tenant.java) | `tenant` | URL-safe `slug` for tenant resolution |
| [`Country.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/entity/Country.java) | `country` | Composite unique `(tenant_id, iso_code)` |
| [`Company.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/entity/Company.java) | `company` | Multiple per country; unique `(country_id, registration_number)` |
| [`Department.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/entity/Department.java) | `department` | Unique `(company_id, code)` |
| [`Employee.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/employee/entity/Employee.java) | `employee` | `BigDecimal(19,2)` baseSalary; bank details + tax ID |
| [`AppUser.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/entity/AppUser.java) | `app_user` | Operator ≠ Employee; BCrypt `passwordHash` |
| [`UserCountryAccess.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/entity/UserCountryAccess.java) | `user_country_access` | RBAC country-scope join entity |
| [`UserCompanyAccess.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/entity/UserCompanyAccess.java) | `user_company_access` | RBAC company-scope join entity |

### POM Changes
| File | Change |
|------|--------|
| [`pom.xml`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/pom.xml) | Added `flyway-core` + `flyway-database-postgresql` |

---

## Design Decisions

1. **No bidirectional relationships** — All `@ManyToOne` are unidirectional with `FetchType.LAZY` to prevent N+1 queries and Jackson serialization loops.
2. **`@MappedSuperclass` over `@EntityListeners`** — JPA lifecycle callbacks keep the entity layer framework-agnostic; no Spring Data auditing dependency required.
3. **`IDENTITY` generation strategy** — Maps directly to PostgreSQL `BIGSERIAL`; avoids sequence pre-allocation overhead for this workload.
4. **`EnumType.STRING` everywhere** — Ordinal-based storage would break on reordering; string storage is migration-safe and human-readable in SQL queries.
5. **Access-scoping as join entities** — `UserCountryAccess` and `UserCompanyAccess` don't extend `BaseEntity` since they are pure RBAC mappings without audit lifecycle needs.
6. **`ON DELETE RESTRICT` on all FKs** — Protects referential integrity and audit trails as mandated by the spec.

---

## Verification

- ✅ `mvnw compile` — **BUILD SUCCESS** (exit code 0, zero errors)

---

# Phase 2 — Authentication, Security & Granular Data Scoping

## Overview

Implemented the complete authentication and authorization pipeline: stateless JWT with embedded RBAC scope, Spring Security 6 filter chain (lambda DSL only, zero deprecated Security 5 methods), custom data-scoping annotations with AOP enforcement, and a login endpoint.

---

## Package Structure (New)

```
com.payroll.aj
├── security
│   ├── config
│   │   └── SecurityConfig.java              ← SecurityFilterChain, CORS, BCrypt
│   ├── dto
│   │   ├── LoginRequest.java                ← Java 21 record with validation
│   │   └── AuthResponse.java               ← JWT + RBAC scope response
│   ├── jwt
│   │   ├── JwtService.java                  ← JJWT 0.12.x token gen/validation
│   │   └── JwtAuthenticationFilter.java     ← Stateless OncePerRequestFilter
│   ├── scope
│   │   ├── RequireCountryAccess.java        ← Declarative RBAC annotation
│   │   ├── RequireCompanyAccess.java        ← Declarative RBAC annotation
│   │   └── DataScopeAspect.java             ← AOP enforcer (SUPER_ADMIN bypasses)
│   └── userdetails
│       ├── UserPrincipal.java               ← Custom UserDetails with scope sets
│       └── AppUserDetailsService.java       ← Loads user + access mappings
├── identity
│   ├── controller
│   │   └── AuthController.java              ← POST /api/v1/auth/login
│   ├── service
│   │   └── AuthService.java                 ← Login orchestration
│   └── repository
│       ├── AppUserRepository.java           ← findByUsernameOrEmail
│       ├── UserCountryAccessRepository.java ← RBAC country-scope projection
│       └── UserCompanyAccessRepository.java ← RBAC company-scope projection
```

## Files Created (Phase 2)

### DTOs (Java 21 Records)
| File | Purpose |
|------|---------|
| [`LoginRequest.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/dto/LoginRequest.java) | Login payload (login + password) with Bean Validation |
| [`AuthResponse.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/dto/AuthResponse.java) | JWT + scope metadata, snake_case JSON via @JsonProperty |

### JWT & Security Core
| File | Purpose |
|------|---------|
| [`JwtService.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/jwt/JwtService.java) | JJWT 0.12.x — generates/validates tokens with 6 custom claims |
| [`JwtAuthenticationFilter.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/jwt/JwtAuthenticationFilter.java) | Stateless filter reconstructing UserPrincipal from JWT claims |
| [`UserPrincipal.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/userdetails/UserPrincipal.java) | Custom UserDetails with tenantId, countryIds, companyIds |
| [`AppUserDetailsService.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/userdetails/AppUserDetailsService.java) | Loads AppUser + RBAC scope in single read-only transaction |
| [`SecurityConfig.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/config/SecurityConfig.java) | Spring Security 6 lambda DSL, stateless sessions, CORS |

### Data Scoping (AOP)
| File | Purpose |
|------|---------|
| [`RequireCountryAccess.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/scope/RequireCountryAccess.java) | Method-level country-scope annotation |
| [`RequireCompanyAccess.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/scope/RequireCompanyAccess.java) | Method-level company-scope annotation |
| [`DataScopeAspect.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/scope/DataScopeAspect.java) | AOP @Before advice — SUPER_ADMIN bypasses |

### Auth Endpoint
| File | Purpose |
|------|---------|
| [`AuthController.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/controller/AuthController.java) | `POST /api/v1/auth/login` |
| [`AuthService.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/service/AuthService.java) | AuthenticationManager → JWT generation |

### Repositories
| File | Purpose |
|------|---------|
| [`AppUserRepository.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/repository/AppUserRepository.java) | `findByUsernameOrEmail` for flexible login |
| [`UserCountryAccessRepository.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/repository/UserCountryAccessRepository.java) | Country ID projection query |
| [`UserCompanyAccessRepository.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/identity/repository/UserCompanyAccessRepository.java) | Company ID projection query |

### Modified Files
| File | Change |
|------|--------|
| [`pom.xml`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/pom.xml) | Added `spring-aop` + `aspectjweaver` |
| [`application.properties`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/resources/application.properties) | PostgreSQL, Flyway, JWT, and actuator config |

## Key Design Decisions (Phase 2)

1. **Fully stateless JWT** — All RBAC scope (tenant_id, country_ids, company_ids) is embedded in JWT custom claims. Zero DB hits after login.
2. **UserPrincipal dual-path construction** — From entity (login) and from claims (filter). Avoids coupling the filter to the database.
3. **AOP data scoping** — `@RequireCountryAccess` / `@RequireCompanyAccess` annotations resolved via method parameter names (enabled by Spring Boot's `-parameters` flag). SUPER_ADMIN bypasses all checks.
4. **Java 21 features used** — Records for DTOs, pattern matching `instanceof` in DataScopeAspect.
5. **No deprecated Security 5 APIs** — Pure SecurityFilterChain bean + lambda DSL. No `WebSecurityConfigurerAdapter`, no `antMatchers()`, no `authorizeRequests()`.

## Verification

- ✅ `mvnw compile` — **BUILD SUCCESS** (exit code 0, zero errors)

---

# Phase 3 — Dynamic Salary Component & SpEL Formula Engine

## Overview

Built the core payroll calculation engine: salary component definitions, per-employee structure assignments, a secure SpEL formula evaluator (using `SimpleEvaluationContext` to block injection attacks), and a dependency-resolving calculation service with comprehensive unit tests.

---

## Package Structure (New)

```
com.payroll.aj
├── common.enums
│   ├── ComponentType.java               ← EARNING / DEDUCTION
│   └── CalculationType.java            ← FLAT_AMOUNT / PERCENTAGE_OF_BASE / CUSTOM_FORMULA
├── payroll
│   ├── entity
│   │   ├── SalaryComponent.java         ← Reusable component template
│   │   └── EmployeeSalaryStructure.java ← Per-employee assignment + override
│   └── engine
│       ├── SpelFormulaEvaluatorService.java         ← Secure SpEL evaluator
│       ├── SalaryComponentCalculationService.java   ← Two-phase dependency resolution
│       ├── PayrollBreakdown.java                    ← Result record
│       └── ComponentLineItem.java                   ← Line item record
└── (test)
    └── payroll.engine
        ├── SpelFormulaEvaluatorServiceTest.java     ← 16 tests
        └── SalaryComponentCalculationServiceTest.java ← 12 tests
```

## Files Created (Phase 3)

### Database Migration
| File | Purpose |
|------|---------|
| [`V2__salary_components.sql`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/resources/db/migration/V2__salary_components.sql) | `salary_component` + `employee_salary_structure` tables |

### Enums
| File | Purpose |
|------|---------|
| [`ComponentType.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/ComponentType.java) | EARNING / DEDUCTION classification |
| [`CalculationType.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/CalculationType.java) | FLAT_AMOUNT / PERCENTAGE_OF_BASE / CUSTOM_FORMULA |

### JPA Entities
| File | Purpose |
|------|---------|
| [`SalaryComponent.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/entity/SalaryComponent.java) | Component template scoped to tenant/country |
| [`EmployeeSalaryStructure.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/entity/EmployeeSalaryStructure.java) | Per-employee assignment with override + date bounds |

### Engine Services
| File | Purpose |
|------|---------|
| [`SpelFormulaEvaluatorService.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/engine/SpelFormulaEvaluatorService.java) | Secure SpEL evaluator (SimpleEvaluationContext) |
| [`SalaryComponentCalculationService.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/engine/SalaryComponentCalculationService.java) | Two-phase dependency-resolving calculation engine |
| [`PayrollBreakdown.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/engine/PayrollBreakdown.java) | Result record (gross, deductions, net, itemised) |
| [`ComponentLineItem.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/engine/ComponentLineItem.java) | Single component line item record |

### Unit Tests
| File | Test Count | Covers |
|------|-----------|--------|
| [`SpelFormulaEvaluatorServiceTest.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/test/java/com/payroll/aj/payroll/engine/SpelFormulaEvaluatorServiceTest.java) | 16 | Arithmetic, rounding, injection safety, validation |
| [`SalaryComponentCalculationServiceTest.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/test/java/com/payroll/aj/payroll/engine/SalaryComponentCalculationServiceTest.java) | 12 | All calc types, overrides, dependencies, aggregation |

## Key Design Decisions (Phase 3)

1. **SimpleEvaluationContext** — Blocks `T()` type references, `new` constructors, `@bean` references, and arbitrary method calls. Only arithmetic operators and `#VARIABLE` references are permitted.
2. **Two-phase dependency resolution** — Phase 1 resolves FLAT/PERCENTAGE (no dependencies); Phase 2 iteratively resolves formulas as their dependencies become available (implicit topological sort with cycle detection).
3. **Variable naming convention** — Component code `EAR-IND-HRA01` → formula variable `#HRA01` (the `{CODE}` suffix). `#BASE_SALARY` is always a built-in.
4. **Override semantics** — For FLAT: overrides the value. For PERCENTAGE: overrides the percentage. For FORMULA: bypasses evaluation entirely.
5. **Java 21 features** — Pattern matching `switch` for `toBigDecimal()` and `resolveAmount()`. Records for `PayrollBreakdown` and `ComponentLineItem`.

## Verification

- ✅ `mvnw test-compile` — **BUILD SUCCESS** (exit code 0, zero errors)

---

# Phase 4 — Manual Batch Execution & Immutable Ledger Storage

## Overview

Implemented the full payroll batch execution pipeline: idempotent batch creation, per-employee calculation with immutable snapshot persistence, BigDecimal aggregation, and REST endpoints for execution and retrieval. 21 new files.

---

## Files Created (Phase 4)

### DDL
| File | Tables |
|------|--------|
| [`V3__payroll_execution_ledger.sql`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/resources/db/migration/V3__payroll_execution_ledger.sql) | `payroll_batch`, `payroll_record`, `payroll_record_detail` |

### Enums
| File | Values |
|------|--------|
| [`BatchStatus.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/BatchStatus.java) | DRAFT, PENDING, PROCESSING, COMPLETED, FAILED |
| [`PdfStatus.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/enums/PdfStatus.java) | PENDING, GENERATING, COMPLETED, FAILED |

### Entities
| File | Design Notes |
|------|-------------|
| [`PayrollBatch.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/entity/PayrollBatch.java) | Extends BaseEntity, unique (company_id, year, month) |
| [`PayrollRecord.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/entity/PayrollRecord.java) | Standalone (created_at only, no updated_at) — immutable snapshot |
| [`PayrollRecordDetail.java`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/entity/PayrollRecordDetail.java) | Standalone (no audit cols) — write-once, CASCADE delete |

### Repositories (7)
| File | Key Queries |
|------|------------|
| [`TenantRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/tenant/repository/TenantRepository.java) | getReferenceById (proxy) |
| [`CompanyRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/repository/CompanyRepository.java) | existsById, getReferenceById |
| [`EmployeeRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/employee/repository/EmployeeRepository.java) | findByCompanyIdAndEmploymentStatus |
| [`EmployeeSalaryStructureRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/repository/EmployeeSalaryStructureRepository.java) | JOIN FETCH component, date-bounded |
| [`PayrollBatchRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/repository/PayrollBatchRepository.java) | Idempotency check, JOIN FETCH executedBy |
| [`PayrollRecordRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/repository/PayrollRecordRepository.java) | Paginated JOIN FETCH employee, bulk delete |
| [`PayrollRecordDetailRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/repository/PayrollRecordDetailRepository.java) | Batch-load by record IDs |

### DTOs & Exception
| File | Purpose |
|------|---------|
| [`PayrollRunRequest`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/dto/PayrollRunRequest.java) | year + month with @Min/@Max validation |
| [`PayrollBatchResponse`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/dto/PayrollBatchResponse.java) | Batch metadata + totals + static from() factory |
| [`PayrollRecordResponse`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/dto/PayrollRecordResponse.java) | Employee payslip summary with detail list |
| [`PayrollRecordDetailResponse`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/dto/PayrollRecordDetailResponse.java) | Single component line item |
| [`PayrollBatchConflictException`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/exception/PayrollBatchConflictException.java) | @ResponseStatus(409) |

### Service & Controller
| File | Purpose |
|------|---------|
| [`PayrollExecutionService`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/service/PayrollExecutionService.java) | Full batch orchestration + query methods |
| [`PayrollExecutionController`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/controller/PayrollExecutionController.java) | 4 REST endpoints |

### Tests
| File | Tests | Covers |
|------|-------|--------|
| [`PayrollExecutionServiceTest`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/test/java/com/payroll/aj/payroll/service/PayrollExecutionServiceTest.java) | 7 | E2E run, empty employees, idempotency (COMPLETED/PROCESSING/FAILED), error propagation, company validation |

## Key Design Decisions (Phase 4)

1. **PayrollRecord is standalone** — Does not extend BaseEntity (only `created_at`, no `updated_at`) to match the immutable ledger DDL.
2. **Idempotency with retry** — COMPLETED/PROCESSING block re-runs (409 Conflict). DRAFT/PENDING/FAILED are cleaned up via bulk delete (DB CASCADE handles details) and retried.
3. **Batch-loaded details** — The paginated records endpoint batch-loads details for all records on the page via `findByRecordIdIn(List<Long>)` to avoid N+1 queries.
4. **@RequireCompanyAccess integration** — Service methods annotated for AOP-enforced company-level RBAC.
5. **Proxy references** — `getReferenceById()` used for Tenant/Company/AppUser to avoid unnecessary SELECT queries when building the batch entity.

## Verification

- ✅ `mvnw test-compile` — **BUILD SUCCESS** (exit code 0, zero errors)

---

# Phase 5 — Live Data Integration & UI Modernization

## Overview

Replaced all mock/dummy data across the frontend with live API calls, built an executive dashboard with real-time KPIs, created a full employee onboarding flow, and modernized the entire UI for readability and premium aesthetics.

---

## Backend Additions

### New Controllers & Services
| File | Purpose |
|------|---------|
| [`DashboardController`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/controller/DashboardController.java) | `GET /api/v1/dashboard?companyId=` — Returns KPIs, dept breakdown, recent joiners |
| [`DashboardService`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/service/DashboardService.java) | Aggregate queries: headcount, payroll cost, dept stats |
| [`SalaryComponentController`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/controller/SalaryComponentController.java) | `GET /api/v1/salary-components`, `POST /api/v1/salary-components` |
| [`SalaryComponentService`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/payroll/service/SalaryComponentService.java) | Component catalog CRUD with conflict checks |
| [`CompanyController`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/controller/CompanyController.java) | `GET /api/v1/companies` — scoped to user's accessible companies |
| [`DepartmentController`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/controller/DepartmentController.java) | `GET /api/v1/departments?companyId=` |
| [`EmployeeController`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/employee/controller/EmployeeController.java) | Full CRUD: list, get, create (POST), update (PUT) |
| [`GlobalExceptionHandler`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/common/exception/GlobalExceptionHandler.java) | `@RestControllerAdvice`: handles validation (400), conflict (409), access denied (403), not found (404) |

### Enhanced Services
| File | Change |
|------|--------|
| [`EmployeeService`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/employee/service/EmployeeService.java) | Added `createEmployee()` — onboards with auto salary structure init; `updateEmployee()` — partial field update |
| [`AppUserDetailsService`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/security/userdetails/AppUserDetailsService.java) | SUPER_ADMIN now gets all company IDs automatically from DB |
| [`CompanyRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/repository/CompanyRepository.java) | Added `findActiveByIdsWithCountry()` JOIN FETCH; `findByTenantIdWithCountry()` |
| [`DepartmentRepository`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/src/main/java/com/payroll/aj/organization/repository/DepartmentRepository.java) | `findActiveByCompanyId()` for filtered dept lists |

### Rich Seed Data (V5__rich_seed_data.sql)
- **+4 departments** (Operations, Product Management, Marketing, QA)
- **+7 salary components** (Special Allowance, LTA, Medical, Conveyance, Professional Tax, ESI, TDS)
- **+15 realistic Indian employees** (IDs 6–20) with bank details, IFSC codes, real PAN numbers
- **Admin scoping** — SUPER_ADMIN (id=1) gets explicit UserCompanyAccess rows for both companies

---

## Frontend Additions

### New API Modules (no more mocks)
| File | Endpoints |
|------|---------|
| [`api/dashboard.ts`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/api/dashboard.ts) | `getDashboardData(companyId)` |
| [`api/employee.ts`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/api/employee.ts) | `listEmployees()`, `createEmployee()`, `updateEmployee()` |
| [`api/company.ts`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/api/company.ts) | `listCompanies()` |
| [`api/component.ts`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/api/component.ts) | `listComponents()`, `createComponent()`, `testFormula()` |
| [`api/payroll.ts`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/api/payroll.ts) | `runPayroll()`, `listBatches()`, `getBatchRecords()` |

### New & Updated Pages
| File | Changes |
|------|---------|
| [`Dashboard.tsx`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/pages/Dashboard.tsx) | **NEW** — KPI cards, dept progress bars, recent joiners, quick actions |
| [`EmployeeDirectory.tsx`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/pages/EmployeeDirectory.tsx) | Replaced `MOCK_EMPLOYEES` with live API; integrated AddEmployeeModal |
| [`FormulaStudio.tsx`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/pages/FormulaStudio.tsx) | Replaced `MOCK_COMPONENTS` with live catalog; create modal |
| [`PayrollConsole.tsx`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/pages/PayrollConsole.tsx) | Replaced `MOCK_BATCHES` with live batch list + execution |
| [`AppLayout.tsx`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/components/layout/AppLayout.tsx) | Live company switcher; dark sidebar with active route highlight |

### New Components
| File | Purpose |
|------|---------|
| [`AddEmployeeModal.tsx`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-ui/src/components/employees/AddEmployeeModal.tsx) | Full employee onboarding form (name, email, dept, salary, PAN, bank) |

### UI/CSS Improvements
- **`index.css`** — Global dark theme with high-contrast CSS variables
- **`Dashboard.css`** — KPI card gradients, dept bar charts, premium layout
- **`EmployeeDirectory.css`** — Readable table, status badge colors
- **`PayrollConsole.css`** — Batch status colors, timeline feel
- **`FormulaStudio.css`** — Code-editor aesthetic for formula display
- **`AppLayout.css`** — Collapsible sidebar, glass header

## Key Design Decisions (Phase 5)

1. **Zero mocks in production code** — All `MOCK_*` arrays replaced with live `useEffect` + API calls.
2. **Optimistic loading** — Pages show loading spinners while fetching; errors shown as alerts.
3. **Auto salary structure** — `createEmployee()` in backend automatically assigns all active salary components for the employee's country.
4. **INR formatting** — `Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })` used consistently.
5. **Company-scoped queries** — Every API call passes `companyId` from Zustand `activeCompanyId`.

## Verification

- ✅ `mvnw compile` — **BUILD SUCCESS** (exit code 0, zero errors)
- ✅ `npm run build` — **Vite build success** (zero TypeScript errors)

---

# Phase 6 — DevOps & GitHub

## Overview

Configured version control, created proper `.gitignore`, and pushed the full codebase to GitHub.

---

## Infrastructure

### Docker Compose
All infrastructure services defined in [`docker-compose.yml`](file:///c:/Users/91877/OneDrive/Desktop/Java%20Studies/enterprise-payroll-platform/enterprise-payroll-api/docker-compose.yml):

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| PostgreSQL | `postgres:16-alpine` | 5433 | Primary database |
| Redis | `redis:7-alpine` | 6379 | Session/rate-limit cache |
| Kafka | `confluentinc/cp-kafka:latest` (KRaft) | 9092 | Async payslip events |
| MinIO | `quay.io/minio/minio:latest` | 9000/9001 | PDF payslip object storage |

### Startup Commands
```bash
# 1. Start all infrastructure
cd enterprise-payroll-api
docker compose up -d

# 2. Start backend (Spring Boot)
./mvnw spring-boot:run
# OR on Windows:
mvnw.cmd spring-boot:run

# 3. Start frontend (Vite dev server)
cd ../enterprise-payroll-ui
npm run dev
```

### Access Points
| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | — |
| Backend API | http://localhost:8080 | — |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |
| pgAdmin / psql | localhost:5433/payroll_db | payroll_admin / changeme |

## GitHub
- **Repository**: https://github.com/Ajaykumarkuppusamy/payroll
- **Branch**: `main`
- **Initial Commit**: All 6 phases, full codebase

### .gitignore Coverage
```
enterprise-payroll-api/target/     ← Maven build output
enterprise-payroll-ui/node_modules/ ← npm packages
enterprise-payroll-ui/dist/        ← Vite production build
*.env / application-local.yml      ← Secrets
*.log                              ← Runtime logs
```

## Known Startup Issues

> **IDE "Run Java" button** — The VS Code/IDE run button executes compiled `.class` files directly, bypassing Maven classpath. This causes `SpringApplication cannot be resolved`. Always use `./mvnw spring-boot:run` instead.

> **Kafka/MinIO optional** — Payroll execution works without Kafka/MinIO. PDF generation will log an error but core salary calculations still complete and save to the database.

> **First-time DB reset** — If Flyway fails with schema conflicts: `docker compose down -v && docker compose up -d`

## Verification

- ✅ Git initialized and committed with full project history
- ✅ Pushed to `https://github.com/Ajaykumarkuppusamy/payroll`
- ✅ `.gitignore` excludes build artifacts, secrets, and node_modules

