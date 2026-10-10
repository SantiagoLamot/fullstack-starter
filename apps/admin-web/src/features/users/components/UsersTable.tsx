import { Table, Text } from '@mantine/core';

import { ROLE_LABELS, type UserResponse } from '../types';
import { UserStatusBadges } from './UserStatusBadges';

interface UsersTableProps {
  users: readonly UserResponse[];
  onSelect: (user: UserResponse) => void;
}

function formatDate(iso?: string): string {
  return iso ? new Date(iso).toLocaleDateString('es-AR') : '-';
}

export function UsersTable({ users, onSelect }: UsersTableProps) {
  if (users.length === 0) {
    return <Text c="dimmed">No hay usuarios que coincidan con los filtros.</Text>;
  }
  return (
    <Table.ScrollContainer minWidth={640}>
      <Table highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Nombre</Table.Th>
            <Table.Th>Email</Table.Th>
            <Table.Th>Teléfono</Table.Th>
            <Table.Th>Rol</Table.Th>
            <Table.Th>Estado</Table.Th>
            <Table.Th>Alta</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {users.map((user) => (
            <Table.Tr
              key={user.id}
              style={{ cursor: 'pointer' }}
              onClick={() => {
                onSelect(user);
              }}
            >
              <Table.Td>{`${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()}</Table.Td>
              <Table.Td>{user.email}</Table.Td>
              <Table.Td>{user.phone ?? '-'}</Table.Td>
              <Table.Td>{user.role ? ROLE_LABELS[user.role] : '-'}</Table.Td>
              <Table.Td>
                <UserStatusBadges user={user} />
              </Table.Td>
              <Table.Td>{formatDate(user.createdAt)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
