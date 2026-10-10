import { Ionicons } from '@expo/vector-icons';
import type { ComponentProps } from 'react';

import { colors, type ColorToken, iconSizes, type IconSizeToken } from '@/theme';

export type IconName = ComponentProps<typeof Ionicons>['name'];

interface IconProps {
  name: IconName;
  size?: IconSizeToken;
  color?: ColorToken;
}

/** Ícono de línea fina (variantes `-outline`). Nunca rellenos ni multicolor. */
export function Icon({ name, size = 'md', color = 'textPrimary' }: IconProps) {
  return <Ionicons name={name} size={iconSizes[size]} color={colors[color]} />;
}
