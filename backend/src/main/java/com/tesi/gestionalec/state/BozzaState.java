package com.tesi.gestionalec.state;

import com.tesi.gestionalec.model.Pratica;

/**
 * Stato iniziale di una pratica appena creata (pattern State).
 * L'avanzamento la porta in lavorazione, dando inizio al flusso operativo.
 */
public final class BozzaState implements StatoPraticaState {

    /** Stato privo di campi d'istanza: una sola istanza condivisa è sufficiente. */
    public static final BozzaState INSTANCE = new BozzaState();

    private BozzaState() {
    }

    @Override
    public void avanza(Pratica pratica) {
        pratica.setStatoCorrente(InLavorazioneState.INSTANCE);
    }

    @Override
    public String getCodice() {
        return "BOZZA";
    }

    @Override
    public String getEtichetta() {
        return "Bozza";
    }

    @Override
    public String toString() {
        return getCodice();
    }
}
