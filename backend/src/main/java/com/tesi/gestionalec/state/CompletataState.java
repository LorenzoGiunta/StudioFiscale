package com.tesi.gestionalec.state;

import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.model.Pratica;

/**
 * Stato terminale di una pratica conclusa (pattern State).
 * Non sono previste ulteriori transizioni: ogni tentativo di avanzamento
 * viene respinto con un'eccezione, a tutela della coerenza del flusso.
 */
public final class CompletataState implements StatoPraticaState {

    /** Stato privo di campi d'istanza: una sola istanza condivisa è sufficiente. */
    public static final CompletataState INSTANCE = new CompletataState();

    private CompletataState() {
    }

    @Override
    public void avanza(Pratica pratica) {
        throw new InvalidStateException("Pratica", getCodice(), "avanza");
    }

    @Override
    public String getCodice() {
        return "COMPLETATA";
    }

    @Override
    public String getEtichetta() {
        return "Completata";
    }

    @Override
    public boolean isFinale() {
        return true;
    }

    @Override
    public String toString() {
        return getCodice();
    }
}
