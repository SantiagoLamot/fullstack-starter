import { createContext, type ReactNode, useContext, useMemo } from 'react';

import { colors } from './colors';
import { radii } from './radii';
import { shadows } from './shadows';
import { spacing } from './spacing';
import { typography } from './typography';

const theme = { colors, typography, spacing, radii, shadows } as const;

export type Theme = typeof theme;

const ThemeContext = createContext<Theme>(theme);

interface ThemeProviderProps {
  children: ReactNode;
}

/**
 * Expone los tokens vía contexto para el día en que haya variantes (oscuro,
 * marca por evento, etc). Fase 1: siempre el mismo tema fijo.
 */
export function ThemeProvider({ children }: ThemeProviderProps) {
  const value = useMemo(() => theme, []);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): Theme {
  return useContext(ThemeContext);
}
