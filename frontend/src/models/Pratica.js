import BaseModel from './BaseModel.js';

const GIORNI_SCADENZA_DEFAULT = 7;
const MS_PER_GIORNO = 1000 * 60 * 60 * 24;

/**
 * Modello di dominio della pratica lato client.
 *
 * Fornisce proprietà derivate per la presentazione (etichetta del tipo, stato di
 * completamento, assegnazione) e il calcolo dei giorni alla scadenza, inclusa la
 * verifica di imminenza entro una soglia configurabile.
 */
export default class Pratica extends BaseModel {

  get tipoLabel() {
    return this.tipoPratica?.replace(/_/g, ' ') || '—';
  }

  get completata() {
    return this.stato === 'COMPLETATA';
  }

  // Giorni di calendario mancanti alla scadenza: l'orario viene azzerato così
  // che "scade oggi" valga 0, coerentemente con il conteggio del backend
  // (ChronoUnit.DAYS.between) usato per le notifiche di scadenza.
  giorniAllaScadenza() {
    if (!this.scadenza) return null;
    const oggi = new Date();
    oggi.setHours(0, 0, 0, 0);
    const scad = new Date(this.scadenza);
    scad.setHours(0, 0, 0, 0);
    return Math.round((scad - oggi) / MS_PER_GIORNO);
  }

  // In scadenza se mancano da 0 (oggi) a `giorni` giorni inclusi: stessa
  // finestra usata dal backend per le notifiche (>= 0 && <= 7).
  isInScadenza(giorni = GIORNI_SCADENZA_DEFAULT) {
    if (this.completata || !this.scadenza) return false;
    const g = this.giorniAllaScadenza();
    return g >= 0 && g <= giorni;
  }

  get assegnata() {
    return !!this.nomeCollaboratore;
  }
}
