import BaseService from './BaseService.js';
import Pratica from '../models/Pratica.js';
import Documento from '../models/Documento.js';
import Cliente from '../models/Cliente.js';

/**
 * Service dell'area cliente: pratiche, documenti e profilo dell'utente
 * autenticato, restituiti come istanze dei rispettivi modelli.
 */
class ClienteService extends BaseService {

  async getPratiche() {
    return Pratica.fromList(await this.get('/cliente/pratiche'));
  }

  async getDocumenti() {
    return Documento.fromList(await this.get('/cliente/documenti'));
  }

  async getProfilo() {
    return new Cliente(await this.get('/cliente/profilo'));
  }

  async aggiornaProfilo(payload) {
    return new Cliente(await this.put('/cliente/profilo', payload));
  }
}

export default new ClienteService();
