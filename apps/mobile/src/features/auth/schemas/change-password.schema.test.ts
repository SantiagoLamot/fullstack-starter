import { changePasswordSchema } from './change-password.schema';
import { forgotPasswordSchema } from './forgot-password.schema';

describe('changePasswordSchema', () => {
  const valid = {
    currentPassword: 'vieja',
    newPassword: 'Nueva-clave-2026',
    confirmPassword: 'Nueva-clave-2026',
  };

  it('acepta datos válidos', () => {
    expect(changePasswordSchema.safeParse(valid).success).toBe(true);
  });

  it('rechaza nueva corta, sin número o que no coincide', () => {
    expect(
      changePasswordSchema.safeParse({ ...valid, newPassword: 'corta1', confirmPassword: 'corta1' })
        .success,
    ).toBe(false);
    expect(
      changePasswordSchema.safeParse({
        ...valid,
        newPassword: 'sinnumeroalguno',
        confirmPassword: 'sinnumeroalguno',
      }).success,
    ).toBe(false);
    expect(
      changePasswordSchema.safeParse({ ...valid, confirmPassword: 'otra-clave-2026' }).success,
    ).toBe(false);
  });
});

describe('forgotPasswordSchema', () => {
  it('valida el formato del email', () => {
    expect(forgotPasswordSchema.safeParse({ email: 'a@b.com' }).success).toBe(true);
    expect(forgotPasswordSchema.safeParse({ email: 'no-es-email' }).success).toBe(false);
  });
});
