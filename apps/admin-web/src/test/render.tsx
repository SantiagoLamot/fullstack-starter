import { MantineProvider } from '@mantine/core';
import { render } from '@testing-library/react';
import type { ReactElement } from 'react';

import { theme } from '@/theme/theme';

/** render() con los providers de UI necesarios para componentes Mantine. */
export function renderWithProviders(ui: ReactElement) {
  return render(<MantineProvider theme={theme}>{ui}</MantineProvider>);
}
