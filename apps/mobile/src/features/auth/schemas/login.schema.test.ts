import { loginSchema } from './login.schema';

describe('loginSchema', () => {
  it('acepta un email y contraseña válidos', () => {
    const result = loginSchema.safeParse({ email: 'user@example.test', password: 'secreto123' });
    expect(result.success).toBe(true);
  });

  it('rechaza un email inválido', () => {
    const result = loginSchema.safeParse({ email: 'no-es-un-email', password: 'secreto123' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toEqual(['email']);
    }
  });

  it('rechaza contraseña vacía', () => {
    const result = loginSchema.safeParse({ email: 'user@example.test', password: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toEqual(['password']);
    }
  });
});
