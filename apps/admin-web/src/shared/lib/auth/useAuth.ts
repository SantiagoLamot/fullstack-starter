import { useContext } from 'react';

import { AuthContext } from './AuthContext';
import type { AuthContextValue } from './types';

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return value;
}
