import { Link, useLocalSearchParams } from 'expo-router';
import { StyleSheet } from 'react-native';

import { LoginForm } from '@/features/auth';
import { AuthBackdrop, Card, Text } from '@/shared/ui';
import { spacing } from '@/theme';

export default function LoginScreen() {
  const { activated } = useLocalSearchParams<{ activated?: string }>();

  return (
    <AuthBackdrop>
      <Card>
        <Text variant="label" color="textSecondary">
          (01) Ingreso
        </Text>
        <Text variant="h1" style={styles.title}>
          Ingresá a tu cuenta
        </Text>
        <LoginForm
          notice={activated ? 'Tu cuenta quedó activada. Ya podés ingresar.' : undefined}
        />
      </Card>
      <Link href="/olvide-contrasena" style={[styles.link, styles.linkSpaced]}>
        <Text variant="bodySm" color="link">
          ¿Olvidaste tu contraseña?
        </Text>
      </Link>
      <Link href="/activar" style={styles.link}>
        <Text variant="bodySm" color="link">
          Tengo una invitación
        </Text>
      </Link>
    </AuthBackdrop>
  );
}

const styles = StyleSheet.create({
  title: {
    marginTop: spacing.xxs,
    marginBottom: spacing.lg,
  },
  link: {
    textAlign: 'center',
  },
  linkSpaced: {
    marginTop: spacing.md,
    marginBottom: spacing.sm,
  },
});
