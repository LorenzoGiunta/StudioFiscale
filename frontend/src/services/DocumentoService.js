import BaseService from './BaseService.js';
import Documento from '../models/Documento.js';

const MULTIPART = { headers: { 'Content-Type': 'multipart/form-data' } };

/**
 * Service per la gestione dei documenti: caricamento e versionamento via
 * multipart, assegnazione del revisore, eliminazione e download del file.
 */
class DocumentoService extends BaseService {

  async getById(id) {
    return new Documento(await this.get(`/documenti/${id}`));
  }

  async upload(formData) {
    return new Documento(await this.post('/documenti', formData, MULTIPART));
  }

  async nuovaVersione(id, formData) {
    return new Documento(await this.post(`/documenti/${id}/nuova-versione`, formData, MULTIPART));
  }

  assegnaRevisore(documentoId, collaboratoreId) {
    return this.put(`/documenti/${documentoId}/assegna-revisore/${collaboratoreId}`);
  }

  // Approvazione/rifiuto da parte del commercialista dello studio
  approva(id) {
    return this.put(`/documenti/${id}/approva`);
  }

  rifiuta(id, motivazione) {
    return this.put(`/documenti/${id}/rifiuta`, null, { params: { motivazione } });
  }

  elimina(id) {
    return this.delete(`/documenti/${id}`);
  }

  // Recupera il file come blob e ne avvia il download nel browser
  async download(id, nomeFile) {
    const response = await this.client.get(`/documenti/${id}/download`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', nomeFile || 'documento');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }
}

export default new DocumentoService();
