import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Contesto di autenticazione dell'applicazione.
 *
 * Conserva utente e token mantenendoli sincronizzati con il localStorage, così
 * da preservare la sessione tra i ricaricamenti, ed espone le operazioni di
 * login e logout e la rotta della dashboard corrispondente al ruolo.
 */
const AuthContext = createContext(null);

const ROLE_ROUTES = {
  CLIENTE: '/dashboard/cliente',
  COMMERCIALISTA: '/dashboard/commercialista',
  COLLABORATORE: '/dashboard/collaboratore',
  AMMINISTRATORE: '/dashboard/admin',
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('sf_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('sf_token'));

  const loginUser = useCallback((userData, authToken) => {
    localStorage.setItem('sf_user', JSON.stringify(userData));
    localStorage.setItem('sf_token', authToken);
    setUser(userData);
    setToken(authToken);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('sf_user');
    localStorage.removeItem('sf_token');
    setUser(null);
    setToken(null);
  }, []);

  const getDashboardRoute = useCallback(() => {
    if (!user) return '/login';
    return ROLE_ROUTES[user.ruolo] || '/login';
  }, [user]);

  const isAuthenticated = !!user && !!token;

  return (
    <AuthContext.Provider
      value={{ user, token, isAuthenticated, loginUser, logout, getDashboardRoute }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

export { ROLE_ROUTES };
