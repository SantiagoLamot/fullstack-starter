import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Paper, PasswordInput, Stack, Text, Title } from '@mantine/core';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router';

import { paths } from '@/routes';
import { PublicPageLayout } from '@/shared/components/ui';
import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { confirmPasswordReset } from '../api/password';
import { type ResetPasswordFormValues, resetPasswordSchema } from '../schemas/password.schema';
import {
  GENERIC_ERROR_MESSAGE,
  httpStatus,
  tooManyRequestsMessage,
} from '../utils/password-errors';

const DONE_MESSAGE =
  'Tu contraseña se actualizó. Si sos administrador, ingresá al panel; si sos usuario, ingresá desde la app.';
const INVALID_LINK_MESSAGE = 'El link es inválido o venció. Pedí uno nuevo.';

/** Página pública del link del mail de restablecimiento: /restablecer?token=... (todos los roles). */
export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [done, setDone] = useState(false);
  const [linkInvalid, setLinkInvalid] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  const onSubmit = async (values: ResetPasswordFormValues) => {
    if (!token) return;
    setFormError(null);
    try {
      await confirmPasswordReset({ token, newPassword: values.newPassword });
      setDone(true);
    } catch (error) {
      const status = httpStatus(error);
      if (status === 422) {
        setLinkInvalid(true);
        return;
      }
      if (status === 429) {
        setFormError(tooManyRequestsMessage(error));
        return;
      }
      const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, ['newPassword']);
      setFormError(unmapped[0]?.message ?? GENERIC_ERROR_MESSAGE);
    }
  };

  const requestAnother = (
    <Button component={Link} to={paths.forgotPassword} fullWidth>
      Pedir un link nuevo
    </Button>
  );

  return (
    <PublicPageLayout>
      <Paper w="100%" p="xl" shadow="sm">
        {done ? (
          <Stack>
            <Title order={2}>Contraseña actualizada</Title>
            <Text>{DONE_MESSAGE}</Text>
            <Button component={Link} to={paths.login} fullWidth>
              Ir a iniciar sesión
            </Button>
          </Stack>
        ) : linkInvalid ? (
          <Stack>
            <Alert color="danger" title="Link inválido" role="alert">
              {INVALID_LINK_MESSAGE}
            </Alert>
            {requestAnother}
          </Stack>
        ) : token ? (
          <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
            <Stack>
              <div>
                <Title order={2}>Nueva contraseña</Title>
                <Text size="sm" c="dimmed">
                  Elegí tu contraseña (mínimo 10 caracteres, con letras y números).
                </Text>
              </div>
              {formError && (
                <Alert color="danger" role="alert">
                  {formError}
                </Alert>
              )}
              <PasswordInput
                label="Contraseña nueva"
                autoComplete="new-password"
                error={errors.newPassword?.message}
                {...register('newPassword')}
              />
              <PasswordInput
                label="Repetir contraseña"
                autoComplete="new-password"
                error={errors.confirmPassword?.message}
                {...register('confirmPassword')}
              />
              <Button type="submit" fullWidth loading={isSubmitting}>
                Guardar contraseña
              </Button>
            </Stack>
          </form>
        ) : (
          <Stack>
            <Alert color="danger" title="Link inválido">
              Falta el código de restablecimiento. Usá el link que recibiste por mail.
            </Alert>
            {requestAnother}
          </Stack>
        )}
      </Paper>
    </PublicPageLayout>
  );
}
