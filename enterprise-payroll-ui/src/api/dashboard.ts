import apiClient from './axios';
import type { Employee } from './employee';
import type { PayrollBatchResponse } from './payroll';

export interface DepartmentDistribution {
  department_name: string;
  count: number;
}

export interface DashboardData {
  company_id: number;
  total_employees: number;
  active_employees: number;
  on_leave_employees: number;
  probation_employees: number;
  monthly_payroll_estimate: number;
  avg_salary: number;
  last_batch: PayrollBatchResponse | null;
  department_distribution: DepartmentDistribution[];
  recent_employees: Employee[];
}

/**
 * Fetches executive dashboard statistics for a company.
 * Maps to GET /api/v1/companies/{companyId}/dashboard
 */
export const getDashboardData = async (companyId: number): Promise<DashboardData> => {
  const { data } = await apiClient.get<DashboardData>(`/companies/${companyId}/dashboard`);
  return data;
};
