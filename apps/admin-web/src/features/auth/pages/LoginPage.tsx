import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Anchor,
  Button,
  Paper,
  PasswordInput,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation } from 'react-router';

import { paths, resolvePostLoginPath } from '@/routes';
import { PublicPageLayout } from '@/shared/components/ui';
import { useAuth } from '@/shared/lib/auth';
import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { type LoginFormValues, loginSchema } from '../schemas/login.schema';
import type { LoginLocationState } from '../types/location-state';

const INVALID_CREDENTIALS = 'Email o contraseña incorrectos.';
const GENERIC_ERROR = 'No pudimos iniciar sesión. Intentá de nuevo en unos minutos.';

export function LoginPage() {
  const { status, login, roles } = useAuth();
  const location = useLocation();
  const locationState = location.state as LoginLocationState | null;
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  if (status === 'authenticated') {
    return <Navigate to={resolvePostLoginPath(locationState?.from, roles)} replace />;
  }

  const onSubmit = async (values: LoginFormValues) => {
    setFormError(null);
    try {
      // Al quedar autenticado, el render redirige a `from` o a la pantalla inicial del rol.
      await login(values.email, values.password);
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 401) {
        setFormError(INVALID_CREDENTIALS);
        return;
      }
      const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, [
        'email',
        'password',
      ]);
      setFormError(unmapped[0]?.message ?? GENERIC_ERROR);
    }
  };

  return (
    <PublicPageLayout>
      <Paper w="100%" p="xl" shadow="sm">
        <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
          <Stack>
            <div>
              <Title order={2}>Iniciar sesión</Title>
              <Text size="sm" c="dimmed">
                Panel de administración
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
            <PasswordInput
              label="Contraseña"
              autoComplete="current-password"
              error={errors.password?.message}
              {...register('password')}
            />
            <Button type="submit" fullWidth loading={isSubmitting}>
              Ingresar
            </Button>
            <Anchor component={Link} to={paths.forgotPassword} size="sm" ta="center">
              ¿Olvidaste tu contraseña?
            </Anchor>
          </Stack>
        </form>
      </Paper>
    </PublicPageLayout>
  );
}
