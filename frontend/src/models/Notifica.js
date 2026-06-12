import BaseModel from './BaseModel.js';

/**
 * Modello di dominio della notifica lato client, con la proprietà derivata che
 * indica se la notifica è ancora da leggere.
 */
export default class Notifica extends BaseModel {

  get nonLetta() {
    return !this.letta;
  }
}
