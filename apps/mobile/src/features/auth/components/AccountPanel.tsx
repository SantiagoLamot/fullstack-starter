import { useLocalSearchParams, useRouter } from 'expo-router';
import { ScrollView, StyleSheet, View } from 'react-native';

import { Button, Card, Screen, Text } from '@/shared/ui';
import { spacing } from '@/theme';

import { logout } from '../api/session.service';
import { useSessionStore } from '../store/session.store';

/** Tab Cuenta: datos del usuario, cambio de contraseña y cierre de sesión. */
export function AccountPanel() {
  const router = useRouter();
  const { passwordChanged } = useLocalSearchParams<{ passwordChanged?: string }>();
  const user = useSessionStore((s) => s.user);
  const fullName = [user?.firstName, user?.lastName].filter(Boolean).join(' ');

  return (
    <Screen edges={['top']}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
        <Text variant="label" color="textSecondary">
          (CUENTA)
        </Text>
        <Text variant="h1" style={styles.title}>
          Tu cuenta
        </Text>
        <Card>
          {fullName ? <Text variant="h3">{fullName}</Text> : null}
          {user?.email ? (
            <Text variant="body" color="textSecondary">
              {user.email}
            </Text>
          ) : null}
        </Card>
        <View style={styles.section}>
          {passwordChanged ? (
            <Text variant="bodySm" color="success" style={styles.notice}>
              Contraseña actualizada. Cerramos tus otras sesiones.
            </Text>
          ) : null}
          <Button
            variant="secondary"
            label="Cambiar contraseña"
            onPress={() => router.push('/cambiar-contrasena')}
          />
        </View>
        <View style={styles.section}>
          <Button variant="secondary" label="Cerrar sesión" onPress={() => void logout()} />
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { paddingTop: spacing.lg, paddingBottom: spacing.lg },
  title: { marginBottom: spacing.lg },
  section: { marginTop: spacing.md },
  notice: { marginBottom: spacing.sm },
});
