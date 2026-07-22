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
