import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import AdminRoute from './auth/AdminRoute';
import { ROUTES } from './routes';

import Login from './features/auth/Login';
import Register from './features/auth/Register';
import ForgotPassword from './features/auth/ForgotPassword';
import ResetPassword from './features/auth/ResetPassword';
import Verify from './features/auth/Verify';
import Home from './features/home/Home';
import ComingSoon from './components/ComingSoon';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Navigate to={ROUTES.HOME} replace />} />
          <Route path={ROUTES.LOGIN} element={<Login />} />
          <Route path={ROUTES.REGISTER} element={<Register />} />
          <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
          <Route path={ROUTES.RESET_PASSWORD} element={<ResetPassword />} />
          <Route path={ROUTES.VERIFY} element={<Verify />} />

          <Route element={<ProtectedRoute />}>
            <Route path={ROUTES.HOME} element={<Home />} />
            <Route path={ROUTES.LOCKERS} element={<ComingSoon title="Casilleros" />} />
            <Route path={ROUTES.MY_RENTALS} element={<ComingSoon title="Mis Alquileres" />} />
            <Route path={ROUTES.HELP} element={<ComingSoon title="Ayuda" />} />
          </Route>

          <Route element={<AdminRoute />}>
            <Route path={ROUTES.ADMIN_PERIODS} element={<ComingSoon title="Períodos" />} />
            <Route path={ROUTES.ADMIN_RENTALS} element={<ComingSoon title="Rentas" />} />
            <Route path={ROUTES.ADMIN_USERS} element={<ComingSoon title="Usuarios" />} />
          </Route>

          <Route path="*" element={<Navigate to={ROUTES.HOME} replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
