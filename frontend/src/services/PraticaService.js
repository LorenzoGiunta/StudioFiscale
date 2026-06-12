import BaseService from './BaseService.js';
import Pratica from '../models/Pratica.js';
import Documento from '../models/Documento.js';

/**
 * Service per le pratiche: elenco paginato, dettaglio con documenti collegati,
 * creazione, avanzamento di stato, assegnazione ai collaboratori ed eliminazione.
 */
class PraticaService extends BaseService {

  // Restituisce la pagina di risultati con il contenuto mappato a istanze Pratica
  async getAll(page = 0, size = 20) {
    const data = await this.get('/pratiche', { params: { page, size } });
    return { ...data, content: Pratica.fromList(data.content) };
  }

  async getById(id) {
    return new Pratica(await this.get(`/pratiche/${id}`));
  }

  async getDocumenti(id) {
    return Documento.fromList(await this.get(`/pratiche/${id}/documenti`));
  }

  async create(payload) {
    return new Pratica(await this.post('/pratiche', payload));
  }

  avanzaStato(id) {
    return this.put(`/pratiche/${id}/avanza`);
  }

  assegnaCollaboratore(praticaId, collaboratoreId) {
    return this.put(`/pratiche/${praticaId}/assegna/${collaboratoreId}`);
  }

  elimina(id) {
    return this.delete(`/pratiche/${id}`);
  }
}

export default new PraticaService();
