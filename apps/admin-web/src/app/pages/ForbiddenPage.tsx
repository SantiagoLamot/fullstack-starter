import { Button, Center, Group, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';

import { paths } from '@/routes';
import { useAuth } from '@/shared/lib/auth';

export function ForbiddenPage() {
  const { logout } = useAuth();
  return (
    <Center mih="60vh">
      <Stack align="center" gap="sm">
        <Title order={2}>Sin permisos</Title>
        <Text c="dimmed">Tu usuario no tiene acceso a esta sección.</Text>
        <Group>
          <Button component={Link} to={paths.dashboard} variant="light">
            Volver al inicio
          </Button>
          <Button
            variant="subtle"
            onClick={() => {
              void logout();
            }}
          >
            Cerrar sesión
          </Button>
        </Group>
      </Stack>
    </Center>
  );
}
