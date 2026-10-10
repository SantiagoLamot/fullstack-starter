import { Redirect, Stack } from 'expo-router';

import { useSessionStore } from '@/features/auth';
import { LoadingScreen } from '@/shared/ui';

/** Grupo público: con sesión activa ⇒ va a la app (el grupo (app) valida el rol). */
export default function AuthGroupLayout() {
  const status = useSessionStore((state) => state.status);

  if (status === 'loading') return <LoadingScreen />;
  if (status === 'authenticated') return <Redirect href="/" />;

  return <Stack screenOptions={{ headerShown: false }} />;
}
