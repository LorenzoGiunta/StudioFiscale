package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;

import java.util.List;

/**
 * Contratto specifico per il collaboratore: consultazione delle pratiche
 * assegnate e dei documenti da revisionare, con approvazione o rifiuto
 * motivato,
 * in aggiunta alle funzionalità comuni ereditate dal contratto utente.
 */
public interface CollaboratoreService extends UtenteService {
    List<Pratica> trovaPraticheAssegnate(Long collaboratoreId);

    List<Documento> trovaDocumentiInRevisione(Long collaboratoreId);

    /**
     * Approva il documento solo se è assegnato al collaboratore specificato.
     * Lancia ForbiddenOperationException altrimenti.
     */
    void approvaDocumento(Long documentoId, Long collaboratoreId);

    /**
     * Rifiuta il documento con motivazione, solo se assegnato al collaboratore
     * specificato.
     * Lancia ForbiddenOperationException altrimenti.
     */
    void rifiutaDocumento(Long documentoId, String motivazione, Long collaboratoreId);
}
