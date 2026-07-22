import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import Spinner from '../../components/Spinner';
import Banner from '../../components/Banner';
import * as authApi from '../../api/authApi';
import { extractErrorMessage } from '../../api/client';
import { ROUTES } from '../../routes';

export default function Verify() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const requestedRef = useRef(false);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    // El endpoint consume el token (no es idempotente); StrictMode invoca los
    // efectos dos veces en desarrollo, así que evitamos la segunda llamada.
    if (requestedRef.current) return;
    requestedRef.current = true;
    authApi
      .verifyAccount(token)
      .then(setResult)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [token]);

  return (
    <AuthLayout title="Verificación de cuenta" icon="fa-envelope-circle-check">
      {loading && <Spinner label="Verificando tu cuenta..." />}
      {!loading && !token && <Banner type="error">El enlace no es válido: falta el token de verificación.</Banner>}
      {!loading && token && error && <Banner type="error">{error}</Banner>}
      {!loading && token && result && (
        <Banner type={result.verified ? 'success' : 'error'}>{result.message}</Banner>
      )}
      <div className="auth-links">
        <Link to={ROUTES.LOGIN}>Ir a iniciar sesión</Link>
      </div>
    </AuthLayout>
  );
}
