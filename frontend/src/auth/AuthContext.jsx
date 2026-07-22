import { createContext, useContext, useMemo, useState } from 'react';
import * as authApi from '../api/authApi';
import { getStoredUser, setAuth, clearAuth } from './storage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getStoredUser());

  async function login(credentials) {
    const response = await authApi.login(credentials);
    const storedUser = setAuth(response.token);
    setUser(storedUser);
    return storedUser;
  }

  function logout() {
    clearAuth();
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isAdmin: !!user?.roles?.includes('ADMIN'),
      login,
      logout,
    }),
    [user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
}
