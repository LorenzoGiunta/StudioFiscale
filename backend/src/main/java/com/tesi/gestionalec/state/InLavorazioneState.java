package com.tesi.gestionalec.state;

import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoPratica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Stato di una pratica in corso di elaborazione (pattern State).
 * L'avanzamento la mette in attesa della documentazione necessaria.
 */
@Component
@RequiredArgsConstructor
public class InLavorazioneState implements StatoPraticaState{
    @Override
    public void avanza(Pratica pratica) {
        pratica.setStato(StatoPratica.IN_ATTESA_DOCUMENTI);
        pratica.setStatoCorrente(new InAttesaDocumentiState());
    }

    @Override
    public StatoPratica getStato() {
        return StatoPratica.IN_LAVORAZIONE;
    }
}
