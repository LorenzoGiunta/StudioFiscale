package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.dto.request.RegistrazioneRequest;
import com.tesi.gestionalec.model.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contratto comune per la gestione degli utenti, base delle interfacce
 * specifiche dei singoli ruoli.
 *
 * Raccoglie registrazione, ricerca, validazione per ruolo e le operazioni
 * amministrative sul ciclo di vita degli account, inclusa la cancellazione
 * logica e il relativo ripristino.
 */
public interface UtenteService {

    Utente registra(Utente utente);

    /** Registra l'utente associandolo, se previsto, al commercialista indicato. */
    Utente registra(Utente utente, Long commercialistaId);

    /** Verifica i campi obbligatori che dipendono dal ruolo richiesto. */
    void validaPerRuolo(RegistrazioneRequest request);

    Utente trovaPerId(Long id);

    /** Ricerca per email, utilizzata in fase di autenticazione. */
    Utente trovaPerEmail(String email);

    List<Utente> trovaTutti();

    Page<Utente> trovaTutti(Pageable pageable);

    void abilitaUtente(Long id);

    void disabilitaUtente(Long id);

    /** Cancellazione logica dell'utente. */
    void eliminaUtente(Long id);

    /** Annulla la cancellazione logica di un utente. */
    void ripristinaUtente(Long id);

    /** Elenco degli utenti cancellati logicamente. */
    List<Utente> trovaEliminati();
}
