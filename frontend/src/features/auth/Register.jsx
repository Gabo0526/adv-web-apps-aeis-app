import { useState } from 'react';
import { Link } from 'react-router-dom';
import AuthLayout from './AuthLayout';
import FormField from '../../components/FormField';
import Button from '../../components/Button';
import Banner from '../../components/Banner';
import { useForm } from '../../hooks/useForm';
import * as authApi from '../../api/authApi';
import { extractErrorMessage } from '../../api/client';
import {
  validateCedula,
  validateUniqueCode,
  validateUsername,
  validateEmail,
  validatePassword,
  validatePasswordConfirmation,
  required,
} from '../../utils/validators';
import { COLLEGE } from '../../utils/constants';
import { ROUTES } from '../../routes';

const INITIAL_VALUES = {
  id: '',
  username: '',
  name: '',
  lastName: '',
  uniqueCode: '',
  email: '',
  password: '',
  confirmPassword: '',
  college: '',
};

function validate(values) {
  return {
    id: validateCedula(values.id),
    username: validateUsername(values.username),
    name: required(values.name, 'El nombre'),
    lastName: required(values.lastName, 'El apellido'),
    uniqueCode: validateUniqueCode(values.uniqueCode),
    email: validateEmail(values.email),
    password: validatePassword(values.password),
    confirmPassword: validatePasswordConfirmation(values.password, values.confirmPassword),
    college: required(values.college, 'La facultad'),
  };
}

export default function Register() {
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const { values, errors, touched, submitting, handleChange, handleBlur, handleSubmit } = useForm({
    initialValues: INITIAL_VALUES,
    validate,
    onSubmit: async (formValues) => {
      setServerError('');
      setSuccessMessage('');
      const { confirmPassword: _confirmPassword, ...payload } = formValues;
      try {
        const response = await authApi.register(payload);
        setSuccessMessage(response.message ?? 'Registro exitoso. Revisa tu correo para verificar tu cuenta.');
      } catch (err) {
        setServerError(extractErrorMessage(err));
      }
    },
  });

  return (
    <AuthLayout title="Crear cuenta" icon="fa-user-plus" subtitle="Regístrate en el sistema AEIS-EPN" maxWidth={560}>
      <Banner type="success">{successMessage}</Banner>
      <Banner type="error">{serverError}</Banner>

      {!successMessage && (
        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-form__grid">
            <FormField
              label="Cédula"
              name="id"
              value={values.id}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.id}
              touched={touched.id}
              maxLength={10}
            />
            <FormField
              label="Código único"
              name="uniqueCode"
              value={values.uniqueCode}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.uniqueCode}
              touched={touched.uniqueCode}
              maxLength={9}
            />
            <FormField
              label="Nombres"
              name="name"
              value={values.name}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.name}
              touched={touched.name}
            />
            <FormField
              label="Apellidos"
              name="lastName"
              value={values.lastName}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.lastName}
              touched={touched.lastName}
            />
          </div>

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
          <FormField
            label="Facultad"
            name="college"
            as="select"
            value={values.college}
            onChange={handleChange}
            onBlur={handleBlur}
            error={errors.college}
            touched={touched.college}
          >
            <option value="">Selecciona tu facultad</option>
            {Object.entries(COLLEGE).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </FormField>

          <div className="auth-form__grid">
            <FormField
              label="Contraseña"
              name="password"
              type="password"
              value={values.password}
              onChange={handleChange}
              onBlur={handleBlur}
              error={errors.password}
              touched={touched.password}
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
          </div>

          <Button type="submit" className="btn--full" loading={submitting}>
            Registrarme
          </Button>
        </form>
      )}

      <div className="auth-links">
        <span>
          ¿Ya tienes cuenta? <Link to={ROUTES.LOGIN}>Inicia sesión</Link>
        </span>
      </div>
    </AuthLayout>
  );
}
