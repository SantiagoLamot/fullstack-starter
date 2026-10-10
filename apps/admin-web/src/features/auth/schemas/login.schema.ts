import { z } from 'zod';

// Replica LoginRequest del backend: email (<= 254) y password (<= 72), ambos obligatorios.
export const loginSchema = z.object({
  email: z
    .email({ error: 'Ingresá un email válido' })
    .max(254, { error: 'El email no puede superar los 254 caracteres' }),
  password: z
    .string()
    .min(1, { error: 'Ingresá tu contraseña' })
    .max(72, { error: 'La contraseña no puede superar los 72 caracteres' }),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
