package com.tesi.gestionalec.state;

import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoPratica;

/**
 * Astrazione dello stato di una pratica nel pattern State.
 *
 * Ogni stato del ciclo di vita di una pratica è rappresentato da
 * un'implementazione distinta che conosce la propria identità e l'unica
 * transizione ammessa a partire da sé. La pratica delega a questo tipo la
 * richiesta di avanzamento: in questo modo il comportamento cambia in base
 * allo stato corrente senza ricorrere a catene di condizioni sparse nel codice.
 */
public interface StatoPraticaState {

    /**
     * Fa evolvere la pratica verso lo stato successivo previsto dal flusso.
     * Gli stati terminali segnalano l'impossibilità di avanzare ulteriormente.
     */
    void avanza(Pratica pratica);

    /** Restituisce il valore di dominio corrispondente a questo stato. */
    StatoPratica getStato();
}
