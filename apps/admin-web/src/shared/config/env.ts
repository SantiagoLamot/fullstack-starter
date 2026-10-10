import { z } from 'zod';

/**
 * Variables públicas (VITE_*) validadas al arrancar. Nunca poner secretos acá:
 * todo lo que empieza con VITE_ termina en el bundle del navegador.
 */
const envSchema = z.object({
  VITE_API_BASE_URL: z.string().min(1).default('/api/v1'),
  // Nombre visible del producto (header, pantallas públicas). Reemplazar al renombrar la plantilla.
  VITE_APP_NAME: z.string().min(1).default('App'),
});

const parsed = envSchema.parse(import.meta.env);

export const env = {
  apiBaseUrl: parsed.VITE_API_BASE_URL,
  appName: parsed.VITE_APP_NAME,
  isDev: import.meta.env.DEV,
} as const;
