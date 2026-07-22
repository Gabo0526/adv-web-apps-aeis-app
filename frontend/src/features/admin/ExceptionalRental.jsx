import { useState } from 'react';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import Spinner from '../../components/Spinner';
import { useForm } from '../../hooks/useForm';
import { useUserSearch } from './useUserSearch';
import { useLockerBlocks } from '../lockers/useLockerBlocks';
import { createExceptionalRental } from '../../api/rentalsApi';
import { extractErrorMessage } from '../../api/client';
import { validateCustomRentalRange, validateAmount } from '../../utils/validators';
import { RENTAL_SETTINGS } from '../../utils/constants';
import './ExceptionalRental.css';

function todayDateStr() {
  return new Date().toLocaleDateString('en-CA');
}

export default function ExceptionalRental() {
  const [idPrefix, setIdPrefix] = useState('');
  const { data: matchingUsers, loading: searching } = useUserSearch(idPrefix);
  const [selectedUser, setSelectedUser] = useState(null);

  const { data: blocks, loading: blocksLoading, error: blocksError, reload: reloadBlocks } = useLockerBlocks();
  const [selectedLocker, setSelectedLocker] = useState(null);

  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const isCustom = !!selectedLocker?.allowCustomRental;

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit, setValues } = useForm({
    initialValues: { endDate: '', amountPaid: '' },
    validate: (vals) => ({
      endDate: isCustom ? validateCustomRentalRange(todayDateStr(), vals.endDate, RENTAL_SETTINGS.MAX_RENT_DAYS) : null,
      amountPaid: validateAmount(vals.amountPaid),
    }),
    onSubmit: async (vals) => {
      setServerError('');
      setSuccessMessage('');
      try {
        const payload = {
          username: selectedUser.username,
          lockerId: selectedLocker.id,
          amountPaid: Number(vals.amountPaid),
        };
        if (isCustom) {
          payload.endDate = `${vals.endDate}T23:59:59`;
        }
        const rental = await createExceptionalRental(payload);
        setSuccessMessage(
          `Renta creada: casillero #${rental.lockerNumber} (${rental.blockName}) para ${rental.username}.`
        );
        setSelectedUser(null);
        setSelectedLocker(null);
        setIdPrefix('');
        setValues({ endDate: '', amountPaid: '' });
        reloadBlocks();
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  function handleLockerClick(locker, block) {
    if (locker.status !== 'AVAILABLE') return;
    setSelectedLocker({ ...locker, blockName: block.name, allowCustomRental: block.allowCustomRental });
  }

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Renta excepcional" subtitle="Registra manualmente la renta de un casillero para un usuario" />

        <Banner type="success">{successMessage}</Banner>
        <Banner type="error">{serverError}</Banner>

        <Card className="exceptional-rental__section">
          <h3>1. Buscar usuario</h3>
          {selectedUser ? (
            <div className="exceptional-rental__selected-user">
              <span>
                <strong>{selectedUser.name} {selectedUser.lastName}</strong> — @{selectedUser.username} ({selectedUser.id})
              </span>
              <Button variant="secondary" onClick={() => setSelectedUser(null)}>
                Cambiar
              </Button>
            </div>
          ) : (
            <>
              <FormField
                label="Cédula (mínimo 3 dígitos)"
                name="idPrefix"
                value={idPrefix}
                onChange={(e) => setIdPrefix(e.target.value)}
                placeholder="Ej. 1725"
              />
              {searching && <Spinner label="Buscando..." />}
              {!searching && idPrefix.trim().length >= 3 && (
                <ul className="exceptional-rental__results">
                  {(matchingUsers ?? []).length === 0 && <li className="exceptional-rental__empty">Sin resultados.</li>}
                  {(matchingUsers ?? []).map((u) => (
                    <li key={u.id}>
                      <button type="button" onClick={() => setSelectedUser(u)}>
                        {u.name} {u.lastName} — @{u.username} ({u.id})
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </Card>

        <Card className="exceptional-rental__section">
          <h3>2. Elegir casillero disponible</h3>
          {blocksLoading && <Spinner label="Cargando casilleros..." />}
          {blocksError && <Banner type="error">{blocksError}</Banner>}
          {selectedLocker && (
            <div className="exceptional-rental__selected-user">
              <span>
                Casillero <strong>#{selectedLocker.number}</strong> — {selectedLocker.blockName}
              </span>
              <Button variant="secondary" onClick={() => setSelectedLocker(null)}>
                Cambiar
              </Button>
            </div>
          )}
          {!blocksLoading && !blocksError && !selectedLocker && (
            <>
              {(blocks ?? []).map((block) => (
                <div key={block.id} className="exceptional-rental__block">
                  <h4>{block.name}</h4>
                  <div
                    className="exceptional-rental__grid"
                    style={{ gridTemplateColumns: `repeat(${block.blockColumns}, 1fr)` }}
                  >
                    {block.lockers.map((locker) => (
                      <button
                        key={locker.id}
                        type="button"
                        className={`exceptional-rental__cell exceptional-rental__cell--${locker.status.toLowerCase()}`}
                        disabled={locker.status !== 'AVAILABLE'}
                        onClick={() => handleLockerClick(locker, block)}
                        title={`Casillero ${locker.number}`}
                      >
                        {locker.number}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </>
          )}
        </Card>

        <Card className="exceptional-rental__section">
          <h3>3. Detalles de la renta</h3>
          <form onSubmit={handleSubmit} noValidate>
            {isCustom && (
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
            )}
            {selectedLocker && !isCustom && (
              <p className="exceptional-rental__hint">
                Este casillero renta por el período activo completo (sin fecha de fin personalizada).
              </p>
            )}
            <FormField
              label="Monto pagado ($)"
              name="amountPaid"
              type="number"
              step="0.01"
              min="0"
              value={values.amountPaid}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.amountPaid}
              touched={touched.amountPaid}
            />
            <Button type="submit" loading={submitting} disabled={!selectedUser || !selectedLocker}>
              Crear renta excepcional
            </Button>
          </form>
        </Card>
      </div>
    </>
  );
}
