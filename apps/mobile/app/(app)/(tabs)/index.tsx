import { ScrollView, StyleSheet } from 'react-native';

import { useSessionStore } from '@/features/auth';
import { Card, Screen, Text } from '@/shared/ui';
import { spacing } from '@/theme';

export default function HomeScreen() {
  const firstName = useSessionStore((state) => state.user?.firstName);

  return (
    <Screen edges={['top']}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        <Text variant="label" color="textSecondary">
          (Inicio)
        </Text>
        <Text variant="display" style={styles.greeting}>
          {firstName ? `¡Hola, ${firstName}!` : '¡Hola!'}
        </Text>
        <Card>
          <Text variant="body" color="textSecondary">
            Pantalla de inicio de la plantilla. Reemplazala por las secciones del producto.
          </Text>
        </Card>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingTop: spacing.lg, paddingBottom: spacing.xl },
  greeting: { marginTop: spacing.xxs, marginBottom: spacing.lg },
});
