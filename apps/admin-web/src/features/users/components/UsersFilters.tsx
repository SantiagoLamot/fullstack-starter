import { Group, Select, TextInput } from '@mantine/core';

import { ROLE_LABELS, STATUS_LABELS, type UserRole, type UserStatus } from '../types';

interface UsersFiltersProps {
  q: string;
  role: UserRole | null;
  status: UserStatus | null;
  onQChange: (value: string) => void;
  onRoleChange: (value: UserRole | null) => void;
  onStatusChange: (value: UserStatus | null) => void;
}

const ROLE_OPTIONS = (Object.keys(ROLE_LABELS) as UserRole[]).map((value) => ({
  value,
  label: ROLE_LABELS[value],
}));
const STATUS_OPTIONS = (Object.keys(STATUS_LABELS) as UserStatus[]).map((value) => ({
  value,
  label: STATUS_LABELS[value],
}));

export function UsersFilters({
  q,
  role,
  status,
  onQChange,
  onRoleChange,
  onStatusChange,
}: UsersFiltersProps) {
  return (
    <Group align="flex-end">
      <TextInput
        label="Buscar"
        placeholder="Nombre o email"
        value={q}
        onChange={(event) => {
          onQChange(event.currentTarget.value);
        }}
      />
      <Select
        label="Rol"
        placeholder="Todos"
        clearable
        data={ROLE_OPTIONS}
        value={role}
        onChange={(value) => {
          onRoleChange(value);
        }}
      />
      <Select
        label="Estado"
        placeholder="Todos"
        clearable
        data={STATUS_OPTIONS}
        value={status}
        onChange={(value) => {
          onStatusChange(value);
        }}
      />
    </Group>
  );
}
