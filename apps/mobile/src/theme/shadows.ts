import type { ViewStyle } from 'react-native';

import { colors } from './colors';

/** Sombras muy suaves y difusas; la profundidad sale sobre todo del borde `sand`. */
export const shadows = {
  none: {} as ViewStyle,
  sm: {
    shadowColor: colors.ink,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 8,
    elevation: 1,
  } as ViewStyle,
  md: {
    shadowColor: colors.ink,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.08,
    shadowRadius: 16,
    elevation: 3,
  } as ViewStyle,
} as const;

export type ShadowToken = keyof typeof shadows;
