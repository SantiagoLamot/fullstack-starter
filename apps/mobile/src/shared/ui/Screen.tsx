import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { type Edge, SafeAreaView } from 'react-native-safe-area-context';

import { colors, spacing } from '@/theme';

interface ScreenProps {
  children: ReactNode;
  /** `false` para pantallas con su propio scroll/layout (ej. listas). */
  padded?: boolean;
  /** Bordes con safe area; en pantallas con tab bar usar `['top']`. */
  edges?: Edge[];
}

/** Contenedor base de pantalla: fondo del tema + safe area + padding horizontal. */
export function Screen({ children, padded = true, edges = ['top', 'bottom'] }: ScreenProps) {
  return (
    <SafeAreaView style={styles.safeArea} edges={edges}>
      <View style={[styles.content, padded && styles.padded]}>{children}</View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    flex: 1,
  },
  padded: {
    paddingHorizontal: spacing.md,
  },
});
