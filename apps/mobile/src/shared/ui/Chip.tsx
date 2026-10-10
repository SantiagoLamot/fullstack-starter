import { Pressable, StyleSheet, View } from 'react-native';

import { colors, radii, spacing } from '@/theme';

import { Text } from './Text';

interface ChipProps {
  label: string;
  /** Seleccionado: relleno `seaLight` (el texto sigue en charcoal). */
  selected?: boolean;
  /** Si se pasa, el chip es presionable (filtros); si no, es un tag informativo. */
  onPress?: () => void;
}

/** Tag/chip píldora con borde 1 px charcoal (filtros, Carpa/Sombrilla, estados). */
export function Chip({ label, selected = false, onPress }: ChipProps) {
  const content = (
    <Text variant="bodySm" color="textPrimary">
      {label}
    </Text>
  );

  if (!onPress) {
    return <View style={[styles.base, selected && styles.selected]}>{content}</View>;
  }

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[styles.base, selected && styles.selected]}
    >
      {content}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderColor: colors.primary,
    borderRadius: radii.pill,
    paddingVertical: spacing.xxs,
    paddingHorizontal: spacing.sm,
  },
  selected: {
    backgroundColor: colors.accentLight,
  },
});
