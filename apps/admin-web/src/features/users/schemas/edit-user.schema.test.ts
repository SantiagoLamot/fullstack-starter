import { describe, expect, it } from 'vitest';

import { editUserSchema } from './edit-user.schema';

const valid = { firstName: 'Ana', lastName: 'Pérez', phone: '5491111111111' };

describe('editUserSchema', () => {
  it('acepta datos válidos y teléfono vacío', () => {
    expect(editUserSchema.safeParse(valid).success).toBe(true);
    expect(editUserSchema.safeParse({ ...valid, phone: '' }).success).toBe(true);
  });

  it('teléfono: solo dígitos de 8 a 15', () => {
    for (const bad of ['+5491111111111', '1234567', '1234567890123456', '11 2233 4455', 'abc12345'])
      expect(editUserSchema.safeParse({ ...valid, phone: bad }).success).toBe(false);
  });

  it('nombre y apellido obligatorios', () => {
    expect(editUserSchema.safeParse({ ...valid, firstName: ' ' }).success).toBe(false);
    expect(editUserSchema.safeParse({ ...valid, lastName: '' }).success).toBe(false);
  });
});
