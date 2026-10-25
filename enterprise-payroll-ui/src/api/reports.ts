import axiosInstance from './axios';

export interface ReportCatalogItem {
  code: string;
  title: string;
  category: string;
  description: string;
  requiredParameters: string[];
  supportedFormats: string[];
}

export interface ExecuteReportRequest {
  reportCode: string;
  companyId?: number;
  batchId?: number;
  employeeId?: number;
  format?: 'PDF' | 'EXCEL' | 'CSV';
}

export interface ExecuteReportResponse {
  jobId: string;
  reportCode: string;
  status: string;
  previewUrl: string;
  downloadUrl: string;
  message: string;
}

/**
 * Fetch available reports catalog from backend
 */
export async function fetchReportCatalog(): Promise<ReportCatalogItem[]> {
  const response = await axiosInstance.get<ReportCatalogItem[]>('/reports/catalog');
  return response.data;
}

/**
 * Execute report: triggers backend stored procedure -> stages data in report_staging_data -> runs JasperReports -> returns job response
 */
export async function executeReport(request: ExecuteReportRequest): Promise<ExecuteReportResponse> {
  const response = await axiosInstance.post<ExecuteReportResponse>('/reports/execute', request);
  return response.data;
}

/**
 * Fetch PDF preview blob for an executed report job
 */
export async function getReportPdfBlob(jobId: string): Promise<Blob> {
  const response = await axiosInstance.get<Blob>(`/reports/preview/${jobId}`, {
    responseType: 'blob',
  });
  return new Blob([response.data], { type: 'application/pdf' });
}

/**
 * Trigger browser file download of generated PDF
 */
export async function downloadReportFile(jobId: string, customFileName?: string): Promise<void> {
  const blob = await getReportPdfBlob(jobId);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', customFileName || `report_${jobId}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Direct shortcut to download employee payslip PDF
 */
export async function downloadPayslipPdf(batchId: number, employeeId: number): Promise<void> {
  const response = await axiosInstance.get<Blob>(
    `/reports/payslip/${batchId}/${employeeId}`,
    { responseType: 'blob' }
  );
  const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `payslip_${batchId}_emp${employeeId}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
