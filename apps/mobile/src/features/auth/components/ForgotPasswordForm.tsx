import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { Button, Text, TextField } from '@/shared/ui';
import { spacing } from '@/theme';

import { authApi } from '../api/auth.api';
import {
  type ForgotPasswordFormValues,
  forgotPasswordSchema,
} from '../schemas/forgot-password.schema';
import {
  isNetworkError,
  isTooManyRequests,
  NETWORK_ERROR_MESSAGE,
  TOO_MANY_REQUESTS_MESSAGE,
} from '../utils/password-errors';

interface ForgotPasswordFormProps {
  onBack: () => void;
}

/** Pide el link de restablecimiento. El mensaje de éxito es siempre el mismo (anti-enumeración). */
export function ForgotPasswordForm({ onBack }: ForgotPasswordFormProps) {
  const [sent, setSent] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  const onSubmit = async ({ email }: ForgotPasswordFormValues) => {
    setFormError(null);
    try {
      await authApi.requestPasswordReset(email);
      setSent(true);
    } catch (error) {
      if (isTooManyRequests(error)) setFormError(TOO_MANY_REQUESTS_MESSAGE);
      else if (isNetworkError(error)) setFormError(NETWORK_ERROR_MESSAGE);
      else setFormError('Ocurrió un error inesperado. Intentá de nuevo.');
    }
  };

  if (sent) {
    return (
      <View>
        <Text variant="body" color="success" style={styles.message}>
          Si el email está registrado y activo, te enviamos un link para restablecer tu contraseña.
          Revisá tu correo (y la carpeta de spam). El link vence en 1 hora.
        </Text>
        <Text variant="bodySm" color="textSecondary" style={styles.message}>
          Abrí el link desde este teléfono: la contraseña se elige en una página web y después
          ingresás acá con la nueva.
        </Text>
        <Button label="Volver a iniciar sesión" onPress={onBack} />
      </View>
    );
  }

  return (
    <View>
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
      {formError ? (
        <Text variant="bodySm" color="danger" style={styles.message}>
          {formError}
        </Text>
      ) : null}
      <Button
        label="Enviar link"
        onPress={() => void handleSubmit(onSubmit)()}
        loading={isSubmitting}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  message: { marginBottom: spacing.sm },
});
