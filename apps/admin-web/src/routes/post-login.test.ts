import { describe, expect, it } from 'vitest';

import { resolvePostLoginPath } from './paths';

describe('resolvePostLoginPath', () => {
  it('sin from va a la home del rol', () => {
    expect(resolvePostLoginPath(undefined, ['ADMIN'])).toBe('/');
  });

  it('respeta from si el rol tiene acceso (con query)', () => {
    expect(resolvePostLoginPath('/usuarios?q=a', ['ADMIN'])).toBe('/usuarios?q=a');
  });

  it('ignora from de una ruta a la que el rol no puede entrar', () => {
    expect(resolvePostLoginPath('/usuarios', ['USER'])).toBe('/');
  });

  it('ignora from inexistente, público o externo', () => {
    expect(resolvePostLoginPath('/no-existe', ['ADMIN'])).toBe('/');
    expect(resolvePostLoginPath('/login', ['ADMIN'])).toBe('/');
    expect(resolvePostLoginPath('//evil.com', ['ADMIN'])).toBe('/');
  });
});
