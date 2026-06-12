import BaseModel from './BaseModel.js';

/**
 * Modello di dominio del cliente lato client.
 *
 * Estende il modello base con proprietà derivate utili alla presentazione, come
 * il nome completo, l'etichetta del regime fiscale e la verifica della presenza
 * dei dati fiscali necessari al calcolo delle imposte.
 */
export default class Cliente extends BaseModel {

  get nomeCompleto() {
    return `${this.nome ?? ''} ${this.cognome ?? ''}`.trim();
  }

  get haDatiFiscali() {
    return !!this.regime && this.redditoAnnuo != null;
  }

  get regimeLabel() {
    if (this.regime === 'FORFETTARIO') return 'Forfettario';
    if (this.regime === 'ORDINARIO') return 'Ordinario';
    return 'Non specificato';
  }
}
