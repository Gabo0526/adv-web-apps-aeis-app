import { useState } from 'react';
import Modal from '../../components/Modal';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import { createPeriod, updatePeriod } from '../../api/rentalsApi';
import { extractErrorMessage } from '../../api/client';
import { required, validateDateRange } from '../../utils/validators';

function toDateInput(value) {
  return value ? value.slice(0, 10) : '';
}

export default function PeriodFormModal({ period, onClose, onSaved }) {
  const [serverError, setServerError] = useState('');
  const isEdit = !!period;

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: {
      name: period?.name ?? '',
      startDate: toDateInput(period?.startDate),
      endDate: toDateInput(period?.endDate),
    },
    validate: (vals) => ({
      name: required(vals.name, 'El nombre'),
      endDate: validateDateRange(vals.startDate, vals.endDate),
    }),
    onSubmit: async (vals) => {
      setServerError('');
      const payload = {
        name: vals.name,
        startDate: `${vals.startDate}T00:00:00`,
        endDate: `${vals.endDate}T00:00:00`,
      };
      try {
        if (isEdit) {
          await updatePeriod(period.id, payload);
        } else {
          await createPeriod(payload);
        }
        onSaved();
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  return (
    <Modal
      open
      title={isEdit ? `Editar período: ${period.name}` : 'Nuevo período'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} loading={submitting}>
            Guardar
          </Button>
        </>
      }
    >
      <Banner type="error">{serverError}</Banner>
      <form onSubmit={handleSubmit} noValidate>
        <FormField
          label="Nombre"
          name="name"
          value={values.name}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.name}
          touched={touched.name}
        />
        <FormField
          label="Fecha de inicio"
          name="startDate"
          type="date"
          value={values.startDate}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.startDate}
          touched={touched.startDate}
        />
        <FormField
          label="Fecha de fin"
          name="endDate"
          type="date"
          value={values.endDate}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.endDate}
          touched={touched.endDate}
        />
      </form>
    </Modal>
  );
}
