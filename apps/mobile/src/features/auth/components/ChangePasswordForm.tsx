import { zodResolver } from '@hookform/resolvers/zod';
import { isAxiosError } from 'axios';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ScrollView, StyleSheet, View } from 'react-native';

import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';
import { Button, Card, Screen, Text, TextField } from '@/shared/ui';
import { spacing } from '@/theme';

import { authApi } from '../api/auth.api';
import { PASSWORD_MAX, PASSWORD_MIN } from '../schemas/activate.schema';
import {
  type ChangePasswordFormValues,
  changePasswordSchema,
} from '../schemas/change-password.schema';
import {
  isNetworkError,
  isTooManyRequests,
  NETWORK_ERROR_MESSAGE,
  TOO_MANY_REQUESTS_MESSAGE,
} from '../utils/password-errors';

/** Cambio de contraseña desde "Mi cuenta". La sesión actual sigue válida (se cierran las otras). */
export function ChangePasswordForm() {
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  });

  const onSubmit = async ({ currentPassword, newPassword }: ChangePasswordFormValues) => {
    setFormError(null);
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      router.replace({ pathname: '/cuenta', params: { passwordChanged: '1' } });
    } catch (error) {
      const problem = getProblemDetails(error);
      const status = isAxiosError(error) ? error.response?.status : undefined;
      if (status === 400 || status === 422) {
        // 400 y 422 traen `errors[{field,message}]`; sin errores de campo, mensaje general.
        const hasFieldErrors = (problem?.errors?.length ?? 0) > 0;
        const unmapped = applyProblemFieldErrors(problem, setError, [
          'currentPassword',
          'newPassword',
        ]);
        if (!hasFieldErrors || unmapped.length > 0) {
          setFormError(
            problem?.detail ??
              (status === 400
                ? 'Revisá los datos ingresados.'
                : 'No pudimos cambiar la contraseña.'),
          );
        }
      } else if (isTooManyRequests(error)) {
        setFormError(TOO_MANY_REQUESTS_MESSAGE);
      } else if (isNetworkError(error)) {
        setFormError(NETWORK_ERROR_MESSAGE);
      } else {
        setFormError('Ocurrió un error inesperado. Intentá de nuevo.');
      }
    }
  };

  return (
    <Screen>
      <ScrollView keyboardShouldPersistTaps="handled" showsVerticalScrollIndicator={false}>
        <View style={styles.content}>
          <Text variant="label" color="textSecondary">
            (CUENTA)
          </Text>
          <Text variant="h1" style={styles.title}>
            Cambiar contraseña
          </Text>
          <Card>
            <Controller
              control={control}
              name="currentPassword"
              render={({ field: { onChange, onBlur, value } }) => (
                <TextField
                  label="Contraseña actual"
                  autoCapitalize="none"
                  autoComplete="current-password"
                  secureTextEntry
                  value={value}
                  onChangeText={onChange}
                  onBlur={onBlur}
                  error={errors.currentPassword?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="newPassword"
              render={({ field: { onChange, onBlur, value } }) => (
                <TextField
                  label="Contraseña nueva"
                  autoCapitalize="none"
                  autoComplete="new-password"
                  secureTextEntry
                  value={value}
                  onChangeText={onChange}
                  onBlur={onBlur}
                  error={errors.newPassword?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="confirmPassword"
              render={({ field: { onChange, onBlur, value } }) => (
                <TextField
                  label="Repetir contraseña nueva"
                  autoCapitalize="none"
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
              {`Entre ${PASSWORD_MIN} y ${PASSWORD_MAX} caracteres, con al menos una letra y un número. Al cambiarla, cerramos tus otras sesiones.`}
            </Text>
            {formError ? (
              <Text variant="bodySm" color="danger" style={styles.hint}>
                {formError}
              </Text>
            ) : null}
            <Button
              label="Guardar contraseña"
              loading={isSubmitting}
              onPress={() => void handleSubmit(onSubmit)()}
            />
          </Card>
          <View style={styles.cancel}>
            <Button
              variant="secondary"
              label="Cancelar"
              disabled={isSubmitting}
              onPress={() => router.back()}
            />
          </View>
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: { paddingTop: spacing.lg, paddingBottom: spacing.lg },
  title: { marginBottom: spacing.lg },
  hint: { marginBottom: spacing.sm },
  cancel: { marginTop: spacing.md },
});
