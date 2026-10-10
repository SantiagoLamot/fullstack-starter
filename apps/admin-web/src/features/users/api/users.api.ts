import type { QueryParams, Schemas } from '@app/api-types';

import { http } from '@/shared/lib/http';

export type UserListParams = QueryParams<'/api/v1/admin/users', 'get'>;

export async function listUsers(
  params: UserListParams,
): Promise<Schemas['PageResponseUserResponse']> {
  const { data } = await http.get<Schemas['PageResponseUserResponse']>('/admin/users', { params });
  return data;
}

export async function createUser(
  body: Schemas['CreateUserRequest'],
): Promise<Schemas['UserResponse']> {
  const { data } = await http.post<Schemas['UserResponse']>('/admin/users', body);
  return data;
}

export async function updateUser(
  id: number,
  body: Schemas['UpdateUserRequest'],
): Promise<Schemas['UserResponse']> {
  const { data } = await http.patch<Schemas['UserResponse']>(`/admin/users/${String(id)}`, body);
  return data;
}

export async function resendInvitation(id: number): Promise<void> {
  await http.post(`/admin/users/${String(id)}/invitation`);
}

export async function disableUser(id: number): Promise<Schemas['UserResponse']> {
  const { data } = await http.post<Schemas['UserResponse']>(`/admin/users/${String(id)}/disable`);
  return data;
}

export async function enableUser(id: number): Promise<Schemas['UserResponse']> {
  const { data } = await http.post<Schemas['UserResponse']>(`/admin/users/${String(id)}/enable`);
  return data;
}
