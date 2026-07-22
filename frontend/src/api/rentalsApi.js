import client from './client';

export function getActivePeriod() {
  return client.get('/periods/active').then((res) => res.data);
}

export function createPreRental(payload) {
  return client.post('/rentals/pre-rentals', payload).then((res) => res.data);
}

export function confirmPayphonePayment(id, clientTransactionId) {
  return client
    .get('/payments/payphone/confirm', { params: { id, clientTransactionId } })
    .then((res) => res.data);
}

export function getMyRentals() {
  return client.get('/rentals/mine').then((res) => res.data);
}

export function getPeriods() {
  return client.get('/periods').then((res) => res.data);
}

export function createPeriod(payload) {
  return client.post('/periods', payload).then((res) => res.data);
}

export function updatePeriod(id, payload) {
  return client.put(`/periods/${id}`, payload).then((res) => res.data);
}

export function activatePeriod(id) {
  return client.post(`/periods/${id}/activate`).then((res) => res.data);
}

export function getAdminRentals({ page = 0, size = 10, sortBy = 'startDate', direction = 'desc' } = {}) {
  return client.get('/rentals/admin', { params: { page, size, sortBy, direction } }).then((res) => res.data);
}

export function getFilteredRentals(
  filters = {},
  { page = 0, size = 10, sortBy = 'startDate', direction = 'desc' } = {}
) {
  return client
    .get('/rentals/admin/filtered', { params: { ...filters, page, size, sortBy, direction } })
    .then((res) => res.data);
}

export function getRentalStatuses() {
  return client.get('/rentals/admin/statuses').then((res) => res.data);
}

export function createExceptionalRental(payload) {
  return client.post('/rentals/admin/exceptional', payload).then((res) => res.data);
}

export function generateExcel(rows, filename = 'reporte') {
  return client
    .post('/excel/generate', rows, { params: { filename }, responseType: 'blob' })
    .then((res) => res.data);
}
