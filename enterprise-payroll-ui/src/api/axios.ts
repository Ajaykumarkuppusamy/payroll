import axios from 'axios';

const TOKEN_KEY = 'payroll_jwt';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Request Interceptor ──────────────────────────────────────────────────────
// Attaches the stored JWT as a Bearer token on every outbound request.
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// ── Response Interceptor ─────────────────────────────────────────────────────
// Handles 401 Unauthorized globally: clears persisted auth state and
// redirects to the login page to prevent stale-token loops.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
      // Avoid importing the Zustand store here to prevent circular deps;
      // a hard redirect forces the store to re-hydrate on the login page.
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export { TOKEN_KEY };
export default apiClient;
