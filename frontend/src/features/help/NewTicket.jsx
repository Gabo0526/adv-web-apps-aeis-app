import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../../components/Navbar';
import PageHeader from '../../components/PageHeader';
import Card from '../../components/Card';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import { useMyRentals } from '../rentals/useMyRentals';
import { createTicket } from '../../api/helpApi';
import { extractErrorMessage } from '../../api/client';
import { required } from '../../utils/validators';
import { helpTicketPath } from '../../routes';

export default function NewTicket() {
  const navigate = useNavigate();
  const { data: rentals } = useMyRentals();
  const [serverError, setServerError] = useState('');

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: { subject: '', description: '', rentalRef: '' },
    validate: (vals) => ({
      subject: required(vals.subject, 'El asunto'),
      description: required(vals.description, 'La descripción'),
    }),
    onSubmit: async (vals) => {
      setServerError('');
      try {
        const payload = { subject: vals.subject, description: vals.description };
        if (vals.rentalRef) payload.rentalRef = vals.rentalRef;
        const ticket = await createTicket(payload);
        navigate(helpTicketPath(ticket.id));
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  return (
    <>
      <Navbar />
      <div className="page-content">
        <PageHeader title="Nuevo ticket de ayuda" subtitle="Cuéntanos qué problema tienes y te ayudamos" />
        <Card>
          <Banner type="error">{serverError}</Banner>
          <form onSubmit={handleSubmit} noValidate>
            <FormField
              label="Asunto"
              name="subject"
              value={values.subject}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.subject}
              touched={touched.subject}
            />
            <FormField
              label="Descripción"
              name="description"
              as="textarea"
              rows={5}
              value={values.description}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.description}
              touched={touched.description}
            />
            <FormField
              label="Casillero relacionado (opcional)"
              name="rentalRef"
              as="select"
              value={values.rentalRef}
              onChange={handleChange}
            >
              <option value="">Ninguno</option>
              {(rentals ?? []).map((rental) => {
                const ref = `Casillero #${rental.lockerNumber} - ${rental.blockName}`;
                return (
                  <option key={rental.id} value={ref}>
                    {ref}
                  </option>
                );
              })}
            </FormField>
            <Button type="submit" loading={submitting}>
              Crear ticket
            </Button>
          </form>
        </Card>
      </div>
    </>
  );
}
