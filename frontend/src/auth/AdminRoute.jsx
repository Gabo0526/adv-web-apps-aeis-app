import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { ROUTES } from '../routes';

export default function AdminRoute() {
  const { isAuthenticated, isAdmin } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} replace />;
  }
  return isAdmin ? <Outlet /> : <Navigate to={ROUTES.HOME} replace />;
}
