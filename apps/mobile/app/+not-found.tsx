import { Link } from 'expo-router';
import { StyleSheet } from 'react-native';

import { Screen, Text } from '@/shared/ui';
import { spacing } from '@/theme';

export default function NotFoundScreen() {
  return (
    <Screen>
      <Text variant="h1">No encontrado</Text>
      <Text variant="body" color="textSecondary" style={styles.message}>
        Esta pantalla no existe.
      </Text>
      <Link href="/" style={styles.link}>
        <Text variant="bodyStrong" color="accent">
          Volver al inicio
        </Text>
      </Link>
    </Screen>
  );
}

const styles = StyleSheet.create({
  message: {
    marginTop: spacing.xs,
  },
  link: {
    marginTop: spacing.md,
  },
});
