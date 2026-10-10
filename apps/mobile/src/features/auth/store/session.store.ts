import { create } from 'zustand';

import type { Schema } from '@app/api-types';

export type SessionStatus = 'loading' | 'authenticated' | 'unauthenticated';

export type SessionUser = Schema<'AuthUserResponse'>;

interface SessionState {
  /** Arranca en `loading` hasta que termina el bootstrap (refresh + /me). */
  status: SessionStatus;
  user: SessionUser | null;
  /** Access token en memoria (nunca en SecureStore). */
  accessToken: string | null;
  setAccessToken: (accessToken: string) => void;
  setSession: (session: { user: SessionUser; accessToken: string }) => void;
  clearSession: () => void;
}

export const useSessionStore = create<SessionState>((set) => ({
  status: 'loading',
  user: null,
  accessToken: null,
  setAccessToken: (accessToken) => set({ accessToken }),
  setSession: ({ user, accessToken }) => set({ status: 'authenticated', user, accessToken }),
  clearSession: () => set({ status: 'unauthenticated', user: null, accessToken: null }),
}));
