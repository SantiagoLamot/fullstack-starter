import { Badge } from '@mantine/core';

import { STATUS_COLORS, STATUS_LABELS, type UserResponse } from '../types';

interface UserStatusBadgesProps {
  user: Pick<UserResponse, 'status'>;
}

/** Estado de la cuenta (invitado, activo, deshabilitado). */
export function UserStatusBadges({ user }: UserStatusBadgesProps) {
  if (!user.status) return null;
  return <Badge color={STATUS_COLORS[user.status]}>{STATUS_LABELS[user.status]}</Badge>;
}
