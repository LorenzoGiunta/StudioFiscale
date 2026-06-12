import BaseService from './BaseService.js';
import Invito from '../models/Invito.js';

/**
 * Service per gli inviti di collaborazione, lato commercialista (emissione,
 * elenco, revoca) e lato collaboratore (inviti ricevuti, accettazione). Include
 * il rifiuto pubblico raggiungibile dal link email senza autenticazione.
 */
class InvitoService extends BaseService {

  async invita(emailDestinatario) {
    return new Invito(await this.post('/inviti', { emailDestinatario }));
  }

  async getMiei() {
    return Invito.fromList(await this.get('/inviti/miei'));
  }

  revoca(id) {
    return this.delete(`/inviti/${id}`);
  }

  async getPending() {
    return Invito.fromList(await this.get('/inviti/pending'));
  }

  async getAccettati() {
    return Invito.fromList(await this.get('/inviti/accettati'));
  }

  accetta(token) {
    return this.post(`/inviti/${token}/accetta`);
  }

  // Endpoint pubblico raggiungibile dal link email, senza autenticazione.
  // Si usa fetch nativo per evitare l'interceptor del 401 del client condiviso.
  async rifiutaPubblico(token) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL
      ? `${import.meta.env.VITE_API_BASE_URL}/api`
      : '/api';
    const res = await fetch(`${baseUrl}/inviti/${token}/rifiuta`, { method: 'POST' });
    if (!res.ok) throw new Error('Errore nel rifiuto dell\'invito');
  }
}

export default new InvitoService();
