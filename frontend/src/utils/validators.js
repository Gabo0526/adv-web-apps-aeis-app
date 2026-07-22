const USERNAME_REGEX = /^[a-zA-Z0-9_.]{4,30}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const DIGITS_REGEX = /^\d+$/;

export function required(value, label = 'Este campo') {
  if (value === undefined || value === null || String(value).trim() === '') {
    return `${label} es requerido`;
  }
  return null;
}

export function validateCedula(value) {
  if (!value) return 'La cédula es requerida';
  if (!DIGITS_REGEX.test(value)) return 'La cédula debe contener solo dígitos';
  if (value.length !== 10) return 'La cédula debe tener exactamente 10 dígitos';
  return null;
}

export function validateUniqueCode(value) {
  if (!value) return 'El código único es requerido';
  if (!DIGITS_REGEX.test(value)) return 'El código único debe contener solo dígitos';
  if (value.length !== 9) return 'El código único debe tener exactamente 9 dígitos';
  return null;
}

export function validateUsername(value) {
  if (!value) return 'El usuario es requerido';
  if (!USERNAME_REGEX.test(value)) {
    return 'El usuario debe tener entre 4 y 30 caracteres (letras, números, "_" o ".")';
  }
  return null;
}

export function validateEmail(value) {
  if (!value) return 'El email es requerido';
  if (!EMAIL_REGEX.test(value)) return 'El email no tiene un formato válido';
  return null;
}

export function validatePassword(value, min = 8) {
  if (!value) return 'La contraseña es requerida';
  if (value.length < min) return `La contraseña debe tener al menos ${min} caracteres`;
  return null;
}

export function validatePasswordConfirmation(password, confirmation) {
  if (!confirmation) return 'Confirma la contraseña';
  if (password !== confirmation) return 'Las contraseñas no coinciden';
  return null;
}

export function validatePositiveInt(value) {
  const number = Number(value);
  if (!Number.isInteger(number) || number < 1) return 'Debe ser un número entero mayor o igual a 1';
  return null;
}

export function validateNonNegativeNumber(value) {
  const number = Number(value);
  if (Number.isNaN(number) || number < 0) return 'Debe ser un número mayor o igual a 0';
  return null;
}

export function validateDateRange(startDate, endDate) {
  if (!startDate || !endDate) return 'Ambas fechas son requeridas';
  if (new Date(startDate) >= new Date(endDate)) return 'La fecha de inicio debe ser anterior a la fecha de fin';
  return null;
}

export function validateCustomRentalRange(startDate, endDate, maxDays = 15) {
  if (!startDate || !endDate) return 'Ambas fechas son requeridas';
  // Comparación como strings "YYYY-MM-DD": evita desfases de huso horario
  // entre el "hoy" local y el parseo UTC de fechas sin hora (new Date('YYYY-MM-DD')).
  const todayStr = new Date().toLocaleDateString('en-CA');
  if (startDate < todayStr) return 'La fecha de inicio no puede ser en el pasado';
  if (startDate >= endDate) return 'La fecha de inicio debe ser anterior a la fecha de fin';
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  const days = (end - start) / (1000 * 60 * 60 * 24);
  if (days > maxDays) return `El rango no puede superar los ${maxDays} días`;
  return null;
}

export function validateAmount(value) {
  const number = Number(value);
  if (Number.isNaN(number) || number < 0) return 'El monto debe ser un número mayor o igual a 0';
  return null;
}
