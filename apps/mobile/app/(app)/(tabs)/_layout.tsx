import { Tabs } from 'expo-router';

import { Icon, type IconName } from '@/shared/ui';
import { colors, fonts, typography } from '@/theme';

function tabIcon(name: IconName) {
  return ({ focused }: { focused: boolean }) => (
    <Icon name={name} color={focused ? 'textPrimary' : 'textSecondary'} />
  );
}

/** Tabs base: Inicio y Cuenta. Cada feature nueva agrega su `Tabs.Screen` y su archivo en esta carpeta. */
export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.textPrimary,
        tabBarInactiveTintColor: colors.textSecondary,
        tabBarLabelStyle: { fontFamily: fonts.body, fontSize: typography.caption.fontSize },
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{ title: 'Inicio', tabBarIcon: tabIcon('home-outline') }}
      />
      <Tabs.Screen
        name="cuenta"
        options={{ title: 'Cuenta', tabBarIcon: tabIcon('person-outline') }}
      />
    </Tabs>
  );
}
