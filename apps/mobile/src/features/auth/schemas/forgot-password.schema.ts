import { z } from 'zod';

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, { error: 'Ingresá tu email' })
    .max(254, { error: 'El email es demasiado largo' })
    .pipe(z.email({ error: 'Ingresá un email válido' })),
});

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
