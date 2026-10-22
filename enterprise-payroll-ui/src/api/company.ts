import apiClient from './axios';

export interface Company {
  id: number;
  name: string;
  registration_number: string;
  country_id: number;
  country_name: string;
  currency_code: string;
  is_active: boolean;
}

/**
 * Fetches all companies accessible by the authenticated user.
 * Maps to GET /api/v1/me/companies
 */
export const getMyCompanies = async (): Promise<Company[]> => {
  const { data } = await apiClient.get<Company[]>('/me/companies');
  return data;
};
