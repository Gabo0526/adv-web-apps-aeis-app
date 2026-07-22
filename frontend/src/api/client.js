import axios from 'axios';
import { getToken, clearAuth } from '../auth/storage';
import { ROUTES } from '../routes';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

client.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuth();
      if (window.location.pathname !== ROUTES.LOGIN) {
        window.location.href = ROUTES.LOGIN;
      }
    }
    return Promise.reject(error);
  }
);

export function extractErrorMessage(error) {
  return error.response?.data?.error ?? 'No se pudo conectar con el servidor. Intenta más tarde.';
}

export default client;
