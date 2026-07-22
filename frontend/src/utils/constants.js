export const LOCKER_STATUS = {
  AVAILABLE: { label: 'Disponible', color: 'var(--success-color)' },
  OCCUPIED: { label: 'Ocupado', color: 'var(--danger-color)' },
  PENDING: { label: 'Pendiente', color: 'var(--warning-color)' },
  UNDER_MAINTENANCE: { label: 'En mantenimiento', color: '#9ca3af' },
};

export const RENTAL_STATUS = {
  ACTIVE: { label: 'Activo', color: 'var(--success-color)' },
  COMPLETED: { label: 'Completado', color: 'var(--info-color)' },
  CANCELLED: { label: 'Cancelado', color: 'var(--danger-color)' },
  PENDING: { label: 'Pendiente', color: 'var(--warning-color)' },
};

export const PRE_RENTAL_STATUS = {
  PENDING: { label: 'Pendiente', color: 'var(--warning-color)' },
  CANCELLED: { label: 'Cancelado', color: 'var(--danger-color)' },
  EXPIRED: { label: 'Expirado', color: '#9ca3af' },
  COMPLETED: { label: 'Completado', color: 'var(--success-color)' },
};

export const COLLEGE = {
  FC: 'Facultad de Ciencias',
  FCA: 'Facultad de Ciencias Administrativas',
  FICA: 'Facultad de Ingeniería Civil y Ambiental',
  FIEE: 'Facultad de Ingeniería Eléctrica y Electrónica',
  FGP: 'Facultad de Geología y Petróleos',
  FIM: 'Facultad de Ingeniería Mecánica',
  FIQA: 'Facultad de Ingeniería Química y Agroindustria',
  FIS: 'Facultad de Ingeniería de Sistemas',
  ESFOT: 'Escuela de Formación de Tecnólogos',
  FB: 'Formación Básica',
};

export const ROLES = {
  ADMIN: 'ADMIN',
  USER: 'USER',
};

// Debe reflejar exactamente LockerRentalSetting del rental-service (§14.4 del PLAN).
export const RENTAL_SETTINGS = {
  PERIOD_RENT_PRICE: 6.5,
  CUSTOM_RENT_DAILY_PRICE: 1,
  MAX_RENT_DAYS: 15,
};
