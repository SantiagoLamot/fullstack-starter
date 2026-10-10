import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Anchor,
  Button,
  Group,
  Paper,
  PasswordInput,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconArrowLeft } from '@tabler/icons-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { homePath } from '@/routes';
import { useAuth } from '@/shared/lib/auth';
import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { changePassword } from '../api/password';
import { type ChangePasswordFormValues, changePasswordSchema } from '../schemas/password.schema';
import {
  GENERIC_ERROR_MESSAGE,
  httpStatus,
  tooManyRequestsMessage,
} from '../utils/password-errors';

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administración',
  USER: 'Usuario',
};

const EMPTY: ChangePasswordFormValues = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

/** Mi cuenta: datos del usuario y cambio de contraseña (todos los roles del panel). */
export function AccountPage() {
  const { user, roles } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: EMPTY,
  });

  const onSubmit = async (values: ChangePasswordFormValues) => {
    setFormError(null);
    try {
      await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      reset(EMPTY);
      notifications.show({
        color: 'success',
        message: 'Contraseña actualizada. Cerramos tus otras sesiones.',
      });
    } catch (error) {
      const status = httpStatus(error);
      const problem = getProblemDetails(error);
      if (status === 429) {
        setFormError(tooManyRequestsMessage(error));
        return;
      }
      // 400 y 422 traen errors[{field,message}]: se asignan a los campos; el resto, alerta general.
      const unmapped = applyProblemFieldErrors(problem, setError, [
        'currentPassword',
        'newPassword',
      ]);
      const mapped = (problem?.errors?.length ?? 0) - unmapped.length;
      if (mapped > 0 && unmapped.length === 0) return;
      setFormError(unmapped[0]?.message ?? problem?.detail ?? GENERIC_ERROR_MESSAGE);
    }
  };

  return (
    <Stack maw={520} mx="auto" w="100%">
      <Anchor component={Link} to={homePath(roles)} size="sm" c="dimmed">
        <Group gap={4} wrap="nowrap">
          <IconArrowLeft size={14} stroke={1.5} />
          Volver
        </Group>
      </Anchor>
      <Title order={2}>Mi cuenta</Title>
      <Paper p="md" withBorder>
        <Stack gap="xs">
          <Text size="sm">
            <Text span fw={700}>
              Nombre:{' '}
            </Text>
            {user?.fullName}
          </Text>
          <Text size="sm">
            <Text span fw={700}>
              Email:{' '}
            </Text>
            {user?.email}
          </Text>
          <Text size="sm">
            <Text span fw={700}>
              Rol:{' '}
            </Text>
            {roles.map((role) => ROLE_LABELS[role] ?? role).join(', ')}
          </Text>
        </Stack>
      </Paper>

      <Paper p="md" withBorder>
        <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
          <Stack>
            <Title order={4}>Cambiar contraseña</Title>
            {formError && (
              <Alert color="danger" role="alert">
                {formError}
              </Alert>
            )}
            <PasswordInput
              label="Contraseña actual"
              autoComplete="current-password"
              error={errors.currentPassword?.message}
              {...register('currentPassword')}
            />
            <PasswordInput
              label="Contraseña nueva"
              description="Mínimo 10 caracteres, con letras y números."
              autoComplete="new-password"
              error={errors.newPassword?.message}
              {...register('newPassword')}
            />
            <PasswordInput
              label="Repetir contraseña nueva"
              autoComplete="new-password"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />
            <Button type="submit" loading={isSubmitting}>
              Cambiar contraseña
            </Button>
          </Stack>
        </form>
      </Paper>
    </Stack>
  );
}
