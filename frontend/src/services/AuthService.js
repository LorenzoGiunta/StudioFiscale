import BaseService from './BaseService.js';

/**
 * Service per autenticazione e registrazione, oltre al recupero dell'elenco
 * pubblico dei commercialisti usato in fase di registrazione del cliente.
 */
class AuthService extends BaseService {
  login(email, password) {
    return this.post('/auth/login', { email, password });
  }

  register(payload) {
    return this.post('/auth/registra', payload);
  }

  /** Lista dei commercialisti abilitati (endpoint pubblico, nessun token richiesto). */
  getCommercialisti() {
    return this.get('/auth/commercialisti');
  }

  // ─── Recupero password ─────────────────────────────────────────────────────

  /** Chiede l'invio del link di reimpostazione all'email indicata. */
  richiediResetPassword(email) {
    return this.post('/auth/password/richiedi-reset', { email });
  }

  /** Imposta la nuova password usando il token ricevuto via email. */
  resetPassword(token, nuovaPassword) {
    return this.post('/auth/password/reset', { token, nuovaPassword });
  }
}

export default new AuthService();
