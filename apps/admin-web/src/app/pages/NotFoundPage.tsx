import { Button, Center, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';

import { paths } from '@/routes';

export function NotFoundPage() {
  return (
    <Center mih="100vh">
      <Stack align="center" gap="sm">
        <Title order={1}>404</Title>
        <Text c="dimmed">La página que buscás no existe.</Text>
        <Button component={Link} to={paths.dashboard} variant="light">
          Ir al inicio
        </Button>
      </Stack>
    </Center>
  );
}
