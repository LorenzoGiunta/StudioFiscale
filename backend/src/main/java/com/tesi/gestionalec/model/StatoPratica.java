package com.tesi.gestionalec.model;

/** Stati del ciclo di vita di una pratica, percorsi in sequenza tramite il pattern State. */
public enum StatoPratica {
    BOZZA("Bozza"),
    IN_LAVORAZIONE("In lavorazione"),
    IN_ATTESA_DOCUMENTI("In attesa documenti"),
    COMPLETATA("Completata");

    private final String etichetta;

    StatoPratica(String etichetta) {
        this.etichetta = etichetta;
    }

    /** Descrizione leggibile (senza underscore) per messaggi ed email. */
    public String getEtichetta() {
        return etichetta;
    }
}
