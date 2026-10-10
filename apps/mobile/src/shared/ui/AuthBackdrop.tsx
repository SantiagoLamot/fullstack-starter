import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, spacing } from '@/theme';

import { Text } from './Text';

interface AuthBackdropProps {
  children: ReactNode;
}

/**
 * Marco de las pantallas públicas (login, activación, olvidé contraseña): nombre del producto y
 * el formulario en una tarjeta. Para aplicar la marca, reemplazar el título por el logo
 * (docs/brand/brand.md).
 */
export function AuthBackdrop({ children }: AuthBackdropProps) {
  return (
    <View style={styles.root}>
      <SafeAreaView style={styles.safeArea} edges={['top', 'bottom']}>
        <ScrollView
          contentContainerStyle={styles.scroll}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <Text variant="display" style={styles.title}>
            {process.env.EXPO_PUBLIC_APP_NAME ?? 'App'}
          </Text>
          {children}
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  safeArea: { flex: 1 },
  scroll: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xl,
    gap: spacing.xl,
  },
  title: { textAlign: 'center' },
});
