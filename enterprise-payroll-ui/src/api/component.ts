import apiClient from './axios';

// ── Enums ─────────────────────────────────────────────────────────────────────
// Mirror the backend Java enums exactly.

/** Classifies a component as earning or deduction. */
export type ComponentType = 'EARNING' | 'DEDUCTION';

/**
 * Determines how the monetary value is computed.
 *
 * - FLAT_AMOUNT        → fixed value from defaultValue
 * - PERCENTAGE_OF_BASE → (defaultValue / 100) × baseSalary
 * - CUSTOM_FORMULA     → SpEL expression in formulaExpression
 */
export type CalculationType =
  | 'FLAT_AMOUNT'
  | 'PERCENTAGE_OF_BASE'
  | 'CUSTOM_FORMULA';

// ── Interfaces ────────────────────────────────────────────────────────────────

/**
 * Projection of the backend {@code SalaryComponent} entity.
 *
 * Field names are snake_case to match {@code @JsonProperty} aliases
 * used by the backend DTO (consistent with EmployeeResponse contract).
 *
 * Component codes follow the pattern: {@code {TYPE}-{COUNTRY}-{CODE}}
 * e.g. {@code EAR-IND-HRA01} or {@code DED-IND-PF01}.
 * The trailing {@code {CODE}} segment becomes the SpEL variable name.
 */
export interface SalaryComponent {
  id: number;
  code: string;
  name: string;
  type: ComponentType;
  calculation_type: CalculationType;
  /**
   * SpEL expression (only set when calculation_type === 'CUSTOM_FORMULA').
   * Variables reference other resolved component values via #VARNAME syntax.
   * Example: "(#BASE_SALARY * 0.40) + #TRANSPORT"
   */
  formula_expression: string | null;
  /**
   * Monetary amount (FLAT_AMOUNT) or percentage integer (PERCENTAGE_OF_BASE).
   * Zero for CUSTOM_FORMULA components.
   */
  default_value: number;
  is_taxable: boolean;
  is_statutory: boolean;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Extracts the SpEL variable name from a full component code.
 * Mirrors SalaryComponentCalculationService.extractFormulaVariable().
 *
 * @example extractVariableName("EAR-IND-HRA01") → "HRA01"
 * @example extractVariableName("BASIC") → "BASIC"
 */
export const extractVariableName = (code: string): string => {
  const parts = code.split('-');
  return parts.length >= 3 ? parts.slice(2).join('-') : code;
};

export interface CreateComponentInput {
  code: string;
  name: string;
  type: ComponentType;
  calculation_type: CalculationType;
  formula_expression?: string | null;
  default_value?: number;
  is_taxable?: boolean;
  is_statutory?: boolean;
}

/**
 * Fetches salary component definitions for a company.
 *
 * Maps to: GET /api/v1/companies/{companyId}/components
 */
export const getComponents = async (
  companyId: number,
): Promise<SalaryComponent[]> => {
  const { data } = await apiClient.get<SalaryComponent[]>(
    `/companies/${companyId}/components`,
  );
  return data;
};

/**
 * Creates a new salary component template.
 *
 * Maps to: POST /api/v1/companies/{companyId}/components
 */
export const createComponent = async (
  companyId: number,
  input: CreateComponentInput,
): Promise<SalaryComponent> => {
  const { data } = await apiClient.post<SalaryComponent>(
    `/companies/${companyId}/components`,
    input,
  );
  return data;
};

