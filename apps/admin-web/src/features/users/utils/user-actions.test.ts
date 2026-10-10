import { describe, expect, it } from 'vitest';

import { availableUserActions } from './user-actions';

describe('availableUserActions', () => {
  it('INVITED: reenviar invitación y deshabilitar', () => {
    expect(availableUserActions({ status: 'INVITED' }, false)).toEqual({
      edit: true,
      resendInvitation: true,
      disable: true,
      enable: false,
    });
  });
  it('ACTIVE: solo deshabilitar', () => {
    const actions = availableUserActions({ status: 'ACTIVE' }, false);
    expect(actions.disable).toBe(true);
    expect(actions.enable).toBe(false);
    expect(actions.resendInvitation).toBe(false);
  });
  it('DISABLED: solo habilitar', () => {
    const actions = availableUserActions({ status: 'DISABLED' }, false);
    expect(actions.enable).toBe(true);
    expect(actions.disable).toBe(false);
  });
  it('en el propio usuario no se puede deshabilitar', () => {
    expect(availableUserActions({ status: 'ACTIVE' }, true).disable).toBe(false);
  });
});
