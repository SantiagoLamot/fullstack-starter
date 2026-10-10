import { z } from 'zod';

// Replicar las validaciones del LoginRequest del backend cuando exista (Fase 2).
export const loginSchema = z.object({
  email: z.email({ error: 'Ingresá un email válido' }),
  password: z.string().min(1, { error: 'Ingresá tu contraseña' }),
});

export type LoginFormValues = z.infer<typeof loginSchema>;
