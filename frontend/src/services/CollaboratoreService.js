import BaseService from './BaseService.js';
import Pratica from '../models/Pratica.js';
import Documento from '../models/Documento.js';

/**
 * Service dell'area collaboratore: pratiche assegnate, documenti da revisionare e
 * azioni di approvazione o rifiuto motivato.
 */
class CollaboratoreService extends BaseService {

  async getPratiche() {
    return Pratica.fromList(await this.get('/collaboratore/pratiche'));
  }

  async getDocumenti() {
    return Documento.fromList(await this.get('/collaboratore/documenti'));
  }

  approva(documentoId) {
    return this.put(`/collaboratore/${documentoId}/approva`);
  }

  rifiuta(documentoId, motivazione) {
    return this.put(`/collaboratore/${documentoId}/rifiuta`, null, { params: { motivazione } });
  }
}

export default new CollaboratoreService();
