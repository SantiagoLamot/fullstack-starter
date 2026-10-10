import { describe, expect, it } from 'vitest';

import { activateSchema } from './activate.schema';

const parse = (password: string, confirmPassword = password) =>
  activateSchema.safeParse({ password, confirmPassword });

describe('activateSchema', () => {
  it('acepta una contraseña válida', () => {
    expect(parse('clave12345').success).toBe(true);
  });

  it('rechaza menos de 10 caracteres', () => {
    expect(parse('abc123').success).toBe(false);
  });

  it('rechaza más de 72 caracteres', () => {
    expect(parse(`a1${'x'.repeat(71)}`).success).toBe(false);
  });

  it('exige una letra y un número', () => {
    expect(parse('sololetrasaqui').success).toBe(false);
    expect(parse('1234567890123').success).toBe(false);
  });

  it('exige que la confirmación coincida', () => {
    const result = parse('clave12345', 'clave12346');
    expect(result.success).toBe(false);
    expect(result.error?.issues[0]?.path).toEqual(['confirmPassword']);
  });
});
