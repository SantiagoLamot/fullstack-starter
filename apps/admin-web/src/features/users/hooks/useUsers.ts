import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createUser,
  disableUser,
  enableUser,
  listUsers,
  resendInvitation,
  updateUser,
  type UserListParams,
} from '../api/users.api';
import type { UpdateUserRequest } from '../types';

const USERS_KEY = ['admin', 'users'] as const;

export function useUsers(params: UserListParams) {
  return useQuery({
    queryKey: [...USERS_KEY, params],
    queryFn: () => listUsers(params),
    placeholderData: keepPreviousData,
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: USERS_KEY }),
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateUserRequest }) => updateUser(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: USERS_KEY }),
  });
}

/** Acciones de estado: reenviar invitación, deshabilitar, habilitar. */
export function useUserStateAction(action: 'resend' | 'disable' | 'enable') {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      if (action === 'resend') await resendInvitation(id);
      else if (action === 'disable') await disableUser(id);
      else await enableUser(id);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: USERS_KEY }),
  });
}
