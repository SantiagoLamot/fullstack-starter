import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Paper, PasswordInput, Stack, Text, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router';

import { paths } from '@/routes';
import { PublicPageLayout } from '@/shared/components/ui';
import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { acceptInvitation } from '../api/accept-invitation';
import { type ActivateFormValues, activateSchema } from '../schemas/activate.schema';

const ACTIVATED_MESSAGE =
  'Tu cuenta está activa. Si sos administrador, ingresá al panel; si sos usuario, ingresá desde la app.';

/** Página pública del link del mail de invitación: /activar?token=... */
export function ActivatePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [activated, setActivated] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ActivateFormValues>({
    resolver: zodResolver(activateSchema),
    defaultValues: { password: '', confirmPassword: '' },
  });

  const onSubmit = async (values: ActivateFormValues) => {
    if (!token) return;
    try {
      await acceptInvitation({ token, password: values.password });
      setActivated(true);
    } catch (error) {
      const problem = getProblemDetails(error);
      const unmapped = applyProblemFieldErrors(problem, setError, ['password']);
      const invalidLink =
        isAxiosError(error) &&
        // El backend responde 422 (BusinessException) para invitación inválida/vencida/usada.
        error.response?.status === 422;
      notifications.show({
        color: 'danger',
        title: 'No se pudo activar la cuenta',
        message:
          unmapped[0]?.message ??
          (invalidLink
            ? 'El link es inválido o venció. Pedí una nueva invitación.'
            : 'Ocurrió un error. Intentá de nuevo.'),
      });
    }
  };

  return (
    <PublicPageLayout>
      <Paper w="100%" p="xl" shadow="sm">
        {activated ? (
          <Stack>
            <Title order={2}>Cuenta activa</Title>
            <Text>{ACTIVATED_MESSAGE}</Text>
            <Button component={Link} to={paths.dashboard} fullWidth>
              Ir al panel
            </Button>
          </Stack>
        ) : token ? (
          <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
            <Stack>
              <div>
                <Title order={2}>Activar cuenta</Title>
                <Text size="sm" c="dimmed">
                  Elegí tu contraseña (mínimo 10 caracteres, con letras y números).
                </Text>
              </div>
              <PasswordInput
                label="Contraseña"
                autoComplete="new-password"
                error={errors.password?.message}
                {...register('password')}
              />
              <PasswordInput
                label="Repetir contraseña"
                autoComplete="new-password"
                error={errors.confirmPassword?.message}
                {...register('confirmPassword')}
              />
              <Button type="submit" fullWidth loading={isSubmitting}>
                Activar cuenta
              </Button>
            </Stack>
          </form>
        ) : (
          <Stack>
            <Alert color="danger" title="Link inválido">
              Falta el código de invitación. Usá el link que recibiste por mail.
            </Alert>
            <Button component={Link} to={paths.login} variant="subtle">
              Ir a iniciar sesión
            </Button>
          </Stack>
        )}
      </Paper>
    </PublicPageLayout>
  );
}
