import BaseService from './BaseService.js';
import Utente from '../models/Utente.js';

/**
 * Service per le funzioni di amministrazione.
 *
 * Incapsula le chiamate agli endpoint di amministrazione (gestione utenti,
 * statistiche di sistema) restituendo istanze del modello {@link Utente}.
 */
class AdminService extends BaseService {

  async getUtenti(page = 0, size = 20) {
    const data = await this.get('/admin/utenti', { params: { page, size } });
    return { ...data, content: Utente.fromList(data.content) };
  }

  async getUtenteById(id) {
    return new Utente(await this.get(`/admin/utenti/${id}`));
  }

  async getEliminati() {
    return Utente.fromList(await this.get('/admin/utenti/eliminati'));
  }

  // Metriche di sistema per la dashboard
  getStatistiche() {
    return this.get('/admin/statistiche');
  }

  // Istante dell'ultima azione amministrativa dell'amministratore corrente
  getUltimaAzione() {
    return this.get('/admin/ultima-azione');
  }

  abilita(id) {
    return this.put(`/admin/utenti/${id}/abilita`);
  }

  disabilita(id) {
    return this.put(`/admin/utenti/${id}/disabilita`);
  }

  elimina(id) {
    return this.delete(`/admin/utenti/${id}`);
  }

  ripristina(id) {
    return this.put(`/admin/utenti/${id}/ripristina`);
  }
}

export default new AdminService();
