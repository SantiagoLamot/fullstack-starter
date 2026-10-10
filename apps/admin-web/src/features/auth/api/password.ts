import type { Schemas } from '@app/api-types';

import { http } from '@/shared/lib/http';

export async function requestPasswordReset(body: Schemas['PasswordResetRequest']): Promise<void> {
  await http.post('/auth/password-reset/request', body);
}

export async function confirmPasswordReset(
  body: Schemas['PasswordResetConfirmRequest'],
): Promise<void> {
  await http.post('/auth/password-reset/confirm', body);
}

export async function changePassword(body: Schemas['ChangePasswordRequest']): Promise<void> {
  await http.put('/auth/password', body);
}
