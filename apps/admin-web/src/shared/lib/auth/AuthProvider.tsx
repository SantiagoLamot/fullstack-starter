import { useQueryClient } from '@tanstack/react-query';
import { type ReactNode, useCallback, useEffect, useMemo, useState } from 'react';

import { AuthContext } from './AuthContext';
import {
  fetchMe,
  loginRequest,
  logoutRequest,
  refreshAccessToken,
  setSessionExpiredHandler,
} from './session';
import { tokenStore } from './token-store';
import type { AuthContextValue, AuthState } from './types';

const LOADING: AuthState = { status: 'loading', user: null, roles: [] };
const ANONYMOUS: AuthState = { status: 'unauthenticated', user: null, roles: [] };

interface AuthProviderProps {
  children: ReactNode;
}

/** Sesión: access token en memoria; al cargar se recupera con refresh (cookie) + /auth/me. */
export function AuthProvider({ children }: AuthProviderProps) {
  const queryClient = useQueryClient();
  const [state, setState] = useState<AuthState>(LOADING);

  const clearSession = useCallback(() => {
    tokenStore.clear();
    queryClient.clear();
    setState(ANONYMOUS);
  }, [queryClient]);

  useEffect(() => {
    let active = true;
    setSessionExpiredHandler(clearSession);
    // refreshAccessToken es de vuelo único: StrictMode no duplica la rotación del refresh.
    refreshAccessToken()
      .then(fetchMe)
      .then((user) => {
        if (active) setState({ status: 'authenticated', user, roles: user.roles });
      })
      .catch(() => {
        if (active) setState(ANONYMOUS);
      });
    return () => {
      active = false;
      setSessionExpiredHandler(null);
    };
  }, [clearSession]);

  const login = useCallback(async (email: string, password: string) => {
    const user = await loginRequest(email, password);
    setState({ status: 'authenticated', user, roles: user.roles });
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } catch {
      // Aunque el backend falle, la sesión local se cierra igual.
    }
    clearSession();
  }, [clearSession]);

  const value = useMemo<AuthContextValue>(
    () => ({ ...state, login, logout }),
    [state, login, logout],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}
