import BaseModel from './BaseModel.js';

/**
 * Modello di dominio del documento lato client.
 *
 * Espone proprietà derivate sullo stato di revisione (approvato, rifiutato, da
 * revisionare) con la relativa etichetta e un'indicazione sul tipo di file.
 */
export default class Documento extends BaseModel {

  get approvato() {
    return this.stato === 'APPROVATO';
  }

  get rifiutato() {
    return this.stato === 'RIFIUTATO';
  }

  get daRevisionare() {
    return this.stato === 'IN_REVISIONE' || !this.stato;
  }

  get statoLabel() {
    if (this.approvato) return 'Approvato';
    if (this.rifiutato) return 'Rifiutato';
    return 'Da revisionare';
  }

  get isPdf() {
    return this.nome?.toLowerCase().endsWith('.pdf') ?? false;
  }
}
