export const ROUTES = {
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  RESET_PASSWORD: '/reset-password',
  VERIFY: '/verify',
  HOME: '/home',
  LOCKERS: '/lockers',
  MY_RENTALS: '/rentals/mine',
  PAYMENT_CHECKOUT: '/payment/checkout',
  PAYMENT_RESULT: '/payment/result',
  HELP: '/help',
  HELP_NEW: '/help/new',
  HELP_TICKET: '/help/:id',
  ADMIN_PERIODS: '/admin/periods',
  ADMIN_BLOCKS_NEW: '/admin/blocks/new',
  ADMIN_RENTALS: '/admin/rentals',
  ADMIN_RENTALS_EXCEPTIONAL: '/admin/rentals/exceptional',
  ADMIN_USERS: '/admin/users',
};

export function helpTicketPath(id) {
  return `/help/${id}`;
}
