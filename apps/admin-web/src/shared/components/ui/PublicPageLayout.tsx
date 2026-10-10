import { Box, Center, Title } from '@mantine/core';
import type { ReactNode } from 'react';

import { env } from '@/shared/config/env';

interface PublicPageLayoutProps {
  children: ReactNode;
}

/**
 * Marco de las pantallas públicas (login, activación, restablecer): nombre del producto centrado.
 * Para aplicar la marca: reemplazar el título por el logo (docs/brand/brand.md).
 */
export function PublicPageLayout({ children }: PublicPageLayoutProps) {
  return (
    <Center mih="100vh" p="md">
      <Box w="100%" maw={420}>
        <Title order={1} ta="center" size="h2" mb="xl">
          {env.appName}
        </Title>
        {children}
      </Box>
    </Center>
  );
}
