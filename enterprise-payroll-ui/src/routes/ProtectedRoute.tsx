import React, { useEffect } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { Spin } from 'antd';
import useAuthStore from '../store/useAuthStore';
import { TOKEN_KEY } from '../api/axios';

/**
 * ProtectedRoute — Guards any route that requires authentication.
 *
 * Strategy:
 * 1. If the Zustand store already has a valid session, render children.
 * 2. If the store is empty but a token exists in localStorage, re-hydrate
 *    the store (this handles the hard-refresh case — the store is ephemeral
 *    but localStorage persists across reloads).
 * 3. If no token anywhere, redirect to /login.
 *
 * Note: Re-hydrating from a raw token without calling /auth/me means we
 * cannot verify the token server-side here. The Axios 401 interceptor
 * handles the case where the persisted token is expired or revoked.
 */
const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, login } = useAuthStore();
  const [rehydrating, setRehydrating] = React.useState(!isAuthenticated);

  useEffect(() => {
    if (isAuthenticated) {
      setRehydrating(false);
      return;
    }

    // Attempt to re-hydrate from localStorage on hard refresh
    const storedToken = localStorage.getItem(TOKEN_KEY);
    if (storedToken) {
      try {
        // Decode the JWT payload to extract user claims (no signature verify)
        const payloadBase64 = storedToken.split('.')[1];
        const payload = JSON.parse(atob(payloadBase64)) as {
          sub?: string;
          role?: string;
          user_id?: number;
          allowed_country_ids?: number[];
          allowed_company_ids?: number[];
          exp?: number;
        };

        // Reject if token is already expired
        const nowSecs = Math.floor(Date.now() / 1000);
        if (payload.exp && payload.exp < nowSecs) {
          localStorage.removeItem(TOKEN_KEY);
          setRehydrating(false);
          return;
        }

        login(storedToken, {
          id: payload.user_id ?? 0,
          email: payload.sub ?? '',
          role: payload.role ?? '',
          allowedCountryIds: payload.allowed_country_ids ?? [],
          allowedCompanyIds: payload.allowed_company_ids ?? [],
        });
      } catch {
        // Malformed token — remove it
        localStorage.removeItem(TOKEN_KEY);
      }
    }

    setRehydrating(false);
  }, [isAuthenticated, login]);

  if (rehydrating) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#060b18',
        }}
      >
        <Spin size="large" />
      </div>
    );
  }

  // After re-hydration attempt: check store again
  const { isAuthenticated: finalAuth } = useAuthStore.getState();
  return finalAuth ? <Outlet /> : <Navigate to="/login" replace />;
};

export default ProtectedRoute;
