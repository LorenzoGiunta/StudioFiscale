package com.tesi.gestionalec.facade;

import com.tesi.gestionalec.dto.request.DocumentoRequest;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.ClienteService;
import com.tesi.gestionalec.service.interfaces.DocumentoService;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade per la gestione dei documenti.
 *
 * Espone in un'unica operazione il caricamento di un documento e l'eventuale
 * assegnazione del revisore, coordinando i servizi di dominio e il mapping in
 * modo che il controller dipenda da un solo collaboratore anziché orchestrare
 * direttamente l'intera sequenza. La pratica di destinazione è risolta con il
 * controllo di ownership: il cliente può caricare solo sulle proprie pratiche.
 * Le notifiche di caricamento sono emesse dal servizio, quindi non vengono
 * duplicate qui.
 */
@Component
@RequiredArgsConstructor
public class DocumentoFacade {

    private final DocumentoService documentoService;
    private final PraticaService praticaService;
    private final ClienteService clienteService;

    /**
     * Carica un documento sulla pratica indicata e, se richiesto, lo assegna
     * subito a un revisore.
     *
     * @param collaboratoreId revisore a cui assegnare il documento, oppure
     *                        {@code null} se l'assegnazione non è ancora nota
     * @param richiedente     cliente che carica il documento (ownership)
     */
    public DocumentoResponse caricaEAssegna(DocumentoRequest request, Long collaboratoreId, Utente richiedente) {
        // Ownership check: la pratica deve appartenere al cliente che carica
        Pratica pratica = praticaService.trovaPerId(request.getPraticaId(), richiedente);
        Cliente cliente = clienteService.trovaClientePerId(richiedente.getId());

        // Creazione e persistenza del documento (il servizio emette le notifiche)
        Documento documento = DocumentoMapper.toModel(request, pratica, cliente);
        Documento salvato = documentoService.caricaDocumento(documento);

        // Assegnazione del revisore, se indicato, e rilettura dello stato aggiornato
        if (collaboratoreId != null) {
            documentoService.assegnaRevisore(salvato.getId(), collaboratoreId, richiedente);
            salvato = documentoService.trovaPerId(salvato.getId(), richiedente);
        }

        return DocumentoMapper.toResponse(salvato);
    }
}
