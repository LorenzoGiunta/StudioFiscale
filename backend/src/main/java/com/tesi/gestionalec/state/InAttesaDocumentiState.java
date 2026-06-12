package com.tesi.gestionalec.state;

import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;
import com.tesi.gestionalec.model.StatoPratica;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stato in cui la pratica attende i documenti del cliente (pattern State).
 * L'avanzamento a COMPLETATA è consentito solo quando esiste almeno un
 * documento associato e tutti risultano approvati; in caso contrario
 * viene lanciata un'eccezione di stato non valido (HTTP 409).
 */
@Component
public class InAttesaDocumentiState implements StatoPraticaState{

    @Override
    public void avanza(Pratica pratica) {

        List<Documento> documenti = pratica.getListaDocumenti();

        // Deve esistere almeno un documento associato alla pratica
        if (documenti == null || documenti.isEmpty()) {
            throw new InvalidStateException(
                    "Impossibile completare la pratica: è necessario almeno un documento.");
        }

        // Tutti i documenti devono essere approvati prima di completare
        boolean tuttiApprovati = documenti.stream()
                .allMatch(d -> d.getStato() == StatoDocumento.APPROVATO);

        if (!tuttiApprovati) {
            throw new InvalidStateException(
                    "Impossibile completare la pratica: tutti i documenti devono essere approvati.");
        }

        pratica.setStato(StatoPratica.COMPLETATA);
        pratica.setStatoCorrente(new CompletataState());
    }

    @Override
    public StatoPratica getStato() {
        return StatoPratica.IN_ATTESA_DOCUMENTI;
    }
}
