import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Paper, Stack, Text, TextInput, Title } from '@mantine/core';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { paths } from '@/routes';
import { PublicPageLayout } from '@/shared/components/ui';
import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { requestPasswordReset } from '../api/password';
import { type ForgotPasswordFormValues, forgotPasswordSchema } from '../schemas/password.schema';
import {
  GENERIC_ERROR_MESSAGE,
  httpStatus,
  tooManyRequestsMessage,
} from '../utils/password-errors';

const SENT_MESSAGE =
  'Si el email corresponde a una cuenta activa, te enviamos un link para restablecer la contraseña. Revisá tu correo (y la carpeta de spam). El link vence en 1 hora.';

/** Página pública para pedir el link de restablecimiento: /olvide-contrasena */
export function ForgotPasswordPage() {
  const [sent, setSent] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  const onSubmit = async (values: ForgotPasswordFormValues) => {
    setFormError(null);
    try {
      await requestPasswordReset({ email: values.email });
      setSent(true);
    } catch (error) {
      if (httpStatus(error) === 429) {
        setFormError(tooManyRequestsMessage(error));
        return;
      }
      const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, ['email']);
      setFormError(unmapped[0]?.message ?? GENERIC_ERROR_MESSAGE);
    }
  };

  return (
    <PublicPageLayout>
      <Paper w="100%" p="xl" shadow="sm">
        {sent ? (
          <Stack>
            <Title order={2}>Revisá tu correo</Title>
            <Text>{SENT_MESSAGE}</Text>
            <Button component={Link} to={paths.login} fullWidth>
              Volver a iniciar sesión
            </Button>
          </Stack>
        ) : (
          <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
            <Stack>
              <div>
                <Title order={2}>Restablecer contraseña</Title>
                <Text size="sm" c="dimmed">
                  Ingresá tu email y te enviamos un link para elegir una contraseña nueva.
                </Text>
              </div>
              {formError && (
                <Alert color="danger" role="alert">
                  {formError}
                </Alert>
              )}
              <TextInput
                label="Email"
                type="email"
                autoComplete="username"
                error={errors.email?.message}
                {...register('email')}
              />
              <Button type="submit" fullWidth loading={isSubmitting}>
                Enviar link
              </Button>
              <Button component={Link} to={paths.login} variant="subtle">
                Volver a iniciar sesión
              </Button>
            </Stack>
          </form>
        )}
      </Paper>
    </PublicPageLayout>
  );
}
