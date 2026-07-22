import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import { useAuth } from '../../auth/AuthContext';
import { required } from '../../utils/validators';
import { extractErrorMessage } from '../../api/client';
import { ROUTES } from '../../routes';

function validate(values) {
  return {
    username: required(values.username, 'El usuario'),
    password: required(values.password, 'La contraseña'),
  };
}

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: { username: '', password: '' },
    validate,
    onSubmit: async (formValues) => {
      setServerError('');
      try {
        await login(formValues);
        navigate(ROUTES.HOME);
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  return (
    <AuthLayout title="Iniciar sesión" icon="fa-archive" subtitle="Sistema de casilleros AEIS-EPN">
      <Banner type="error">{serverError}</Banner>
      <form onSubmit={handleSubmit} noValidate>
        <FormField
          label="Usuario"
          name="username"
          value={values.username}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.username}
          touched={touched.username}
          autoComplete="username"
        />
        <FormField
          label="Contraseña"
          name="password"
          type="password"
          value={values.password}
          onChange={handleChange}
          onBlur={handleBlur}
          error={errors.password}
          touched={touched.password}
          autoComplete="current-password"
        />
        <Button type="submit" className="btn--full" loading={submitting}>
          Ingresar
        </Button>
      </form>
      <div className="auth-links">
        <Link to={ROUTES.FORGOT_PASSWORD}>¿Olvidaste tu contraseña?</Link>
        <span>
          ¿No tienes cuenta? <Link to={ROUTES.REGISTER}>Regístrate</Link>
        </span>
      </div>
    </AuthLayout>
  );
}
