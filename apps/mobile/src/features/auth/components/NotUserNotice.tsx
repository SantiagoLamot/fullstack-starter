import { StyleSheet, View } from 'react-native';

import { Button, Screen, Text } from '@/shared/ui';
import { spacing } from '@/theme';

import { logout } from '../api/session.service';

/** Sesión válida pero con un rol distinto de USER: la app móvil es solo para usuarios. */
export function NotUserNotice() {
  return (
    <Screen>
      <View style={styles.content}>
        <Text variant="h1">Solo para usuarios</Text>
        <Text variant="body" color="textSecondary" style={styles.body}>
          Esta app es para usuarios. Ingresá con una cuenta de usuario o usá el panel de
          administración.
        </Text>
        <Button label="Cerrar sesión" onPress={() => void logout()} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  content: {
    flex: 1,
    justifyContent: 'center',
  },
  body: {
    marginVertical: spacing.md,
  },
});
