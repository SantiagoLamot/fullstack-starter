import { QueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';

const MAX_RETRIES = 2;

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        // No reintentar errores 4xx: no van a cambiar solos.
        retry: (failureCount, error) => {
          if (isAxiosError(error) && error.response && error.response.status < 500) {
            return false;
          }
          return failureCount < MAX_RETRIES;
        },
      },
      mutations: { retry: false },
    },
  });
}

export const queryClient = createQueryClient();
