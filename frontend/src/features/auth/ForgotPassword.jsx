import { useState } from 'react';
import { Link } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import * as authApi from '../../api/authApi';
import { extractErrorMessage } from '../../api/client';
import { validateEmail } from '../../utils/validators';
import { ROUTES } from '../../routes';

function validate(values) {
  return { email: validateEmail(values.email) };
}

export default function ForgotPassword() {
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: { email: '' },
    validate,
    onSubmit: async (formValues) => {
      setServerError('');
      setSuccessMessage('');
      try {
        const response = await authApi.forgotPassword(formValues.email);
        setSuccessMessage(
          response.message ?? 'Si el correo existe, te enviamos un enlace para restablecer tu contraseña.'
        );
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  return (
    <AuthLayout
      title="¿Olvidaste tu contraseña?"
      icon="fa-key"
      subtitle="Te enviaremos un enlace para restablecerla"
    >
      <Banner type="success">{successMessage}</Banner>
      <Banner type="error">{serverError}</Banner>

      {!successMessage && (
        <form onSubmit={handleSubmit} noValidate>
          <FormField
            label="Email"
            name="email"
            type="email"
            value={values.email}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.email}
            touched={touched.email}
            autoComplete="email"
          />
          <Button type="submit" className="btn--full" loading={submitting}>
            Enviar enlace
          </Button>
        </form>
      )}

      <div className="auth-links">
        <Link to={ROUTES.LOGIN}>Volver a iniciar sesión</Link>
      </div>
    </AuthLayout>
  );
}
