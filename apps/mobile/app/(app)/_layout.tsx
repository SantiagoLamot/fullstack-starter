import { Redirect, Stack } from 'expo-router';

import { NotUserNotice, useSessionStore } from '@/features/auth';
import { LoadingScreen } from '@/shared/ui';

/**
 * Guard: sin sesión ⇒ login; sesión con rol distinto de USER ⇒ aviso + logout.
 * (La seguridad real la aplica el backend; esto es solo UX.)
 */
export default function AppGroupLayout() {
  const status = useSessionStore((state) => state.status);
  const role = useSessionStore((state) => state.user?.role);

  if (status === 'loading') return <LoadingScreen />;
  if (status === 'unauthenticated') return <Redirect href="/(auth)/login" />;
  if (role !== 'USER') return <NotUserNotice />;

  return <Stack screenOptions={{ headerShown: false }} />;
}
