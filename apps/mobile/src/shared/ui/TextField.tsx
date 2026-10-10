import { forwardRef } from 'react';
import { StyleSheet, TextInput, type TextInputProps, View } from 'react-native';

import { colors, radii, spacing, typography } from '@/theme';

import { Text } from './Text';

interface TextFieldProps extends TextInputProps {
  label: string;
  error?: string;
}

/** Campo de texto con label y error, para usar con react-hook-form (`Controller`). */
export const TextField = forwardRef<TextInput, TextFieldProps>(function TextField(
  { label, error, style, ...rest },
  ref,
) {
  return (
    <View style={styles.container}>
      <Text variant="label" color="textSecondary" style={styles.label}>
        {label}
      </Text>
      <TextInput
        ref={ref}
        style={[styles.input, Boolean(error) && styles.inputError, style]}
        placeholderTextColor={colors.textSecondary}
        selectionColor={colors.link}
        {...rest}
      />
      {error ? (
        <Text variant="caption" color="danger" style={styles.error}>
          {error}
        </Text>
      ) : null}
    </View>
  );
});

const styles = StyleSheet.create({
  container: {
    marginBottom: spacing.sm,
  },
  label: {
    marginBottom: spacing.xxs,
  },
  input: {
    height: 48,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.md,
    paddingHorizontal: spacing.sm,
    ...typography.body,
    color: colors.textPrimary,
    backgroundColor: colors.surface,
  },
  inputError: {
    borderColor: colors.danger,
  },
  error: {
    marginTop: spacing.xxs,
  },
});
