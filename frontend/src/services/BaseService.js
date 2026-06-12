import apiClient from '../api/client.js';

/**
 * Classe base per tutti i service.
 * Incapsula il client HTTP e offre i verbi REST di base; le sottoclassi
 * espongono metodi di dominio che restituiscono istanze dei modelli.
 */
export default class BaseService {
  constructor(client = apiClient) {
    this.client = client;
  }

  async get(url, config) {
    const { data } = await this.client.get(url, config);
    return data;
  }

  async post(url, body, config) {
    const { data } = await this.client.post(url, body, config);
    return data;
  }

  async put(url, body, config) {
    const { data } = await this.client.put(url, body, config);
    return data;
  }

  async delete(url, config) {
    const { data } = await this.client.delete(url, config);
    return data;
  }
}
