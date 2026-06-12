import BaseService from './BaseService.js';
import Notifica from '../models/Notifica.js';

/**
 * Service per le notifiche: elenco paginato (completo o non lette), marcatura
 * come lette e conteggio delle non lette per l'indicatore in interfaccia.
 */
class NotificaService extends BaseService {

  async getAll(page = 0, size = 20) {
    const data = await this.get('/notifiche/mie', { params: { page, size } });
    return { ...data, content: Notifica.fromList(data.content) };
  }

  async getNonLette(page = 0, size = 20) {
    const data = await this.get('/notifiche/non-lette', { params: { page, size } });
    return { ...data, content: Notifica.fromList(data.content) };
  }

  markLetta(id) {
    return this.put(`/notifiche/${id}/letta`);
  }

  markTutteLette() {
    return this.put('/notifiche/letta-tutte');
  }

  async countNonLette() {
    const data = await this.get('/notifiche/non-lette', { params: { page: 0, size: 1 } });
    return data.totalElements ?? 0;
  }
}


export default new NotificaService();
