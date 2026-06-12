import BaseModel from './BaseModel.js';

/**
 * Modello di dominio del messaggio di chat lato client.
 *
 * Offre metodi di supporto alla visualizzazione: riconoscere se il messaggio
 * appartiene all'utente corrente o a una data conversazione e formattarne l'orario.
 */
export default class Messaggio extends BaseModel {

  // Indica se il messaggio è stato inviato dall'utente con l'id fornito
  isMio(utenteId) {
    return this.mittenteId === utenteId;
  }

  get orario() {
    const ts = this.dataInvio || this.timestamp;
    if (!ts) return '';
    const d = new Date(ts);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  // Indica se il messaggio appartiene alla conversazione con il contatto indicato
  appartieneA(contattoId) {
    return this.mittenteId === contattoId || this.destinatarioId === contattoId;
  }
}
