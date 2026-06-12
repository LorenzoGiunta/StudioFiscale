import BaseService from './BaseService.js';
import Messaggio from '../models/Messaggio.js';
import Utente from '../models/Utente.js';

/**
 * Service per la chat: storico dei messaggi, contatti disponibili e gestione dei
 * messaggi non letti, con mappatura sui modelli {@link Messaggio} e {@link Utente}.
 */
class ChatService extends BaseService {

  async getStorico(altroUtenteId) {
    return Messaggio.fromList(await this.get(`/chat/storico/${altroUtenteId}`));
  }

  async getContatti() {
    return Utente.fromList(await this.get('/chat/contatti'));
  }

  // Mappa { mittenteId: numeroNonLetti }
  getNonLetti() {
    return this.get('/chat/non-letti');
  }

  segnaLetti(altroUtenteId) {
    return this.put(`/chat/${altroUtenteId}/letti`);
  }
}

export default new ChatService();
