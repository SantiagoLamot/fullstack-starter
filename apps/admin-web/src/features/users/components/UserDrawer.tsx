import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Divider, Drawer, Group, Stack, Text, TextInput } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { useAuth } from '@/shared/lib/auth';
import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { useUpdateUser, useUserStateAction } from '../hooks/useUsers';
import { type EditUserFormValues, editUserSchema } from '../schemas/edit-user.schema';
import { ROLE_LABELS, type UserResponse } from '../types';
import { availableUserActions } from '../utils/user-actions';
import { UserStatusBadges } from './UserStatusBadges';

interface UserDrawerProps {
  user: UserResponse | null;
  onClose: () => void;
}

export function UserDrawer({ user, onClose }: UserDrawerProps) {
  return (
    <Drawer
      opened={user !== null}
      onClose={onClose}
      position="right"
      title={user ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim() : ''}
    >
      {user && <UserDrawerContent key={user.id} user={user} onClose={onClose} />}
    </Drawer>
  );
}

function failureMessage(error: unknown): string {
  const problem = getProblemDetails(error);
  if (isAxiosError(error) && (error.response?.status === 422 || error.response?.status === 409)) {
    return problem?.detail ?? 'No se pudo completar la acción.';
  }
  return problem?.detail ?? 'Ocurrió un error. Intentá de nuevo.';
}

type Pending = 'disable' | 'enable' | null;

function UserDrawerContent({ user, onClose }: { user: UserResponse; onClose: () => void }) {
  const { user: me } = useAuth();
  const updateUser = useUpdateUser();
  const resend = useUserStateAction('resend');
  const disable = useUserStateAction('disable');
  const enable = useUserStateAction('enable');
  const [confirming, setConfirming] = useState<Pending>(null);

  const actions = availableUserActions(user, me?.id === user.id);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isDirty },
  } = useForm<EditUserFormValues>({
    resolver: zodResolver(editUserSchema),
    defaultValues: {
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      phone: user.phone ?? '',
    },
  });

  const onSubmit = async (values: EditUserFormValues) => {
    if (user.id === undefined) return;
    try {
      // PATCH parcial: un teléfono en blanco lo borra en el backend.
      await updateUser.mutateAsync({
        id: user.id,
        body: {
          firstName: values.firstName,
          lastName: values.lastName,
          phone: values.phone,
        },
      });
      notifications.show({ color: 'success', title: 'Usuario actualizado', message: user.email });
      onClose();
    } catch (error) {
      const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, [
        'firstName',
        'lastName',
        'phone',
      ]);
      notifications.show({
        color: 'danger',
        title: 'No se pudo guardar',
        message: unmapped[0]?.message ?? failureMessage(error),
      });
    }
  };

  const run = async (kind: 'resend' | 'disable' | 'enable') => {
    if (user.id === undefined) return;
    const mutation = { resend, disable, enable }[kind];
    const done = {
      resend: 'Invitación reenviada',
      disable: 'Usuario deshabilitado',
      enable: 'Usuario habilitado',
    }[kind];
    try {
      await mutation.mutateAsync(user.id);
      notifications.show({
        color: 'success',
        title: done,
        message: user.email,
      });
      onClose();
    } catch (error) {
      setConfirming(null);
      notifications.show({
        color: 'danger',
        title: 'No se pudo completar la acción',
        message: failureMessage(error),
      });
    }
  };

  return (
    <Stack>
      <Group justify="space-between">
        <UserStatusBadges user={user} />
        <Text size="sm" c="dimmed">
          {user.role ? ROLE_LABELS[user.role] : ''}
        </Text>
      </Group>
      <TextInput label="Email" value={user.email ?? ''} readOnly disabled />
      <TextInput label="Rol" value={user.role ? ROLE_LABELS[user.role] : ''} readOnly disabled />

      {actions.edit && (
        <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
          <Stack>
            <TextInput
              label="Nombre"
              required
              error={errors.firstName?.message}
              {...register('firstName')}
            />
            <TextInput
              label="Apellido"
              required
              error={errors.lastName?.message}
              {...register('lastName')}
            />
            <TextInput
              label="Teléfono"
              description="Solo dígitos, formato internacional, p. ej. 5491122334455"
              error={errors.phone?.message}
              {...register('phone')}
            />
            <Button type="submit" loading={updateUser.isPending} disabled={!isDirty}>
              Guardar cambios
            </Button>
          </Stack>
        </form>
      )}

      <Divider label="Acciones" labelPosition="left" />

      {actions.resendInvitation && (
        <Button
          variant="default"
          loading={resend.isPending}
          onClick={() => {
            void run('resend');
          }}
        >
          Reenviar invitación
        </Button>
      )}

      {confirming ? (
        <Alert
          color={confirming === 'disable' ? 'danger' : 'neutral'}
          title={confirming === 'disable' ? 'Deshabilitar usuario' : 'Habilitar usuario'}
        >
          <Stack gap="xs">
            <Text size="sm">
              {confirming === 'disable'
                ? 'Deshabilitar cierra todas sus sesiones y no podrá ingresar hasta que lo habilites.'
                : 'El usuario va a poder ingresar de nuevo.'}
            </Text>
            <Group>
              <Button
                color={confirming === 'disable' ? 'danger' : 'neutral'}
                loading={disable.isPending || enable.isPending}
                onClick={() => {
                  void run(confirming);
                }}
              >
                Confirmar
              </Button>
              <Button
                variant="default"
                onClick={() => {
                  setConfirming(null);
                }}
              >
                Cancelar
              </Button>
            </Group>
          </Stack>
        </Alert>
      ) : (
        <>
          {actions.disable && (
            <Button
              color="danger"
              variant="outline"
              onClick={() => {
                setConfirming('disable');
              }}
            >
              Deshabilitar
            </Button>
          )}
          {actions.enable && (
            <Button
              variant="outline"
              onClick={() => {
                setConfirming('enable');
              }}
            >
              Habilitar
            </Button>
          )}
        </>
      )}
    </Stack>
  );
}
