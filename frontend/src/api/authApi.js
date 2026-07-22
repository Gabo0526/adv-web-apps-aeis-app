import client from './client';

export function register(payload) {
  return client.post('/auth/register', payload).then((res) => res.data);
}

export function verifyAccount(token) {
  return client.get('/auth/verify', { params: { token } }).then((res) => res.data);
}

export function login(credentials) {
  return client.post('/auth/login', credentials).then((res) => res.data);
}

export function forgotPassword(email) {
  return client.post('/auth/forgot-password', { email }).then((res) => res.data);
}

export function resetPassword(token, newPassword) {
  return client.post('/auth/reset-password', { token, newPassword }).then((res) => res.data);
}

export function getUsers({ page = 0, size = 10 } = {}) {
  return client.get('/users', { params: { page, size } }).then((res) => res.data);
}

export function searchUsersByIdPrefix(idPrefix) {
  return client.get('/users/search', { params: { idPrefix } }).then((res) => res.data);
}
