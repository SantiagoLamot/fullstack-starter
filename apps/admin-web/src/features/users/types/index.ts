import type { Schemas } from '@app/api-types';

export type UserResponse = Schemas['UserResponse'];
export type UserPage = Schemas['PageResponseUserResponse'];
export type UserRole = NonNullable<UserResponse['role']>;
export type UserStatus = NonNullable<UserResponse['status']>;

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  USER: 'Usuario',
};

export const STATUS_LABELS: Record<UserStatus, string> = {
  INVITED: 'Invitado',
  ACTIVE: 'Activo',
  DISABLED: 'Deshabilitado',
};

export const STATUS_COLORS: Record<UserStatus, string> = {
  INVITED: 'warning',
  ACTIVE: 'success',
  DISABLED: 'neutral',
};
export type UpdateUserRequest = Schemas['UpdateUserRequest'];
