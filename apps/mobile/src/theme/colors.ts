/**
 * TOKENS DE MARCA (reemplazables). Único lugar con hex en mobile. Ver docs/brand/brand.md.
 * Para aplicar la marca de un proyecto: cambiar solo esta paleta y `typography.ts`.
 */
const palette = {
  brand: '#4263EB',
  primaryLight: '#DBE4FF',
  ink: '#212529',
  inkSoft: '#495057',
  line: '#DEE2E6',
  canvas: '#F8F9FA',
  white: '#FFFFFF',
  // Funcionales
  success: '#2F9E44',
  danger: '#E03131',
  warning: '#B26B00',
} as const;

/** Paleta + semánticos. Los componentes usan SOLO los semánticos de abajo. */
export const colors = {
  ...palette,
  background: palette.canvas,
  surface: palette.white,
  border: palette.line,
  textPrimary: palette.ink,
  textSecondary: palette.inkSoft,
  textInverted: palette.white,
  primary: palette.brand,
  accent: palette.primaryLight,
  accentLight: palette.primaryLight,
  link: palette.brand,
  success: palette.success,
} as const;

export type ColorToken = keyof typeof colors;
