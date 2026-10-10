import { z } from 'zod';

import { extractInvitationToken } from '../utils/invitation-token';

// Reglas alineadas con AcceptInvitationRequest del backend: token <= 200,
// contraseña 10-72 caracteres con al menos una letra y un número.
export const PASSWORD_MIN = 10;
export const PASSWORD_MAX = 72;

export const activateSchema = z
  .object({
    token: z
      .string()
      .trim()
      .min(1, { error: 'Pegá el token o el link de tu invitación' })
      .refine((value) => extractInvitationToken(value).length <= 200, {
        error: 'El token de invitación no es válido',
      }),
    password: z
      .string()
      .min(PASSWORD_MIN, { error: `Debe tener al menos ${PASSWORD_MIN} caracteres` })
      .max(PASSWORD_MAX, { error: `Debe tener como máximo ${PASSWORD_MAX} caracteres` })
      .regex(/^(?=.*\p{L})(?=.*\d).*$/u, { error: 'Debe incluir al menos una letra y un número' }),
    confirmPassword: z.string().min(1, { error: 'Repetí la contraseña' }),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    error: 'Las contraseñas no coinciden',
  });

export type ActivateFormValues = z.infer<typeof activateSchema>;
