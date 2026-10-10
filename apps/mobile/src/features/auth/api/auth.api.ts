import type { RequestBody, Schema } from '@app/api-types';

import { http, plainHttp } from '@/shared/lib/http';

export type LoginPayload = RequestBody<'/api/v1/auth/login', 'post'>;
export type AcceptInvitationPayload = RequestBody<'/api/v1/auth/invitations/accept', 'post'>;
export type ChangePasswordPayload = RequestBody<'/api/v1/auth/password', 'put'>;
export type TokenResponse = Schema<'TokenResponse'>;
type AuthUser = NonNullable<TokenResponse['user']>;

// La baseURL ya incluye `/api/v1`.
export const authApi = {
  async login(payload: LoginPayload): Promise<TokenResponse> {
    return (await http.post<TokenResponse>('/auth/login', payload)).data;
  },
  /** Sin interceptores (evita recursión). */
  async refresh(refreshToken: string): Promise<TokenResponse> {
    return (await plainHttp.post<TokenResponse>('/auth/refresh', { refreshToken })).data;
  },
  async logout(refreshToken: string): Promise<void> {
    // Público: la sesión se identifica por el refresh del body; sin interceptores ni Authorization.
    await plainHttp.post('/auth/logout', { refreshToken });
  },
  /** Público y sin interceptores (no manda Authorization). 204 siempre (anti-enumeración). */
  async requestPasswordReset(email: string): Promise<void> {
    await plainHttp.post('/auth/password-reset/request', { email });
  },
  async changePassword(payload: ChangePasswordPayload): Promise<void> {
    await http.put('/auth/password', payload);
  },
  async me(accessToken: string): Promise<AuthUser> {
    const res = await plainHttp.get<AuthUser>('/auth/me', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    return res.data;
  },
  async acceptInvitation(payload: AcceptInvitationPayload): Promise<void> {
    await http.post('/auth/invitations/accept', payload);
  },
};
