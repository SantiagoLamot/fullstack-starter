import axios, { type AxiosAdapter, type AxiosResponse } from 'axios';
import { describe, expect, it, vi } from 'vitest';

import { installRefreshInterceptor } from './refresh-interceptor';

function setup(refresh: () => Promise<string>) {
  let validToken = 'old';
  const onSessionExpired = vi.fn();
  const instance = axios.create({ baseURL: '/api/v1' });
  instance.interceptors.request.use((config) => {
    config.headers.set('Authorization', `Bearer ${validToken}`);
    return config;
  });
  const adapter: AxiosAdapter = (config) => {
    const ok = config.headers.get('Authorization') === 'Bearer new';
    const response = {
      data: {},
      status: ok ? 200 : 401,
      statusText: '',
      headers: {},
      config,
    } as AxiosResponse;
    return ok
      ? Promise.resolve(response)
      : Promise.reject(new axios.AxiosError('401', '401', config, null, response));
  };
  instance.defaults.adapter = adapter;
  installRefreshInterceptor(instance, {
    refresh: async () => {
      const token = await refresh();
      validToken = token;
      return token;
    },
    onSessionExpired,
    excludedUrls: ['/auth/login'],
  });
  return { instance, onSessionExpired };
}

describe('installRefreshInterceptor', () => {
  it('hace un solo refresh para varios 401 concurrentes y reintenta todas', async () => {
    let inflight: Promise<string> | null = null;
    const refresh = vi.fn(() => {
      inflight ??= new Promise<string>((resolve) => setTimeout(() => resolve('new'), 10));
      return inflight;
    });
    const { instance, onSessionExpired } = setup(refresh);

    const results = await Promise.all([instance.get('/a'), instance.get('/b'), instance.get('/c')]);

    expect(results.map((r) => r.status)).toEqual([200, 200, 200]);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(onSessionExpired).not.toHaveBeenCalled();
  });

  it('si el refresh falla, rechaza las requests y cierra la sesión', async () => {
    const refresh = vi.fn(() => Promise.reject(new Error('refresh inválido')));
    const { instance, onSessionExpired } = setup(refresh);

    await expect(instance.get('/a')).rejects.toMatchObject({ response: { status: 401 } });
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });

  it('no intenta refresh en las URLs excluidas (login)', async () => {
    const refresh = vi.fn(() => Promise.resolve('new'));
    const { instance } = setup(refresh);

    await expect(instance.post('/auth/login', {})).rejects.toMatchObject({
      response: { status: 401 },
    });
    expect(refresh).not.toHaveBeenCalled();
  });
});
