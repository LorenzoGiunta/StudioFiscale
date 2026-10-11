package com.tesi.gestionalec.model;

/**
 * Esito della verifica amministrativa sull'iscrizione di un utente.
 *
 * I ruoli che non richiedono verifica nascono direttamente APPROVATA; per gli
 * altri (oggi il commercialista, di cui va controllato a mano il numero di
 * albo) l'account resta IN_ATTESA e disabilitato finché un amministratore non
 * lo approva o lo rifiuta.
 */
public enum StatoRegistrazione {
    IN_ATTESA("In attesa di verifica"),
    APPROVATA("Approvata"),
    RIFIUTATA("Rifiutata");

    private final String etichetta;

    StatoRegistrazione(String etichetta) {
        this.etichetta = etichetta;
    }

    /** Descrizione leggibile per messaggi ed email. */
    public String getEtichetta() {
        return etichetta;
    }
}
