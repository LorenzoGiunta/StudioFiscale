package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import java.util.List;

import com.tesi.gestionalec.model.Utente;

/**
 * Contratto per la gestione dei documenti: caricamento, versionamento,
 * assegnazione del revisore e cancellazione logica.
 */
public interface DocumentoService {
    Documento caricaDocumento(Documento documento);
    Documento trovaPerId(Long id);
    /** Variante sicura: verifica l'ownership in base al ruolo dell'utente. */
    Documento trovaPerId(Long id, Utente richiedente);
    List<Documento> trovaPerPratica(Pratica pratica);
    Documento nuovaVersione(Long documentoId, Documento nuovoDocumento);
    /** Variante sicura: verifica che l'autore sia il cliente loggato. */
    Documento nuovaVersione(Long documentoId, Documento nuovoDocumento, Utente richiedente);
    void assegnaRevisore(Long documentoId, Long collaboratoreId);

    /**
     * Variante sicura: il documento deve appartenere allo studio del
     * commercialista richiedente e il collaboratore-revisore deve far parte
     * dello stesso studio (invito ACCEPTED).
     */
    void assegnaRevisore(Long documentoId, Long collaboratoreId, Utente richiedente);

    /**
     * Approva un documento. Variante sicura: l'ownership è verificata in base al
     * ruolo del richiedente (commercialista dello studio o revisore assegnato) e
     * il cliente autore viene avvisato dell'esito.
     */
    void approvaDocumento(Long id, Utente richiedente);

    /**
     * Rifiuta un documento con la relativa motivazione. Stessa verifica di
     * ownership dell'approvazione, con avviso al cliente autore.
     */
    void rifiutaDocumento(Long id, String motivazione, Utente richiedente);

    /** Cancellazione logica del documento. */
    void eliminaDocumento(Long id);

    /** Variante sicura: il documento deve appartenere allo studio del richiedente. */
    void eliminaDocumento(Long id, Utente richiedente);
}
