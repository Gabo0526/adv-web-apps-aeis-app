import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import * as authApi from '../../api/authApi';
import { extractErrorMessage } from '../../api/client';
import { validatePassword, validatePasswordConfirmation } from '../../utils/validators';
import { ROUTES } from '../../routes';

function validate(values) {
  return {
    newPassword: validatePassword(values.newPassword),
    confirmPassword: validatePasswordConfirmation(values.newPassword, values.confirmPassword),
  };
}

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: { newPassword: '', confirmPassword: '' },
    validate,
    onSubmit: async (formValues) => {
      setServerError('');
      setSuccessMessage('');
      try {
        await authApi.resetPassword(token, formValues.newPassword);
        setSuccessMessage('Tu contraseña fue actualizada. Ya puedes iniciar sesión.');
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  if (!token) {
    return (
      <AuthLayout title="Restablecer contraseña" icon="fa-key">
        <Banner type="error">El enlace no es válido: falta el token de restablecimiento.</Banner>
        <div className="auth-links">
          <Link to={ROUTES.FORGOT_PASSWORD}>Solicitar un nuevo enlace</Link>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Restablecer contraseña" icon="fa-key" subtitle="Elige tu nueva contraseña">
      <Banner type="success">{successMessage}</Banner>
      <Banner type="error">{serverError}</Banner>

      {!successMessage && (
        <form onSubmit={handleSubmit} noValidate>
          <FormField
            label="Nueva contraseña"
            name="newPassword"
            type="password"
            value={values.newPassword}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.newPassword}
            touched={touched.newPassword}
            autoComplete="new-password"
          />
          <FormField
            label="Confirmar contraseña"
            name="confirmPassword"
            type="password"
            value={values.confirmPassword}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.confirmPassword}
            touched={touched.confirmPassword}
            autoComplete="new-password"
          />
          <Button type="submit" className="btn--full" loading={submitting}>
            Restablecer contraseña
          </Button>
        </form>
      )}

      <div className="auth-links">
        <Link to={ROUTES.LOGIN}>Volver a iniciar sesión</Link>
      </div>
    </AuthLayout>
  );
}
