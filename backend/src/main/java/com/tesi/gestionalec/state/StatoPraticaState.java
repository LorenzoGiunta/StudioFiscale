package com.tesi.gestionalec.state;

import com.tesi.gestionalec.model.Pratica;

/**
 * Astrazione dello stato di una pratica nel pattern State.
 *
 * Ogni stato del ciclo di vita è una classe a sé che conosce la propria
 * identità e l'unica transizione ammessa a partire da sé: l'avanzamento è
 * realizzato passando il testimone direttamente alla classe successiva, senza
 * alcun codice di stato affiancato da mantenere in sincronia. La pratica
 * (Context) delega qui la richiesta di avanzamento, così il comportamento
 * cambia con lo stato senza catene di condizioni sparse nel codice.
 *
 * Gli stati sono privi di campi d'istanza, quindi vengono condivisi come
 * singleton ({@code XxxState.INSTANCE}) anziché creati a ogni transizione.
 */
public interface StatoPraticaState {

    /**
     * Fa evolvere la pratica verso lo stato successivo previsto dal flusso,
     * sostituendo su di essa lo stato corrente. Gli stati terminali segnalano
     * l'impossibilità di avanzare ulteriormente con un'eccezione.
     */
    void avanza(Pratica pratica);

    /**
     * Codice stabile dello stato, usato per la persistenza sulla colonna
     * {@code stato} e nelle risposte JSON verso il frontend. È derivato dalla
     * classe e non è modificabile: è una rappresentazione dello stato, non una
     * seconda copia di esso.
     */
    String getCodice();

    /** Descrizione leggibile, usata nei messaggi di notifica e nelle email. */
    String getEtichetta();

    /** Indica se dallo stato non è più possibile avanzare. */
    default boolean isFinale() {
        return false;
    }
}
