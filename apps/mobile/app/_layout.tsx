import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { bootstrapSession, installHttpAuth } from '@/features/auth';
import { queryClient } from '@/shared/lib/query-client';
import { ThemeProvider } from '@/theme';

// Bearer + refresh único en el cliente HTTP (idempotente).
installHttpAuth();

/** Layout raíz: providers globales, bootstrap de sesión y stack de navegación. */
export default function RootLayout() {
  useEffect(() => {
    void bootstrapSession();
  }, []);

  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <QueryClientProvider client={queryClient}>
          <StatusBar style="dark" />
          <Stack screenOptions={{ headerShown: false }} />
        </QueryClientProvider>
      </ThemeProvider>
    </SafeAreaProvider>
  );
}
