package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import java.util.List;

/**
 * Contratto specifico per il commercialista: gestione delle pratiche di studio,
 * assegnazioni ai collaboratori, calcolo delle imposte e accesso a clienti,
 * collaboratori e documenti, in aggiunta alle funzionalità comuni ereditate.
 */
public interface CommercialistaService extends UtenteService {
    List<Pratica> trovaTutteLePratiche();

    void assegnaCollaboratore(Long praticaId, Long collaboratoreId);

    void avanzaStatoPratica(Long praticaId);

    double calcolaImposteCliente(Long clienteId);

    /** Lista di tutti i clienti del sistema (accessibile al commercialista). */
    List<Cliente> trovaTuttiClienti();

    /** Clienti associati a uno specifico commercialista. */
    List<Cliente> trovaClientiDelCommercialista(Long commercialistaId);

    /** Collaboratori associati al commercialista (inviti ACCEPTED). */
    List<Collaboratore> trovaMieiCollaboratori(Long commercialistaId);

    /**
     * Documenti dello studio del commercialista specificato (non quelli di altri
     * studi).
     */
    List<Documento> trovaDocumentiStudio(Long commercialistaId);

    /**
     * Verifica che il cliente con clienteId appartenga al commercialista con
     * commercialistaId.
     * Lancia ForbiddenOperationException se il cliente appartiene a un altro
     * commercialista.
     * Lancia ResourceNotFoundException se il cliente non esiste.
     */
    void verificaAppartenenzaCliente(Long clienteId, Long commercialistaId);
}
