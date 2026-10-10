/** Tamaños de íconos (línea fina; outline de Ionicons). */
export const iconSizes = {
  sm: 18,
  md: 24,
  lg: 32,
} as const;

export type IconSizeToken = keyof typeof iconSizes;
