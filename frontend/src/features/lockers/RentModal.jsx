import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../../components/Modal';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import { useActivePeriod } from './useActivePeriod';
import { createPreRental } from '../../api/rentalsApi';
import { extractErrorMessage } from '../../api/client';
import { validateCustomRentalRange } from '../../utils/validators';
import { RENTAL_SETTINGS } from '../../utils/constants';
import { ROUTES } from '../../routes';
import './RentModal.css';

function todayDateStr() {
  return new Date().toLocaleDateString('en-CA');
}

function calculateBilledDays(endDateStr) {
  const start = new Date(`${todayDateStr()}T00:00:00`);
  const end = new Date(`${endDateStr}T00:00:00`);
  const diff = Math.round((end - start) / (1000 * 60 * 60 * 24));
  return diff + 1;
}

export default function RentModal({ locker, onClose, onRented }) {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const { data: activePeriod } = useActivePeriod();
  const isCustom = !!locker?.allowCustomRental;

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: { endDate: '' },
    validate: (vals) =>
      isCustom
        ? { endDate: validateCustomRentalRange(todayDateStr(), vals.endDate, RENTAL_SETTINGS.MAX_RENT_DAYS) }
        : {},
    onSubmit: async (vals) => {
      setServerError('');
      try {
        const payload = { lockerId: locker.id };
        if (isCustom) {
          payload.startDate = `${todayDateStr()}T00:00:00`;
          payload.endDate = `${vals.endDate}T00:00:00`;
        }
        const preRental = await createPreRental(payload);
        onRented?.();
        navigate(ROUTES.PAYMENT_CHECKOUT, { state: preRental });
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  if (!locker) return null;

  const billedDays = isCustom && values.endDate && !errors.endDate ? calculateBilledDays(values.endDate) : null;
  const customPrice = billedDays ? (billedDays * RENTAL_SETTINGS.CUSTOM_RENT_DAILY_PRICE).toFixed(2) : null;
  const fixedPrice = RENTAL_SETTINGS.PERIOD_RENT_PRICE.toFixed(2);

  return (
    <Modal
      open={!!locker}
      title={`Rentar casillero #${locker.number} — ${locker.blockName}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} loading={submitting}>
            Confirmar renta
          </Button>
        </>
      }
    >
      <Banner type="error">{serverError}</Banner>

      {isCustom ? (
        <form onSubmit={handleSubmit} noValidate>
          <p className="rent-modal__hint">
            Este bloque permite renta personalizada: inicia hoy ({todayDateStr()}) y puede durar hasta{' '}
            {RENTAL_SETTINGS.MAX_RENT_DAYS} días.
          </p>
          <FormField
            label="Fecha de fin"
            name="endDate"
            type="date"
            min={todayDateStr()}
            value={values.endDate}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.endDate}
            touched={touched.endDate}
          />
          {customPrice && (
            <p className="rent-modal__price">
              {billedDays} día{billedDays === 1 ? '' : 's'} × ${RENTAL_SETTINGS.CUSTOM_RENT_DAILY_PRICE.toFixed(2)} ={' '}
              <strong>${customPrice}</strong>
            </p>
          )}
        </form>
      ) : (
        <div className="rent-modal__fixed">
          <p>
            Renta por el período completo
            {activePeriod ? ` "${activePeriod.name}"` : ''}
            {activePeriod?.endDate ? `, hasta ${activePeriod.endDate.slice(0, 10)}` : ''}.
          </p>
          <p className="rent-modal__price">
            Precio: <strong>${fixedPrice}</strong>
          </p>
        </div>
      )}
    </Modal>
  );
}
