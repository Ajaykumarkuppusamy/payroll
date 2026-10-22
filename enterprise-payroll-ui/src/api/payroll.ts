import apiClient from './axios';

// ── Enums ────────────────────────────────────────────────────────────────────

export type BatchStatus = 'DRAFT' | 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type PdfStatus = 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED';

// ── Interfaces ───────────────────────────────────────────────────────────────

export interface PayrollRunRequest {
  year: number;
  month: number;
}

export interface PayrollBatchResponse {
  batch_id: number;
  company_id: number;
  year: number;
  month: number;
  status: BatchStatus;
  total_employees: number;
  total_gross_pay: number;
  total_deductions: number;
  total_net_pay: number;
  executed_at: string | null;
  created_at: string;
  executed_by_username: string;
}

export interface PayrollRecordDetailResponse {
  // Not strictly needed for this view but included for completeness if details are expanded later
  id?: number;
}

export interface PayrollRecordResponse {
  record_id: number;
  employee_id: number;
  employee_code: string;
  employee_name: string;
  base_salary: number;
  gross_earnings: number;
  total_deductions: number;
  net_salary: number;
  pdf_status: PdfStatus;
  payslip_pdf_url: string | null;
  details: PayrollRecordDetailResponse[];
}

export interface PresignedUrlResponse {
  url: string;
  expiresInSeconds: number;
}

// ── API Calls ─────────────────────────────────────────────────────────────────

/**
 * Triggers a monthly payroll run for a company.
 * POST /api/v1/companies/{companyId}/payroll/run
 */
export const runPayroll = async (
  companyId: number,
  year: number,
  month: number,
): Promise<PayrollBatchResponse> => {
  const { data } = await apiClient.post<PayrollBatchResponse>(
    `/companies/${companyId}/payroll/run`,
    { year, month } as PayrollRunRequest,
  );
  return data;
};

/**
 * Lists all historical payroll batches for a company.
 * GET /api/v1/companies/{companyId}/payroll/batches
 */
export const getBatches = async (
  companyId: number,
): Promise<PayrollBatchResponse[]> => {
  const { data } = await apiClient.get<PayrollBatchResponse[]>(
    `/companies/${companyId}/payroll/batches`,
  );
  return data;
};

/**
 * Retrieves paginated employee payroll records within a batch.
 * GET /api/v1/payroll/batches/{batchId}/records
 * Note: Backend returns a Spring Data Page, we map it.
 */
interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const getBatchRecords = async (
  batchId: number,
  page = 0,
  size = 20,
): Promise<Page<PayrollRecordResponse>> => {
  const { data } = await apiClient.get<Page<PayrollRecordResponse>>(
    `/payroll/batches/${batchId}/records`,
    { params: { page, size } },
  );
  return data;
};

/**
 * Retrieves a short-lived presigned URL for downloading the payslip PDF.
 * GET /api/v1/payroll/records/{recordId}/payslip
 */
export const getPayslip = async (
  recordId: number,
): Promise<PresignedUrlResponse> => {
  const { data } = await apiClient.get<PresignedUrlResponse>(
    `/payroll/records/${recordId}/payslip`,
  );
  return data;
};
