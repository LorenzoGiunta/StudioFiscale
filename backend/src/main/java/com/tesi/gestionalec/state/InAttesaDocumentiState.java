package com.tesi.gestionalec.state;

import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;

import java.util.List;

/**
 * Stato in cui la pratica attende i documenti del cliente (pattern State).
 * L'avanzamento a COMPLETATA è consentito solo quando esiste almeno un
 * documento associato e tutti risultano approvati; in caso contrario
 * viene lanciata un'eccezione di stato non valido (HTTP 409).
 */
public final class InAttesaDocumentiState implements StatoPraticaState {

    /** Stato privo di campi d'istanza: una sola istanza condivisa è sufficiente. */
    public static final InAttesaDocumentiState INSTANCE = new InAttesaDocumentiState();

    private InAttesaDocumentiState() {
    }

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

        pratica.setStatoCorrente(CompletataState.INSTANCE);
    }

    @Override
    public String getCodice() {
        return "IN_ATTESA_DOCUMENTI";
    }

    @Override
    public String getEtichetta() {
        return "In attesa documenti";
    }

    @Override
    public String toString() {
        return getCodice();
    }
}
