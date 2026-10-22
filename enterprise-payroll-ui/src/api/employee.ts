import apiClient from './axios';

// ── Enums ────────────────────────────────────────────────────────────────────

/**
 * Mirrors the backend {@code EmploymentStatus} enum exactly.
 * Only ACTIVE employees are eligible for payroll calculation.
 */
export type EmploymentStatus =
  | 'ACTIVE'
  | 'ON_LEAVE'
  | 'PROBATION'
  | 'TERMINATED'
  | 'RESIGNED';

// ── Interfaces ───────────────────────────────────────────────────────────────

/**
 * Projection of the backend {@code EmployeeResponse} record as returned
 * by GET /api/v1/companies/{companyId}/employees.
 *
 * Field names are snake_case to match the backend's {@code @JsonProperty}
 * aliases on the {@code EmployeeResponse} record DTO.
 */
export interface Employee {
  id: number;
  employee_code: string;
  first_name: string;
  last_name: string;
  email: string;
  /** BigDecimal serialised as a JSON number from NUMERIC(19,2). */
  base_salary: number;
  employment_status: EmploymentStatus;
  /** ISO-8601 date string, e.g. "2023-04-01" */
  hire_date: string;
  department_name: string;
}

export interface Department {
  id: number;
  name: string;
  code: string;
  is_active: boolean;
}

export interface CreateEmployeeInput {
  department_id: number;
  first_name: string;
  last_name: string;
  email: string;
  base_salary: number;
  employment_status?: EmploymentStatus;
  hire_date: string;
  bank_account_number?: string;
  bank_name?: string;
  bank_ifsc_code?: string;
  tax_id?: string;
}

export interface UpdateEmployeeInput extends CreateEmployeeInput {
  employment_status: EmploymentStatus;
  termination_date?: string | null;
}

// ── API Calls ─────────────────────────────────────────────────────────────────

/**
 * Fetches all employees for a given company (all statuses — HR Directory view).
 *
 * Maps to: GET /api/v1/companies/{companyId}/employees
 */
export const getEmployees = async (companyId: number): Promise<Employee[]> => {
  const { data } = await apiClient.get<Employee[]>(
    `/companies/${companyId}/employees`,
  );
  return data;
};

/**
 * Fetches active departments for a company.
 *
 * Maps to: GET /api/v1/companies/{companyId}/departments
 */
export const getDepartments = async (companyId: number): Promise<Department[]> => {
  const { data } = await apiClient.get<Department[]>(
    `/companies/${companyId}/departments`,
  );
  return data;
};

/**
 * Creates and onboards a new employee.
 *
 * Maps to: POST /api/v1/companies/{companyId}/employees
 */
export const createEmployee = async (
  companyId: number,
  input: CreateEmployeeInput,
): Promise<Employee> => {
  const { data } = await apiClient.post<Employee>(
    `/companies/${companyId}/employees`,
    input,
  );
  return data;
};

/**
 * Updates an employee.
 *
 * Maps to: PUT /api/v1/companies/{companyId}/employees/{employeeId}
 */
export const updateEmployee = async (
  companyId: number,
  employeeId: number,
  input: UpdateEmployeeInput,
): Promise<Employee> => {
  const { data } = await apiClient.put<Employee>(
    `/companies/${companyId}/employees/${employeeId}`,
    input,
  );
  return data;
};

