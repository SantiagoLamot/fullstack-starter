import { Alert, Button, Group, Loader, Pagination, Stack, Title } from '@mantine/core';
import { useDebouncedValue, useDisclosure } from '@mantine/hooks';
import { useState } from 'react';

import { CreateUserModal } from '../components/CreateUserModal';
import { UserDrawer } from '../components/UserDrawer';
import { UsersFilters } from '../components/UsersFilters';
import { UsersTable } from '../components/UsersTable';
import { useUsers } from '../hooks/useUsers';
import type { UserResponse, UserRole, UserStatus } from '../types';

const PAGE_SIZE = 20;

export function UsersPage() {
  const [q, setQ] = useState('');
  const [role, setRole] = useState<UserRole | null>(null);
  const [status, setStatus] = useState<UserStatus | null>(null);
  const [page, setPage] = useState(0);
  const [debouncedQ] = useDebouncedValue(q.trim(), 300);
  const [modalOpened, modal] = useDisclosure(false);
  const [selected, setSelected] = useState<UserResponse | null>(null);

  const { data, isPending, isError, isFetching } = useUsers({
    q: debouncedQ || undefined,
    role: role ?? undefined,
    status: status ?? undefined,
    page,
    size: PAGE_SIZE,
  });

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Usuarios</Title>
        <Button onClick={modal.open}>Nuevo usuario</Button>
      </Group>

      <UsersFilters
        q={q}
        role={role}
        status={status}
        onQChange={(value) => {
          setQ(value);
          setPage(0);
        }}
        onRoleChange={(value) => {
          setRole(value);
          setPage(0);
        }}
        onStatusChange={(value) => {
          setStatus(value);
          setPage(0);
        }}
      />

      {isError && <Alert color="danger">No se pudo cargar el listado de usuarios.</Alert>}
      {isPending ? (
        <Loader aria-label="Cargando usuarios" />
      ) : (
        <UsersTable users={data?.content ?? []} onSelect={setSelected} />
      )}

      <Group justify="space-between">
        <span>{isFetching ? 'Actualizando...' : `${data?.totalElements ?? 0} usuarios`}</span>
        {(data?.totalPages ?? 0) > 1 && (
          <Pagination
            total={data?.totalPages ?? 1}
            value={page + 1}
            onChange={(value) => {
              setPage(value - 1);
            }}
          />
        )}
      </Group>

      <CreateUserModal opened={modalOpened} onClose={modal.close} />
      <UserDrawer
        user={selected}
        onClose={() => {
          setSelected(null);
        }}
      />
    </Stack>
  );
}
