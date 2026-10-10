import axios, { type AxiosAdapter, type AxiosInstance } from 'axios';

import { installAuthInterceptor } from './auth-interceptor';

function setup(validToken: string, refresh: () => Promise<string>) {
  let token: string | null = 'old';
  const onAuthFailure = jest.fn();
  const calls: string[] = [];
  const adapter: AxiosAdapter = (config) => {
    calls.push(`${config.url}:${String(config.headers.get('Authorization'))}`);
    const ok = config.headers.get('Authorization') === `Bearer ${validToken}`;
    const response = { data: 'ok', status: ok ? 200 : 401, statusText: '', headers: {}, config };
    return ok
      ? Promise.resolve(response)
      : Promise.reject(new axios.AxiosError('401', '401', config, null, response));
  };
  const client: AxiosInstance = axios.create({ adapter });
  installAuthInterceptor(client, {
    getAccessToken: () => token,
    refresh: async () => {
      const next = await refresh();
      token = next;
      return next;
    },
    onAuthFailure,
  });
  return { client, onAuthFailure, calls };
}

describe('installAuthInterceptor', () => {
  it('hace un único refresh para varios 401 simultáneos y reintenta todos', async () => {
    const refresh = jest.fn().mockResolvedValue('new');
    const { client, onAuthFailure } = setup('new', refresh);

    const results = await Promise.all([client.get('/a'), client.get('/b'), client.get('/c')]);

    expect(results.map((r) => r.status)).toEqual([200, 200, 200]);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(onAuthFailure).not.toHaveBeenCalled();
  });

  it('si el refresh falla, llama onAuthFailure una vez y rechaza todos los requests', async () => {
    const refresh = jest.fn().mockRejectedValue(new Error('refresh inválido'));
    const { client, onAuthFailure } = setup('new', refresh);

    const results = await Promise.allSettled([client.get('/a'), client.get('/b')]);

    expect(results.every((r) => r.status === 'rejected')).toBe(true);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(onAuthFailure).toHaveBeenCalledTimes(1);
  });

  it('no reintenta más de una vez (sin bucle si el 401 persiste)', async () => {
    const refresh = jest.fn().mockResolvedValue('still-bad');
    const { client } = setup('new', refresh);

    await expect(client.get('/a')).rejects.toBeDefined();
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('no intenta refresh en los endpoints de auth', async () => {
    const refresh = jest.fn().mockResolvedValue('new');
    const { client } = setup('new', refresh);

    await expect(client.post('/auth/login')).rejects.toBeDefined();
    expect(refresh).not.toHaveBeenCalled();
  });
});
