import { z } from 'zod';

import { PASSWORD_MAX, PASSWORD_MIN } from './activate.schema';

// Alineado con ChangePasswordRequest: actual <= 72; nueva 10-72 con letra y número.
export const changePasswordSchema = z
  .object({
    currentPassword: z
      .string()
      .min(1, { error: 'Ingresá tu contraseña actual' })
      .max(PASSWORD_MAX, { error: `Debe tener como máximo ${PASSWORD_MAX} caracteres` }),
    newPassword: z
      .string()
      .min(PASSWORD_MIN, { error: `Debe tener al menos ${PASSWORD_MIN} caracteres` })
      .max(PASSWORD_MAX, { error: `Debe tener como máximo ${PASSWORD_MAX} caracteres` })
      .regex(/^(?=.*\p{L})(?=.*\d).*$/u, { error: 'Debe incluir al menos una letra y un número' }),
    confirmPassword: z.string().min(1, { error: 'Repetí la contraseña nueva' }),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    error: 'Las contraseñas no coinciden',
  });

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
