import { Stack, Text, Title } from '@mantine/core';

export function DashboardPage() {
  return (
    <Stack gap="xs">
      <Title order={2}>Inicio</Title>
      <Text c="dimmed">Bienvenido al panel de administración.</Text>
    </Stack>
  );
}
