/**
 * Classe base per tutti i modelli di dominio.
 * Copia i campi del DTO ricevuto dal backend sull'istanza, così l'accesso
 * ai campi resta identico, e le sottoclassi aggiungono comportamento (metodi/getter).
 */
export default class BaseModel {
  constructor(data = {}) {
    Object.assign(this, data);
  }

  // Factory: costruisce una lista di istanze da un array di DTO grezzi
  static fromList(arr = []) {
    return (arr || []).map((item) => new this(item));
  }
}
