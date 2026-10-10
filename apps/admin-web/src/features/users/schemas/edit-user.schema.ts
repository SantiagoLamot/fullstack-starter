import { z } from 'zod';

// Replica UpdateUserRequest / CreateUserRequest: nombres <= 100, teléfono solo dígitos 8-15.
// Teléfono vacío = sin dato (en PATCH lo borra).
export const phoneField = z.union([
  z.literal(''),
  z
    .string()
    .trim()
    .regex(/^\d{8,15}$/, { error: 'Solo dígitos, de 8 a 15, sin + ni espacios' }),
]);

export const editUserSchema = z.object({
  firstName: z
    .string()
    .trim()
    .min(1, { error: 'Ingresá el nombre' })
    .max(100, { error: 'Máximo 100 caracteres' }),
  lastName: z
    .string()
    .trim()
    .min(1, { error: 'Ingresá el apellido' })
    .max(100, { error: 'Máximo 100 caracteres' }),
  phone: phoneField,
});

export type EditUserFormValues = z.input<typeof editUserSchema>;
