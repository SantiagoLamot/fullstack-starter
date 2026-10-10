import { useLocalSearchParams, useRouter } from 'expo-router';
import { StyleSheet } from 'react-native';

import { ActivateForm, extractInvitationToken } from '@/features/auth';
import { AuthBackdrop, Card, Text } from '@/shared/ui';
import { spacing } from '@/theme';

/** Deep link: `app://activar?token=...`. Sin token, se pide pegarlo. */
export default function ActivateScreen() {
  const router = useRouter();
  const { token } = useLocalSearchParams<{ token?: string }>();
  const initialToken = token ? extractInvitationToken(token) : undefined;

  return (
    <AuthBackdrop>
      <Card>
        <Text variant="label" color="textSecondary">
          (01) Invitación
        </Text>
        <Text variant="h1" style={styles.title}>
          Activá tu cuenta
        </Text>
        <Text variant="body" color="textSecondary" style={styles.intro}>
          Elegí tu contraseña para entrar al refugio.
        </Text>
        <ActivateForm
          initialToken={initialToken}
          onActivated={() => router.replace({ pathname: '/login', params: { activated: '1' } })}
        />
      </Card>
    </AuthBackdrop>
  );
}

const styles = StyleSheet.create({
  title: {
    marginTop: spacing.xxs,
    marginBottom: spacing.xs,
  },
  intro: {
    marginBottom: spacing.lg,
  },
});
