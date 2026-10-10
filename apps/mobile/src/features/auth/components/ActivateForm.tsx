import { zodResolver } from '@hookform/resolvers/zod';
import { isAxiosError } from 'axios';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';
import { Button, Text, TextField } from '@/shared/ui';
import { spacing } from '@/theme';

import { authApi } from '../api/auth.api';
import {
  type ActivateFormValues,
  activateSchema,
  PASSWORD_MAX,
  PASSWORD_MIN,
} from '../schemas/activate.schema';
import { extractInvitationToken } from '../utils/invitation-token';

interface ActivateFormProps {
  /** Token recibido por deep link (`/activar?token=...`); si falta, se pide pegarlo. */
  initialToken?: string;
  onActivated: () => void;
}

export function ActivateForm({ initialToken, onActivated }: ActivateFormProps) {
  const [formError, setFormError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ActivateFormValues>({
    resolver: zodResolver(activateSchema),
    defaultValues: { token: initialToken ?? '', password: '', confirmPassword: '' },
  });

  const onSubmit = async (values: ActivateFormValues) => {
    setFormError(null);
    try {
      await authApi.acceptInvitation({
        token: extractInvitationToken(values.token),
        password: values.password,
      });
      onActivated();
    } catch (error) {
      const status = isAxiosError(error) ? error.response?.status : undefined;
      if (status === 400) {
        const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, [
          'token',
          'password',
        ]);
        if (unmapped.length > 0) {
          setFormError(
            'La invitación no es válida o ya venció. Pedile al administrador que te la reenvíe.',
          );
        }
      } else if (isAxiosError(error) && !error.response) {
        setFormError('No pudimos conectar con el servidor. Revisá tu conexión.');
      } else {
        setFormError(
          'No pudimos activar la cuenta. La invitación puede haber vencido o ya fue usada.',
        );
      }
    }
  };

  return (
    <View>
      {initialToken ? null : (
        <Controller
          control={control}
          name="token"
          render={({ field: { onChange, onBlur, value } }) => (
            <TextField
              label="Token o link de la invitación"
              autoCapitalize="none"
              autoCorrect={false}
              value={value}
              onChangeText={onChange}
              onBlur={onBlur}
              error={errors.token?.message}
            />
          )}
        />
      )}
      <Controller
        control={control}
        name="password"
        render={({ field: { onChange, onBlur, value } }) => (
          <TextField
            label="Nueva contraseña"
            autoComplete="new-password"
            secureTextEntry
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.password?.message}
          />
        )}
      />
      <Controller
        control={control}
        name="confirmPassword"
        render={({ field: { onChange, onBlur, value } }) => (
          <TextField
            label="Repetir contraseña"
            autoComplete="new-password"
            secureTextEntry
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.confirmPassword?.message}
          />
        )}
      />
      <Text variant="bodySm" color="textSecondary" style={styles.hint}>
        {`Entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres, con al menos una letra y un número.`}
      </Text>
      {formError ? (
        <Text variant="bodySm" color="danger" style={styles.hint}>
          {formError}
        </Text>
      ) : null}
      <Button
        label="Activar cuenta"
        onPress={() => void handleSubmit(onSubmit)()}
        loading={isSubmitting}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  hint: {
    marginBottom: spacing.sm,
  },
});
