import { createBrowserRouter, Navigate } from 'react-router-dom';
import LoginPage from './auth/LoginPage';
import { ProtectedRoute } from './auth/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';
import { lazy, Suspense } from 'react';
import { Spin } from 'antd';

const DashboardPage = lazy(() => import('./modules/dashboard/DashboardPage'));
const EmployeeListPage = lazy(() => import('./modules/employees/EmployeeListPage'));
const OrganizationPage = lazy(() => import('./modules/organization/OrganizationPage'));
const AttendancePage = lazy(() => import('./modules/attendance/AttendancePage'));
const LeavePage = lazy(() => import('./modules/leave/LeavePage'));
const AccessControlPage = lazy(() => import('./modules/access-control/AccessControlPage'));

const Loading = () => (
  <div style={{ display: 'flex', height: '60vh', alignItems: 'center', justifyContent: 'center' }}>
    <Spin size="large" />
  </div>
);

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      {
        path: 'dashboard',
        element: (
          <Suspense fallback={<Loading />}>
            <DashboardPage />
          </Suspense>
        ),
      },
      {
        path: 'employees',
        element: (
          <Suspense fallback={<Loading />}>
            <EmployeeListPage />
          </Suspense>
        ),
      },
      {
        path: 'organization',
        element: (
          <Suspense fallback={<Loading />}>
            <OrganizationPage />
          </Suspense>
        ),
      },
      {
        path: 'attendance',
        element: (
          <Suspense fallback={<Loading />}>
            <AttendancePage />
          </Suspense>
        ),
      },
      {
        path: 'leaves',
        element: (
          <Suspense fallback={<Loading />}>
            <LeavePage />
          </Suspense>
        ),
      },
      {
        path: 'access-control',
        element: (
          <Suspense fallback={<Loading />}>
            <AccessControlPage />
          </Suspense>
        ),
      },
    ],
  },
  { path: '/unauthorized', element: <div style={{ textAlign: 'center', padding: '4rem', color: '#f8fafc' }}>403 — Access Denied</div> },
  { path: '*', element: <div style={{ textAlign: 'center', padding: '4rem', color: '#f8fafc' }}>404 — Page Not Found</div> },
]);
