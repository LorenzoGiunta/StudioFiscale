package com.tesi.gestionalec.state;

import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoPratica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stato terminale di una pratica conclusa (pattern State).
 * Non sono previste ulteriori transizioni: ogni tentativo di avanzamento
 * viene respinto con un'eccezione, a tutela della coerenza del flusso.
 */
@Component
@RequiredArgsConstructor
public class CompletataState implements StatoPraticaState{
    @Override
    public void avanza(Pratica pratica) {
        throw new InvalidStateException("Pratica", StatoPratica.COMPLETATA.name(), "avanza");
    }

    @Override
    public StatoPratica getStato() {
        return StatoPratica.COMPLETATA;
    }
}

