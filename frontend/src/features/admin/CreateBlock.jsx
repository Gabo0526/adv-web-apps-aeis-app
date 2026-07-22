import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import Spinner from '../../components/Spinner';
import { useForm } from '../../hooks/useForm';
import { useActivePeriod } from '../lockers/useActivePeriod';
import { createLockerBlock } from '../../api/lockersApi';
import { extractErrorMessage } from '../../api/client';
import { required, validatePositiveInt, validateNonNegativeNumber } from '../../utils/validators';
import { ROUTES } from '../../routes';
import './CreateBlock.css';

const INITIAL_VALUES = {
  name: '',
  blockRows: '4',
  blockColumns: '5',
  allowCustomRental: false,
  lockerLength: '0.4',
  lockerWidth: '0.4',
  lockerHeight: '0.4',
};

function validate(values) {
  return {
    name: required(values.name, 'El nombre'),
    blockRows: validatePositiveInt(values.blockRows),
    blockColumns: validatePositiveInt(values.blockColumns),
    lockerLength: validateNonNegativeNumber(values.lockerLength),
    lockerWidth: validateNonNegativeNumber(values.lockerWidth),
    lockerHeight: validateNonNegativeNumber(values.lockerHeight),
  };
}

export default function CreateBlock() {
  const navigate = useNavigate();
  const { data: activePeriod, loading: periodLoading } = useActivePeriod();
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit, setValues } = useForm({
    initialValues: INITIAL_VALUES,
    validate,
    onSubmit: async (vals) => {
      setServerError('');
      setSuccessMessage('');
      try {
        const block = await createLockerBlock({
          name: vals.name,
          blockRows: Number(vals.blockRows),
          blockColumns: Number(vals.blockColumns),
          periodId: activePeriod.id,
          allowCustomRental: vals.allowCustomRental,
          lockerLength: Number(vals.lockerLength),
          lockerWidth: Number(vals.lockerWidth),
          lockerHeight: Number(vals.lockerHeight),
        });
        setSuccessMessage(`Bloque "${block.name}" creado con ${block.lockers.length} casilleros.`);
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  function handleCheckboxChange(e) {
    const { name, checked } = e.target;
    setValues((prev) => ({ ...prev, [name]: checked }));
  }

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Nuevo bloque de casilleros" subtitle="Crea un bloque y genera sus casilleros" />

        <Card>
          {periodLoading && <Spinner label="Buscando período activo..." />}

          {!periodLoading && !activePeriod && (
            <Banner type="error">No hay un período activo. Activa uno en Períodos antes de crear un bloque.</Banner>
          )}

          {!periodLoading && activePeriod && (
            <>
              <Banner type="success">{successMessage}</Banner>
              <Banner type="error">{serverError}</Banner>

              <p className="create-block__period">
                Período activo: <strong>{activePeriod.name}</strong>
              </p>

              <form onSubmit={handleSubmit} noValidate>
                <FormField
                  label="Nombre del bloque"
                  name="name"
                  value={values.name}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={errors.name}
                  touched={touched.name}
                />

                <div className="create-block__grid">
                  <FormField
                    label="Filas"
                    name="blockRows"
                    type="number"
                    min="1"
                    value={values.blockRows}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.blockRows}
                    touched={touched.blockRows}
                  />
                  <FormField
                    label="Columnas"
                    name="blockColumns"
                    type="number"
                    min="1"
                    value={values.blockColumns}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.blockColumns}
                    touched={touched.blockColumns}
                  />
                </div>

                <div className="create-block__grid">
                  <FormField
                    label="Largo (m)"
                    name="lockerLength"
                    type="number"
                    step="0.01"
                    min="0"
                    value={values.lockerLength}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.lockerLength}
                    touched={touched.lockerLength}
                  />
                  <FormField
                    label="Ancho (m)"
                    name="lockerWidth"
                    type="number"
                    step="0.01"
                    min="0"
                    value={values.lockerWidth}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.lockerWidth}
                    touched={touched.lockerWidth}
                  />
                  <FormField
                    label="Alto (m)"
                    name="lockerHeight"
                    type="number"
                    step="0.01"
                    min="0"
                    value={values.lockerHeight}
                    onChange={handleChange}
                    onBlur={handleBlur}
                    error={errors.lockerHeight}
                    touched={touched.lockerHeight}
                  />
                </div>

                <label className="create-block__checkbox">
                  <input
                    type="checkbox"
                    name="allowCustomRental"
                    checked={values.allowCustomRental}
                    onChange={handleCheckboxChange}
                  />
                  Permite renta personalizada (por días, en lugar de período completo)
                </label>

                <div className="create-block__actions">
                  <Button type="submit" loading={submitting}>
                    Crear bloque
                  </Button>
                  <Button type="button" variant="secondary" onClick={() => navigate(ROUTES.LOCKERS)}>
                    Ver casilleros
                  </Button>
                </div>
              </form>
            </>
          )}
        </Card>
      </div>
    </>
  );
}
