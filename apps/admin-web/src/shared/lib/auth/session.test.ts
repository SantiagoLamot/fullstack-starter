import { afterEach, describe, expect, it, vi } from 'vitest';

import { authClient, logoutRequest } from './session';
import { tokenStore } from './token-store';

describe('logoutRequest', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    tokenStore.clear();
  });

  it('llama a /auth/logout sin Authorization (revoca por cookie) y limpia el token', async () => {
    tokenStore.set('abc');
    const post = vi.spyOn(authClient, 'post').mockResolvedValue({ status: 204 });
    await logoutRequest();
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith('/auth/logout');
    expect(tokenStore.get()).toBeNull();
  });

  it('si falla, igual limpia el token y propaga el error', async () => {
    tokenStore.set('abc');
    vi.spyOn(authClient, 'post').mockRejectedValue(new Error('red'));
    await expect(logoutRequest()).rejects.toThrow('red');
    expect(tokenStore.get()).toBeNull();
  });
});
