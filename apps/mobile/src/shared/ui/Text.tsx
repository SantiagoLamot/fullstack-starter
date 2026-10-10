import { Text as RNText, type TextProps as RNTextProps } from 'react-native';

import { colors, typography, type TypographyToken } from '@/theme';

interface TextProps extends RNTextProps {
  variant?: TypographyToken;
  color?: keyof typeof colors;
}

/** Texto base: toda tipografía sale de `src/theme/typography.ts`, nunca tamaños sueltos. */
export function Text({ variant = 'body', color = 'textPrimary', style, ...rest }: TextProps) {
  return <RNText style={[typography[variant], { color: colors[color] }, style]} {...rest} />;
}
