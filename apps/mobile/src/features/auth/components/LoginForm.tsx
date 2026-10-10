import { zodResolver } from '@hookform/resolvers/zod';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';
import { Button, Text, TextField } from '@/shared/ui';
import { spacing } from '@/theme';

import { login } from '../api/session.service';
import { type LoginFormValues, loginSchema } from '../schemas/login.schema';

interface LoginFormProps {
  /** Aviso positivo a mostrar arriba (p. ej. tras activar la cuenta). */
  notice?: string;
}

export function LoginForm({ notice }: LoginFormProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setFormError(null);
    try {
      await login(values);
      // La navegación la resuelven los guards de los grupos al cambiar la sesión.
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 400) {
        const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, [
          'email',
          'password',
        ]);
        if (unmapped.length > 0) setFormError('Revisá los datos ingresados.');
      } else if (isAxiosError(error) && error.response?.status === 401) {
        setFormError('Email o contraseña incorrectos, o la cuenta está bloqueada temporalmente.');
      } else if (isAxiosError(error) && !error.response) {
        setFormError('No pudimos conectar con el servidor. Revisá tu conexión.');
      } else {
        setFormError('Ocurrió un error inesperado. Intentá de nuevo.');
      }
    }
  };

  return (
    <View>
      {notice ? (
        <Text variant="bodySm" color="success" style={styles.message}>
          {notice}
        </Text>
      ) : null}
      <Controller
        control={control}
        name="email"
        render={({ field: { onChange, onBlur, value } }) => (
          <TextField
            label="Email"
            autoCapitalize="none"
            autoComplete="email"
            keyboardType="email-address"
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.email?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="password"
        render={({ field: { onChange, onBlur, value } }) => (
          <TextField
            label="Contraseña"
            autoComplete="current-password"
            secureTextEntry
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.password?.message}
          />
        )}
      />
      {formError ? (
        <Text variant="bodySm" color="danger" style={styles.message}>
          {formError}
        </Text>
      ) : null}
      <View style={styles.spacer} />
      <Button
        label="Ingresar"
        onPress={() => void handleSubmit(onSubmit)()}
        loading={isSubmitting}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  spacer: {
    height: spacing.xs,
  },
  message: {
    marginBottom: spacing.sm,
  },
});
