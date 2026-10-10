import { z } from 'zod';

import { phoneField } from './edit-user.schema';

// Replica CreateUserRequest del backend (email <= 254, nombres <= 100, teléfono 8-15 dígitos, rol obligatorio).
export const createUserSchema = z.object({
  email: z.email({ error: 'Ingresá un email válido' }).max(254, { error: 'Máximo 254 caracteres' }),
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
  role: z.enum(['USER', 'ADMIN'], { error: 'Elegí un rol' }),
});

export type CreateUserFormValues = z.input<typeof createUserSchema>;
