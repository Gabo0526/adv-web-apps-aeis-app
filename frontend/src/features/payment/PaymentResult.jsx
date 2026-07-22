import { useSearchParams, useNavigate } from 'react-router-dom';
import AuthLayout from '../auth/AuthLayout';
import Spinner from '../../components/Spinner';
import Banner from '../../components/Banner';
import Button from '../../components/Button';
import { useFetch } from '../../hooks/useFetch';
import { confirmPayphonePayment } from '../../api/rentalsApi';
import { ROUTES } from '../../routes';

export default function PaymentResult() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const id = searchParams.get('id');
  const clientTransactionId = searchParams.get('clientTransactionId');

  const { data, loading, error } = useFetch(
    () => confirmPayphonePayment(id, clientTransactionId),
    [id, clientTransactionId]
  );

  const success = data?.success;

  return (
    <AuthLayout
      title={loading ? 'Confirmando pago...' : success ? '¡Pago exitoso!' : 'Pago no confirmado'}
      icon={success ? 'fa-circle-check' : loading ? 'fa-credit-card' : 'fa-circle-exclamation'}
    >
      {loading && <Spinner label="Confirmando tu pago con Payphone..." />}
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
