import { AppShell, Burger, Button, Group, Menu, NavLink, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconChevronDown, IconLogout, IconUser } from '@tabler/icons-react';
import { Link, NavLink as RouterNavLink, Outlet, useLocation } from 'react-router';

import { canAccess, homePath, type RouteKey, routes } from '@/routes';
import { env } from '@/shared/config/env';
import { useAuth } from '@/shared/lib/auth';

/** Entradas del menú lateral, en orden. Se filtran por rol. */
const NAV_ITEMS: readonly RouteKey[] = ['dashboard', 'users'];

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure();
  const { user, roles, logout } = useAuth();
  const { pathname } = useLocation();
  const items = NAV_ITEMS.filter((key) => canAccess(routes[key].access, roles));
  // Con una sola sección no hay menú lateral en escritorio: la sección y
  // "Mi cuenta" van en el header. En móvil, todos los roles abren el menú desde el burger.
  const showSidebar = items.length > 1;
  const drawerKeys: readonly RouteKey[] = showSidebar ? items : [...items, 'account'];
  const home = homePath(roles);

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{
        width: 240,
        breakpoint: 'sm',
        collapsed: { mobile: !opened, desktop: !showSidebar },
      }}
      padding={{ base: 'sm', sm: 'md' }}
    >
      <AppShell.Header>
        <Group h="100%" px={{ base: 'sm', sm: 'md' }} justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap" miw={0}>
            <Burger
              opened={opened}
              onClick={toggle}
              hiddenFrom="sm"
              size="sm"
              aria-label={opened ? 'Cerrar menú' : 'Abrir menú'}
            />
            <Link
              to={home}
              aria-label="Ir al inicio"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                flexShrink: 0,
                textDecoration: 'none',
              }}
            >
              <Text fw={700} size="lg" c="neutral.9" component="span">
                {env.appName}
              </Text>
              <Text
                size="xs"
                fw={700}
                tt="uppercase"
                lts="0.08em"
                c="dimmed"
                visibleFrom="xs"
                component="span"
              >
                {roles.includes('ADMIN') ? 'Admin' : ''}
              </Text>
            </Link>
            {!showSidebar && (
              <Group gap={4} ml="md" visibleFrom="sm" component="nav" aria-label="Secciones">
                {[...items, 'account' as const].map((key) => {
                  const active = pathname === routes[key].path;
                  return (
                    <Button
                      key={key}
                      component={RouterNavLink}
                      to={routes[key].path}
                      size="compact-md"
                      variant={active ? 'light' : 'subtle'}
                      color={active ? 'success' : 'neutral'}
                      aria-current={active ? 'page' : undefined}
                    >
                      {routes[key].label}
                    </Button>
                  );
                })}
              </Group>
            )}
          </Group>

          <Menu position="bottom-end" width={200} withinPortal>
            <Menu.Target>
              <Button
                variant="subtle"
                color="neutral"
                size="compact-md"
                leftSection={<IconUser size={16} stroke={1.5} />}
                rightSection={<IconChevronDown size={14} stroke={1.5} />}
                aria-label="Menú de usuario"
                style={{ flexShrink: 1, minWidth: 0 }}
              >
                <Text size="sm" truncate maw={{ base: 90, xs: 200 }} component="span">
                  {user?.fullName ?? 'Cuenta'}
                </Text>
              </Button>
            </Menu.Target>
            <Menu.Dropdown>
              <Menu.Item
                component={Link}
                to={routes.account.path}
                leftSection={<IconUser size={16} stroke={1.5} />}
              >
                Mi cuenta
              </Menu.Item>
              <Menu.Item
                leftSection={<IconLogout size={16} stroke={1.5} />}
                onClick={() => {
                  void logout();
                }}
              >
                Cerrar sesión
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs" aria-label="Navegación principal">
        {drawerKeys.map((key) => (
          <NavLink
            key={key}
            component={RouterNavLink}
            to={routes[key].path}
            end
            label={routes[key].label}
            onClick={close}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main miw={0}>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
