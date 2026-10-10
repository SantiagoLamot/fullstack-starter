import { MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import type { ReactNode } from 'react';

import { AuthProvider } from '@/shared/lib/auth';
import { queryClient } from '@/shared/lib/query-client';
import { cssVariablesResolver, theme } from '@/theme/theme';

interface AppProvidersProps {
  children: ReactNode;
}

/** Providers globales (salvo el router, que se monta en App). */
export function AppProviders({ children }: AppProvidersProps) {
  return (
    <MantineProvider
      theme={theme}
      cssVariablesResolver={cssVariablesResolver}
      defaultColorScheme="light"
    >
      <Notifications position="top-right" />
      <QueryClientProvider client={queryClient}>
        <AuthProvider>{children}</AuthProvider>
        {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
      </QueryClientProvider>
    </MantineProvider>
  );
}
