/**
 * Familias tipográficas. Por defecto, las del sistema (`undefined`). Para una marca con fuentes
 * propias: instalar el paquete expo-google-fonts, cargarlas con `useFonts` en `app/_layout.tsx`
 * y poner acá sus nombres.
 */
export const fonts = {
  display: undefined,
  body: undefined,
  bodyBold: undefined,
} as const;

const bold = { fontWeight: '700' as const };

/** Escala tipográfica (tamaño/interlineado). */
export const typography = {
  display: { fontFamily: fonts.display, fontSize: 34, lineHeight: 40, ...bold },
  h1: { fontFamily: fonts.display, fontSize: 28, lineHeight: 34, ...bold },
  h2: { fontFamily: fonts.display, fontSize: 22, lineHeight: 28, ...bold },
  h3: { fontFamily: fonts.display, fontSize: 18, lineHeight: 24, ...bold },
  body: { fontFamily: fonts.body, fontSize: 16, lineHeight: 24 },
  bodyStrong: { fontFamily: fonts.bodyBold, fontSize: 16, lineHeight: 24, ...bold },
  bodySm: { fontFamily: fonts.body, fontSize: 14, lineHeight: 20 },
  button: { fontFamily: fonts.bodyBold, fontSize: 16, lineHeight: 20, ...bold },
  label: {
    fontFamily: fonts.bodyBold,
    fontSize: 12,
    lineHeight: 16,
    letterSpacing: 0.96,
    textTransform: 'uppercase' as const,
    ...bold,
  },
  caption: { fontFamily: fonts.body, fontSize: 12, lineHeight: 16 },
} as const;

export type TypographyToken = keyof typeof typography;
