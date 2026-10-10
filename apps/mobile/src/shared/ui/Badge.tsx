import { StyleSheet, View } from 'react-native';

import { colors, radii, spacing } from '@/theme';

import { Text } from './Text';

type BadgeTone = 'warning' | 'success' | 'danger';

interface BadgeProps {
  label: string;
  tone: BadgeTone;
}

/** Estado en píldora: borde y texto del color funcional (siempre con texto, nunca solo color). */
export function Badge({ label, tone }: BadgeProps) {
  return (
    <View style={[styles.base, { borderColor: colors[tone] }]}>
      <Text variant="bodySm" color={tone}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  base: {
    alignSelf: 'flex-start',
    borderWidth: 1,
    borderRadius: radii.pill,
    paddingVertical: spacing.xxs,
    paddingHorizontal: spacing.sm,
  },
});
