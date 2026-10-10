import type { Schemas } from '@app/api-types';

import { http } from '@/shared/lib/http';

export async function acceptInvitation(body: Schemas['AcceptInvitationRequest']): Promise<void> {
  await http.post('/auth/invitations/accept', body);
}
