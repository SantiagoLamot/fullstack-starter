import { z } from 'zod';

// Replica AcceptInvitationRequest del backend: 10-72 caracteres, al menos una letra y un número.
export const activateSchema = z
  .object({
    password: z
      .string()
      .min(10, { error: 'Debe tener al menos 10 caracteres' })
      .max(72, { error: 'Debe tener como máximo 72 caracteres' })
      .regex(/^(?=.*\p{L})(?=.*\d).*$/u, { error: 'Debe incluir al menos una letra y un número' }),
    confirmPassword: z.string().min(1, { error: 'Repetí la contraseña' }),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    error: 'Las contraseñas no coinciden',
  });

export type ActivateFormValues = z.infer<typeof activateSchema>;
