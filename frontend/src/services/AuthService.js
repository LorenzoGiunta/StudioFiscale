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
}

export default new AuthService();
