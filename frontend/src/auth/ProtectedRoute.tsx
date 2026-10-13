import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import type { Role } from '../types';

interface Props {
  children: React.ReactNode;
  roles?: Role[];
}

export function ProtectedRoute({ children, roles }: Props) {
  const user = useAuthStore((s) => s.user);
  const location = useLocation();

  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  if (roles && !roles.some((r) => user.roles.includes(r))) return <Navigate to="/unauthorized" replace />;
  return <>{children}</>;
}
