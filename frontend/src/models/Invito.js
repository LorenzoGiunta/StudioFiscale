import BaseModel from './BaseModel.js';

const MS_PER_GIORNO = 1000 * 60 * 60 * 24;

/**
 * Modello di dominio dell'invito di collaborazione lato client.
 *
 * Aggiunge proprietà derivate sullo stato (in attesa, accettato) e il calcolo
 * dei giorni mancanti alla scadenza con la relativa etichetta descrittiva.
 */
export default class Invito extends BaseModel {

  get pending() {
    return this.stato === 'PENDING';
  }

  get accettato() {
    return this.stato === 'ACCEPTED';
  }

  giorniAllaScadenza() {
    if (!this.scadeIl) return null;
    return Math.max(0, Math.ceil((new Date(this.scadeIl) - new Date()) / MS_PER_GIORNO));
  }

  get scadenzaLabel() {
    const g = this.giorniAllaScadenza();
    if (g === null) return '';
    if (g === 0) return 'Scade oggi!';
    if (g === 1) return 'Scade domani';
    return `Scade tra ${g} giorni`;
  }
}
