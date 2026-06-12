import BaseModel from './BaseModel.js';

const RUOLO_LABEL = {
  COMMERCIALISTA: 'Commercialista',
  COLLABORATORE: 'Collaboratore',
  CLIENTE: 'Cliente',
  AMMINISTRATORE: 'Admin',
};

/**
 * Modello di dominio dell'utente lato client.
 *
 * Espone proprietà derivate per la presentazione (nome completo, iniziali,
 * etichetta del ruolo) e un metodo di confronto del ruolo.
 */
export default class Utente extends BaseModel {

  get nomeCompleto() {
    return `${this.nome ?? ''} ${this.cognome ?? ''}`.trim();
  }

  get ruoloLabel() {
    return RUOLO_LABEL[this.ruolo] || this.ruolo;
  }

  get iniziali() {
    return `${this.nome?.[0] ?? ''}${this.cognome?.[0] ?? ''}`.toUpperCase();
  }

  is(ruolo) {
    return this.ruolo === ruolo;
  }
}
