import { StyleSheet, View } from 'react-native';

import { spacing } from '@/theme';

import { Button } from './Button';
import { Text } from './Text';

interface ErrorStateProps {
  message?: string;
  onRetry: () => void;
}

/** Error recuperable: mensaje neutro y reintento. */
export function ErrorState({
  message = 'No pudimos cargar el contenido. Revisá tu conexión.',
  onRetry,
}: ErrorStateProps) {
  return (
    <View style={styles.container}>
      <Text variant="body" color="danger" style={styles.text}>
        {message}
      </Text>
      <Button variant="secondary" label="Reintentar" onPress={onRetry} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { paddingVertical: spacing.lg, gap: spacing.md },
  text: { textAlign: 'center' },
});
