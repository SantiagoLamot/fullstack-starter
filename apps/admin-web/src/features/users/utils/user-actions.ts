import type { UserResponse, UserStatus } from '../types';

export interface UserActions {
  edit: boolean;
  resendInvitation: boolean;
  disable: boolean;
  enable: boolean;
}

/** Acciones visibles según el estado. Un admin no puede deshabilitarse a sí mismo (422). */
export function availableUserActions(
  user: Pick<UserResponse, 'status'>,
  isSelf: boolean,
): UserActions {
  const status: UserStatus = user.status ?? 'ACTIVE';
  return {
    edit: true,
    resendInvitation: status === 'INVITED',
    disable: status !== 'DISABLED' && !isSelf,
    enable: status === 'DISABLED',
  };
}
