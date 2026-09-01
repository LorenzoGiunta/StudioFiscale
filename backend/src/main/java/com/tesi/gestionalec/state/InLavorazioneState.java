package com.tesi.gestionalec.state;

import com.tesi.gestionalec.model.Pratica;

/**
 * Stato di una pratica in corso di elaborazione (pattern State).
 * L'avanzamento la mette in attesa della documentazione necessaria.
 */
public final class InLavorazioneState implements StatoPraticaState {

    /** Stato privo di campi d'istanza: una sola istanza condivisa è sufficiente. */
    public static final InLavorazioneState INSTANCE = new InLavorazioneState();

    private InLavorazioneState() {
    }

    @Override
    public void avanza(Pratica pratica) {
        pratica.setStatoCorrente(InAttesaDocumentiState.INSTANCE);
    }

    @Override
    public String getCodice() {
        return "IN_LAVORAZIONE";
    }

    @Override
    public String getEtichetta() {
        return "In lavorazione";
    }

    @Override
    public String toString() {
        return getCodice();
    }
}
