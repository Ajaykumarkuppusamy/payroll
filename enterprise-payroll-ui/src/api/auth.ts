import apiClient from './axios';

// ── Request / Response Types ─────────────────────────────────────────────────
// Typed to exactly match the backend LoginRequest and AuthResponse records.

/**
 * Payload for POST /auth/login.
 *
 * The `login` field accepts either an email address or a username,
 * matching the backend's flexible operator authentication.
 */
export interface LoginRequest {
  login: string;
  password: string;
}

/**
 * Successful response from POST /auth/login.
 *
 * All field names match the backend's snake_case @JsonProperty aliases.
 */
export interface AuthResponse {
  jwt_token: string;
  token_type: string;
  /** Token validity in seconds. */
  expires_in: number;
  user_id: number;
  email: string;
  role: string;
  allowed_country_ids: number[];
  allowed_company_ids: number[];
}

// ── Service Functions ────────────────────────────────────────────────────────

/**
 * Authenticates a system operator against the payroll backend.
 *
 * @param credentials - The operator's login + password.
 * @returns The full {@link AuthResponse} on success.
 * @throws AxiosError with a 401 status on invalid credentials.
 */
export const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials);
  return data;
};
