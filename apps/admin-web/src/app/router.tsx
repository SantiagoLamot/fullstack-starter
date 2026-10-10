import { createBrowserRouter, type RouteObject } from 'react-router';

import {
  AccountPage,
  ActivatePage,
  ForgotPasswordPage,
  LoginPage,
  ResetPasswordPage,
} from '@/features/auth';
import { UsersPage } from '@/features/users';
import { routes } from '@/routes';

import { HomeRoute } from './guards/HomeRoute';
import { RequireAuth } from './guards/RequireAuth';
import { RequireRole } from './guards/RequireRole';
import { AppLayout } from './layout/AppLayout';
import { NotFoundPage } from './pages/NotFoundPage';

export const routeObjects: RouteObject[] = [
  { path: routes.login.path, element: <LoginPage /> },
  { path: routes.activate.path, element: <ActivatePage /> },
  { path: routes.forgotPassword.path, element: <ForgotPasswordPage /> },
  { path: routes.resetPassword.path, element: <ResetPasswordPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: routes.dashboard.path, element: <HomeRoute /> },
          {
            element: <RequireRole access={routes.account.access} />,
            children: [{ path: routes.account.path, element: <AccountPage /> }],
          },
          {
            element: <RequireRole access={routes.users.access} />,
            children: [{ path: routes.users.path, element: <UsersPage /> }],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export const router = createBrowserRouter(routeObjects);
