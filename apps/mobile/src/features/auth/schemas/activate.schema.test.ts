import { extractInvitationToken } from '../utils/invitation-token';
import { activateSchema } from './activate.schema';

const valid = { token: 'tok123', password: 'secreto1234', confirmPassword: 'secreto1234' };

describe('activateSchema', () => {
  it('acepta datos válidos', () => {
    expect(activateSchema.safeParse(valid).success).toBe(true);
  });

  it('rechaza contraseñas de menos de 10 caracteres', () => {
    expect(
      activateSchema.safeParse({ ...valid, password: 'abc123', confirmPassword: 'abc123' }).success,
    ).toBe(false);
  });

  it('exige al menos una letra y un número', () => {
    const onlyLetters = 'sololetrasss';
    const onlyDigits = '1234567890123';
    for (const password of [onlyLetters, onlyDigits]) {
      expect(
        activateSchema.safeParse({ ...valid, password, confirmPassword: password }).success,
      ).toBe(false);
    }
  });

  it('rechaza más de 72 caracteres', () => {
    const password = `a1${'x'.repeat(71)}`;
    expect(
      activateSchema.safeParse({ ...valid, password, confirmPassword: password }).success,
    ).toBe(false);
  });

  it('marca la confirmación cuando no coincide', () => {
    const result = activateSchema.safeParse({ ...valid, confirmPassword: 'otra-cosa-123' });
    expect(result.success).toBe(false);
    if (!result.success) expect(result.error.issues[0]?.path).toEqual(['confirmPassword']);
  });

  it('exige token', () => {
    expect(activateSchema.safeParse({ ...valid, token: '  ' }).success).toBe(false);
  });
});

describe('extractInvitationToken', () => {
  it('devuelve el token suelto tal cual', () => {
    expect(extractInvitationToken('  abc_DEF-1 ')).toBe('abc_DEF-1');
  });

  it('extrae el token de un link https o del deep link', () => {
    expect(extractInvitationToken('https://admin.example.test/activar?token=abc%2B1')).toBe(
      'abc+1',
    );
    expect(extractInvitationToken('app://activar?x=1&token=zzz')).toBe('zzz');
  });
});
