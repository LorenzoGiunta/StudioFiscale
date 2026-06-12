import BaseService from './BaseService.js';
import Cliente from '../models/Cliente.js';
import Utente from '../models/Utente.js';
import Documento from '../models/Documento.js';
import Pratica from '../models/Pratica.js';

/**
 * Service dell'area commercialista: clienti con relative pratiche e documenti,
 * collaboratori, documenti dello studio e calcolo delle imposte di un cliente.
 */
class CommercialistaService extends BaseService {

  async getClienti() {
    return Cliente.fromList(await this.get('/commercialista/clienti'));
  }

  async getCliente(id) {
    return new Cliente(await this.get(`/commercialista/clienti/${id}`));
  }

  async getClientePratiche(id) {
    return Pratica.fromList(await this.get(`/commercialista/clienti/${id}/pratiche`));
  }

  async getClienteDocumenti(id) {
    return Documento.fromList(await this.get(`/commercialista/clienti/${id}/documenti`));
  }

  async getCollaboratori() {
    return Utente.fromList(await this.get('/commercialista/collaboratori'));
  }

  async getDocumenti() {
    return Documento.fromList(await this.get('/commercialista/documenti'));
  }

  // Ritorna l'importo (Double) calcolato dal backend
  calcolaImposte(clienteId) {
    return this.get(`/commercialista/imposte/${clienteId}`);
  }
}

export default new CommercialistaService();
