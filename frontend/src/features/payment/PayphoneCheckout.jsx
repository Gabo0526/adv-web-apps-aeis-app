import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthLayout from '../auth/AuthLayout';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import Spinner from '../../components/Spinner';
import { ROUTES } from '../../routes';
import './PayphoneCheckout.css';

const PAYPHONE_SCRIPT_URL = 'https://cdn.payphonetodoesposible.com/box/v1.1/payphone-payment-box.js';
const PAYPHONE_CSS_URL = 'https://cdn.payphonetodoesposible.com/box/v1.1/payphone-payment-box.css';

let payphoneLoadPromise = null;

function loadPayphoneAssets() {
  if (payphoneLoadPromise) return payphoneLoadPromise;

  if (!document.querySelector(`link[href="${PAYPHONE_CSS_URL}"]`)) {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = PAYPHONE_CSS_URL;
    document.head.appendChild(link);
  }

  payphoneLoadPromise = new Promise((resolve, reject) => {
    if (window.PPaymentButtonBox) {
      resolve();
      return;
    }
    const existing = document.querySelector(`script[src="${PAYPHONE_SCRIPT_URL}"]`);
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', () => reject(new Error('No se pudo cargar la Cajita de Pagos de Payphone.')));
      return;
    }
    const script = document.createElement('script');
    script.type = 'module';
    script.src = PAYPHONE_SCRIPT_URL;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('No se pudo cargar la Cajita de Pagos de Payphone.'));
    document.head.appendChild(script);
  });

  return payphoneLoadPromise;
}

export default function PayphoneCheckout() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [ready, setReady] = useState(false);
  const rendered = useRef(false);

  useEffect(() => {
    if (!state) return;
    loadPayphoneAssets()
      .then(() => setReady(true))
      .catch((err) => setError(err.message));
  }, [state]);

  useEffect(() => {
    if (!ready || !state || rendered.current) return;
    rendered.current = true;
    try {
      new window.PPaymentButtonBox({
        token: state.payphoneToken,
        clientTransactionId: state.clientTransactionId,
        amount: String(state.amountCents),
        amountWithoutTax: String(state.amountCents),
        currency: 'USD',
        storeId: state.payphoneStoreId,
        reference: state.reference,
      }).render('pp-button');
    } catch {
      setError('No se pudo inicializar la Cajita de Pagos de Payphone.');
    }
  }, [ready, state]);

  if (!state) {
    return (
      <AuthLayout title="Pago" icon="fa-credit-card" subtitle="No hay una renta pendiente de pago">
        <Banner type="error">No se encontró información de la renta. Vuelve a intentarlo desde Casilleros.</Banner>
        <Button className="btn--full" onClick={() => navigate(ROUTES.LOCKERS)}>
          Volver a Casilleros
        </Button>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Procesar pago" icon="fa-credit-card" subtitle={state.reference} maxWidth={550}>
      <Banner type="error">{error}</Banner>
      <div className="payphone-box-wrapper">
        {!ready && !error && <Spinner label="Cargando la Cajita de Pagos..." />}
        <div id="pp-button" />
      </div>
      <Button variant="danger" className="btn--full" onClick={() => navigate(ROUTES.LOCKERS)}>
        <i className="fa-solid fa-xmark" /> Cancelar
      </Button>
    </AuthLayout>
  );
}
