import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Group, Modal, Select, Stack, TextInput } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { isAxiosError } from 'axios';
import { Controller, useForm } from 'react-hook-form';

import { applyProblemFieldErrors, getProblemDetails } from '@/shared/lib/problem-details';

import { useCreateUser } from '../hooks/useUsers';
import { type CreateUserFormValues, createUserSchema } from '../schemas/create-user.schema';
import { ROLE_LABELS, type UserRole } from '../types';

interface CreateUserModalProps {
  opened: boolean;
  onClose: () => void;
}

const ROLE_OPTIONS = (Object.keys(ROLE_LABELS) as UserRole[]).map((value) => ({
  value,
  label: ROLE_LABELS[value],
}));

const EMPTY: CreateUserFormValues = {
  email: '',
  firstName: '',
  lastName: '',
  phone: '',
  role: 'USER',
};

export function CreateUserModal({ opened, onClose }: CreateUserModalProps) {
  const createUser = useCreateUser();
  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<CreateUserFormValues>({
    resolver: zodResolver(createUserSchema),
    defaultValues: EMPTY,
  });

  const close = () => {
    reset(EMPTY);
    onClose();
  };

  const onSubmit = async (values: CreateUserFormValues) => {
    try {
      const created = await createUser.mutateAsync({
        ...values,
        phone: values.phone ? values.phone : undefined,
      });
      notifications.show({
        color: 'success',
        title: 'Usuario creado',
        message: `Se envió la invitación a ${created.email ?? values.email}.`,
      });
      close();
    } catch (error) {
      const unmapped = applyProblemFieldErrors(getProblemDetails(error), setError, [
        'email',
        'firstName',
        'lastName',
        'phone',
        'role',
      ]);
      const conflict = isAxiosError(error) && error.response?.status === 409;
      notifications.show({
        color: 'danger',
        title: 'No se pudo crear el usuario',
        message:
          unmapped[0]?.message ??
          (conflict
            ? 'Ya existe un usuario con ese email.'
            : getProblemDetails(error)?.errors?.length
              ? 'Revisá los campos marcados.'
              : 'Ocurrió un error. Intentá de nuevo.'),
      });
    }
  };

  return (
    <Modal opened={opened} onClose={close} title="Nuevo usuario">
      <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
        <Stack>
          <TextInput
            label="Email"
            type="email"
            required
            error={errors.email?.message}
            {...register('email')}
          />
          <TextInput
            label="Nombre"
            required
            error={errors.firstName?.message}
            {...register('firstName')}
          />
          <TextInput
            label="Apellido"
            required
            error={errors.lastName?.message}
            {...register('lastName')}
          />
          <TextInput
            label="Teléfono"
            description="Solo dígitos, formato internacional, p. ej. 5491122334455"
            error={errors.phone?.message}
            {...register('phone')}
          />
          <Controller
            control={control}
            name="role"
            render={({ field }) => (
              <Select
                label="Rol"
                required
                allowDeselect={false}
                data={ROLE_OPTIONS}
                error={errors.role?.message}
                value={field.value}
                onChange={(value) => {
                  field.onChange(value);
                }}
              />
            )}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={close}>
              Cancelar
            </Button>
            <Button type="submit" loading={createUser.isPending}>
              Crear e invitar
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
