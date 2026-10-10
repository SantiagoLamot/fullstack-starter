import { ActivityIndicator, StyleSheet, View } from 'react-native';

import { colors } from '@/theme';

/** Pantalla de carga a pantalla completa (bootstrap de sesión). */
export function LoadingScreen() {
  return (
    <View style={styles.container} accessibilityRole="progressbar">
      <ActivityIndicator color={colors.accent} size="large" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.background,
  },
});
