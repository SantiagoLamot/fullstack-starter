import * as SecureStore from 'expo-secure-store';

import { refreshTokenStorage, SECURE_STORAGE_KEYS } from './secure-storage';

jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

describe('refreshTokenStorage', () => {
  beforeEach(() => jest.clearAllMocks());

  it('guarda el refresh token bajo la clave acordada', async () => {
    await refreshTokenStorage.set('abc');
    expect(SecureStore.setItemAsync).toHaveBeenCalledWith(SECURE_STORAGE_KEYS.refreshToken, 'abc');
  });

  it('lee el refresh token guardado', async () => {
    jest.mocked(SecureStore.getItemAsync).mockResolvedValue('abc');
    await expect(refreshTokenStorage.get()).resolves.toBe('abc');
    expect(SecureStore.getItemAsync).toHaveBeenCalledWith(SECURE_STORAGE_KEYS.refreshToken);
  });

  it('borra el refresh token', async () => {
    await refreshTokenStorage.clear();
    expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith(SECURE_STORAGE_KEYS.refreshToken);
  });
});
