import { useRouter } from 'expo-router';
import { StyleSheet } from 'react-native';

import { ForgotPasswordForm } from '@/features/auth';
import { AuthBackdrop, Card, Text } from '@/shared/ui';
import { spacing } from '@/theme';

export default function ForgotPasswordScreen() {
  const router = useRouter();

  return (
    <AuthBackdrop>
      <Card>
        <Text variant="label" color="textSecondary">
          (01) Contraseña
        </Text>
        <Text variant="h1" style={styles.title}>
          ¿Olvidaste tu contraseña?
        </Text>
        <Text variant="body" color="textSecondary" style={styles.intro}>
          Ingresá tu email y te enviamos un link para elegir una nueva.
        </Text>
        <ForgotPasswordForm onBack={() => router.replace('/login')} />
      </Card>
    </AuthBackdrop>
  );
}

const styles = StyleSheet.create({
  title: { marginTop: spacing.xxs, marginBottom: spacing.xs },
  intro: { marginBottom: spacing.lg },
});
