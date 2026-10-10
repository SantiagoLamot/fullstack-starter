import { z } from 'zod';

// Misma política que AcceptInvitationRequest / PasswordResetConfirmRequest / ChangePasswordRequest.
const newPassword = z
  .string()
  .min(10, { error: 'Debe tener al menos 10 caracteres' })
  .max(72, { error: 'Debe tener como máximo 72 caracteres' })
  .regex(/^(?=.*\p{L})(?=.*\d).*$/u, { error: 'Debe incluir al menos una letra y un número' });

const confirmPassword = z.string().min(1, { error: 'Repetí la contraseña' });

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, { error: 'Ingresá tu email' })
    .max(254, { error: 'Debe tener como máximo 254 caracteres' })
    .pipe(z.email({ error: 'Ingresá un email válido' })),
});

export const resetPasswordSchema = z
  .object({ newPassword, confirmPassword })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    error: 'Las contraseñas no coinciden',
  });

export const changePasswordSchema = z
  .object({
    currentPassword: z
      .string()
      .min(1, { error: 'Ingresá tu contraseña actual' })
      .max(72, { error: 'Debe tener como máximo 72 caracteres' }),
    newPassword,
    confirmPassword,
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    error: 'Las contraseñas no coinciden',
  });

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
