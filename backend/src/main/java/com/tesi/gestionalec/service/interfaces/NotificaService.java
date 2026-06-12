package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Contratto per la consultazione e la gestione delle notifiche destinate agli
 * utenti, disponibili sia in forma di elenco completo sia paginata.
 */
public interface NotificaService {
    List<Notifica> trovaPerUtente(Utente utente);
    Page<Notifica> trovaPerUtente(Utente utente, Pageable pageable);
    List<Notifica> trovaNonLette(Utente utente);
    Page<Notifica> trovaNonLette(Utente utente, Pageable pageable);
    void segnaComeLetta(Long notificaId);
    /** Variante sicura: verifica che notificaId appartenga all'utente loggato. */
    void segnaComeLetta(Long notificaId, Utente richiedente);
    void segnaComeLetteTutte(Long utenteId);
}