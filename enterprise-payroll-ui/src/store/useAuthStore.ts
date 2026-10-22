import { create } from 'zustand';

// ── Types ────────────────────────────────────────────────────────────────────

export interface AuthUser {
  id: number;
  email: string;
  role: string;
  /** Country IDs this user is allowed to access (from JWT claims). */
  allowedCountryIds: number[];
  /** Company IDs this user is allowed to access (from JWT claims). */
  allowedCompanyIds: number[];
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  /**
   * The currently selected company context for all data operations.
   * Auto-set to the first allowed company on login; can be changed
   * by the operator via the Header company switcher.
   */
  activeCompanyId: number | null;
}

interface AuthActions {
  login: (token: string, user: AuthUser) => void;
  logout: () => void;
  /** Switches the active company context (triggers re-fetch in consumers). */
  setActiveCompany: (id: number) => void;
}

// ── Store ────────────────────────────────────────────────────────────────────

/**
 * Global authentication store powered by Zustand.
 *
 * State is intentionally ephemeral (no persist middleware) so that a hard
 * refresh forces re-hydration from localStorage via the Axios interceptor and
 * the ProtectedRoute guard, keeping a single source of truth for the token.
 */
const useAuthStore = create<AuthState & AuthActions>((set) => ({
  // ── Initial state ──────────────────────────────────────────────────────────
  token: null,
  user: null,
  isAuthenticated: false,
  activeCompanyId: null,

  // ── Actions ────────────────────────────────────────────────────────────────

  /**
   * Called after a successful /auth/login response.
   * Automatically seeds `activeCompanyId` with the first allowed company
   * so consumers never have to handle an unset context on first render.
   * The caller is responsible for persisting the token to localStorage.
   */
  login: (token, user) =>
    set({
      token,
      user,
      isAuthenticated: true,
      activeCompanyId: user.allowedCompanyIds.length > 0
        ? user.allowedCompanyIds[0]
        : null,
    }),

  /**
   * Clears all auth state. The Axios 401 interceptor also calls this path
   * indirectly via window.location.href = '/login'.
   */
  logout: () =>
    set({
      token: null,
      user: null,
      isAuthenticated: false,
      activeCompanyId: null,
    }),

  /**
   * Switches the operator's active company context.
   * The company ID must be present in user.allowedCompanyIds (enforced
   * by the backend DataScopeAspect as a secondary guard).
   */
  setActiveCompany: (id) => set({ activeCompanyId: id }),
}));

export default useAuthStore;
