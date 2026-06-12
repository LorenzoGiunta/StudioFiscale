package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.InvitoCollaborazione;

import java.util.List;

/**
 * Contratto per la gestione del ciclo di vita degli inviti di collaborazione,
 * dall'emissione fino ad accettazione, rifiuto, revoca o scadenza.
 */
public interface InvitoCollaborazioneService {

    /**
     * Il Commercialista invita un collaboratore tramite la sua email.
     * Genera un token UUID, salva l'invito in stato PENDING e invia l'email.
     *
     * @param commercialistaId ID del commercialista autenticato
     * @param emailDestinatario email del collaboratore da invitare
     * @return l'entity salvata
     * @throws IllegalStateException se esiste già un invito PENDING per questa coppia
     */
    InvitoCollaborazione invita(Long commercialistaId, String emailDestinatario);

    /**
     * Il Collaboratore accetta un invito tramite il token nel link email.
     * Imposta stato ACCEPTED e collega il Collaboratore all'entità invito.
     *
     * @param token           UUID dell'invito
     * @param collaboratoreId ID del collaboratore autenticato che accetta
     */
    void accetta(String token, Long collaboratoreId);

    /**
     * Rifiuta un invito tramite il token nel link email.
     * Accessibile anche senza autenticazione (via link diretto).
     */
    void rifiuta(String token);

    /**
     * Revoca un'associazione già accettata (o cancella un invito PENDING).
     * Solo il Commercialista proprietario può eseguire questa operazione.
     *
     * @param invitoId         ID dell'invito da revocare
     * @param commercialistaId ID del commercialista richiedente (verifica ownership)
     */
    void revoca(Long invitoId, Long commercialistaId);

    /** Tutti gli inviti inviati da un commercialista (tutti gli stati) */
    List<InvitoCollaborazione> trovaPerCommercialista(Long commercialistaId);

    /** Inviti PENDING indirizzati all'email del collaboratore loggato */
    List<InvitoCollaborazione> trovaPendingPerEmail(String email);

    /** Inviti ACCEPTED indirizzati all'email del collaboratore — studi attivi */
    List<InvitoCollaborazione> trovaAccettatiPerEmail(String email);

    /**
     * Marca come scaduti gli inviti ancora in attesa oltre il termine previsto.
     * È invocato automaticamente da un processo schedulato notturno.
     */
    void scadenzaAutomatica();
}
