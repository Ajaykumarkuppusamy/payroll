/**
 * Reports API — Payslip PDF, Payroll Excel, and future reports.
 * The backend generates PDFs via JasperReports and serves them as file streams.
 * Excel is generated server-side and downloaded as a blob.
 */
import axiosInstance from './axios';

/**
 * Download payslip PDF for a specific employee + batch.
 * Backend: GET /api/v1/reports/payslip/{batchId}/{employeeId}
 * Returns a blob → triggers browser download.
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

/**
 * Download payroll summary Excel for a batch.
 * Backend: GET /api/v1/reports/payroll-summary/{batchId}
 */
export async function downloadPayrollExcel(batchId: number): Promise<void> {
  const response = await axiosInstance.get<Blob>(
    `/reports/payroll-summary/${batchId}`,
    { responseType: 'blob' }
  );
  const url = window.URL.createObjectURL(
    new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
  );
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `payroll_summary_batch${batchId}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Download department headcount PDF.
 * Backend: GET /api/v1/reports/headcount/{companyId}
 */
export async function downloadHeadcountPdf(companyId: number): Promise<void> {
  const response = await axiosInstance.get<Blob>(
    `/reports/headcount/${companyId}`,
    { responseType: 'blob' }
  );
  const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `headcount_report_${companyId}.pdf`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
