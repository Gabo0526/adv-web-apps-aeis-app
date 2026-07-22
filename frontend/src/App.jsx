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
import LockerGrid from './features/lockers/LockerGrid';
import PayphoneCheckout from './features/payment/PayphoneCheckout';
import PaymentResult from './features/payment/PaymentResult';
import MyRentals from './features/rentals/MyRentals';
import ComingSoon from './components/ComingSoon';
import Periods from './features/admin/Periods';
import CreateBlock from './features/admin/CreateBlock';
import Rentals from './features/admin/Rentals';
import ExceptionalRental from './features/admin/ExceptionalRental';
import Users from './features/admin/Users';

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
          <Route path={ROUTES.PAYMENT_RESULT} element={<PaymentResult />} />

          <Route element={<ProtectedRoute />}>
            <Route path={ROUTES.HOME} element={<Home />} />
            <Route path={ROUTES.LOCKERS} element={<LockerGrid />} />
            <Route path={ROUTES.PAYMENT_CHECKOUT} element={<PayphoneCheckout />} />
            <Route path={ROUTES.MY_RENTALS} element={<MyRentals />} />
            <Route path={ROUTES.HELP} element={<ComingSoon title="Ayuda" />} />
          </Route>

          <Route element={<AdminRoute />}>
            <Route path={ROUTES.ADMIN_PERIODS} element={<Periods />} />
            <Route path={ROUTES.ADMIN_BLOCKS_NEW} element={<CreateBlock />} />
            <Route path={ROUTES.ADMIN_RENTALS} element={<Rentals />} />
            <Route path={ROUTES.ADMIN_RENTALS_EXCEPTIONAL} element={<ExceptionalRental />} />
            <Route path={ROUTES.ADMIN_USERS} element={<Users />} />
          </Route>

          <Route path="*" element={<Navigate to={ROUTES.HOME} replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
