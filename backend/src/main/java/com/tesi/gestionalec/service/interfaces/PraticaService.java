package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoPratica;
import com.tesi.gestionalec.model.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contratto per la gestione del ciclo di vita delle pratiche: creazione,
 * ricerca, avanzamento di stato, assegnazione ai collaboratori e cancellazione
 * logica.
 */
public interface PraticaService {
    Pratica creaPratica(Pratica pratica);

    /**
     * Variante sicura: verifica che il cliente intestatario della pratica
     * appartenga al commercialista richiedente prima di crearla, impedendo di
     * aprire pratiche su clienti di altri studi (IDOR).
     */
    Pratica creaPratica(Pratica pratica, Utente richiedente);

    /** Ricerca senza ownership check (uso interno / COMMERCIALISTA). */
    Pratica trovaPerId(Long id);

    /**
     * Ricerca con ownership check basato sul ruolo del richiedente:
     * CLIENTE vede solo le proprie pratiche, COLLABORATORE solo quelle assegnate,
     * COMMERCIALISTA solo quelle dei propri clienti.
     * Lancia ForbiddenOperationException in caso di accesso non autorizzato.
     */
    Pratica trovaPerId(Long id, Utente richiedente);

    List<Pratica> trovaTutte();
    Page<Pratica> trovaTutte(Pageable pageable);
    List<Pratica> trovaPerCliente(Cliente cliente);
    List<Pratica> trovaPerCollaboratore(Collaboratore collaboratore);
    void avanzaStato(Long praticaId);

    /** Variante sicura: la pratica deve appartenere al commercialista richiedente. */
    void avanzaStato(Long praticaId, Utente richiedente);

    void assegnaCollaboratore(Long praticaId, Long collaboratoreId);

    /**
     * Variante sicura: la pratica deve appartenere al commercialista richiedente
     * e il collaboratore deve far parte del suo studio (invito ACCEPTED).
     */
    void assegnaCollaboratore(Long praticaId, Long collaboratoreId, Utente richiedente);

    List<Pratica> trovaPerStato(StatoPratica stato);

    /** Cancellazione logica della pratica. */
    void eliminaPratica(Long id);

    /** Variante sicura: la pratica deve appartenere al commercialista richiedente. */
    void eliminaPratica(Long id, Utente richiedente);
}