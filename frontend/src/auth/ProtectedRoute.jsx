import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { ROUTES } from '../routes';

export default function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Outlet /> : <Navigate to={ROUTES.LOGIN} replace />;
}
