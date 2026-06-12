import axios from 'axios';

/**
 * Client HTTP centralizzato verso le API del backend.
 *
 * In sviluppo le richieste a /api vengono inoltrate dal proxy al backend
 * locale; in produzione l'indirizzo è determinato da una variabile d'ambiente.
 * Due interceptor gestiscono in modo trasversale l'autenticazione (aggiunta del
 * token JWT) e gli errori (estrazione del payload d'errore e gestione del 401).
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
    ? `${import.meta.env.VITE_API_BASE_URL}/api`
    : '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// Aggiunge il token JWT, se presente, a ogni richiesta uscente
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sf_token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  },
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const apiError = error.response?.data;

    // Rende disponibile il payload d'errore del backend ai gestori del catch
    if (apiError && apiError.status && apiError.message) {
      error.apiError = apiError;
    }

    // Un 401 sull'endpoint di login è un esito atteso (credenziali errate o
    // account disabilitato): non va trattato come sessione scaduta, altrimenti
    // il reindirizzamento ricaricherebbe la pagina prima che la LoginPage possa
    // mostrare l'avviso. Lo si lascia gestire al chiamante.
    const isLoginRequest = (error.config?.url || '').includes('/auth/login');

    if (status === 401 && !isLoginRequest) {
      localStorage.removeItem('sf_token');
      localStorage.removeItem('sf_user');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export default apiClient;
