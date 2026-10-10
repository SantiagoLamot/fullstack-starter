/** Roles del sistema (ADR 0005): USER usa la app móvil, ADMIN el panel. */
export type Role = 'USER' | 'ADMIN';

export interface AuthUser {
  id: number;
  email: string;
  fullName: string;
  roles: readonly Role[];
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

export interface AuthState {
  status: AuthStatus;
  user: AuthUser | null;
  roles: readonly Role[];
}

export interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}
