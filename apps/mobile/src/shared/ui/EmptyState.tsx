import { StyleSheet, View } from 'react-native';

import { spacing } from '@/theme';

import { Text } from './Text';

interface EmptyStateProps {
  message: string;
}

/** Estado vacío: mensaje centrado. Con marca, sumar una ilustración arriba del texto. */
export function EmptyState({ message }: EmptyStateProps) {
  return (
    <View style={styles.container}>
      <Text variant="h3" style={styles.text}>
        {message}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: 'center', paddingVertical: spacing.xl },
  text: { textAlign: 'center' },
});
