import { useEffect, useRef, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import AuthLayout from '../auth/AuthLayout';
import Spinner from '../../components/Spinner';
import Banner from '../../components/Banner';
import Button from '../../components/Button';
import { confirmPayphonePayment } from '../../api/rentalsApi';
import { extractErrorMessage } from '../../api/client';
import { ROUTES } from '../../routes';

export default function PaymentResult() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const id = searchParams.get('id');
  const clientTransactionId = searchParams.get('clientTransactionId');
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const requestedRef = useRef(false);

  useEffect(() => {
    if (!id || !clientTransactionId) {
      setLoading(false);
      return;
    }
    // El confirm no es idempotente (la segunda llamada encuentra el pre-alquiler
    // ya COMPLETED y falla); StrictMode invoca los efectos dos veces en
    // desarrollo, así que evitamos la segunda llamada.
    if (requestedRef.current) return;
    requestedRef.current = true;
    confirmPayphonePayment(id, clientTransactionId)
      .then(setData)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [id, clientTransactionId]);

  const success = data?.success;

  return (
    <AuthLayout
      title={loading ? 'Confirmando pago...' : success ? '¡Pago exitoso!' : 'Pago no confirmado'}
      icon={success ? 'fa-circle-check' : loading ? 'fa-credit-card' : 'fa-circle-exclamation'}
    >
      {loading && <Spinner label="Confirmando tu pago con Payphone..." />}
      {!loading && (!id || !clientTransactionId) && (
        <Banner type="error">El enlace no es válido: faltan los datos de la transacción.</Banner>
      )}
      {!loading && error && <Banner type="error">{error}</Banner>}
      {!loading && !error && data && <Banner type={success ? 'success' : 'error'}>{data.message}</Banner>}
      {!loading && (
        <Button className="btn--full" onClick={() => navigate(ROUTES.LOCKERS)}>
          Volver a Casilleros
        </Button>
      )}
    </AuthLayout>
  );
}
