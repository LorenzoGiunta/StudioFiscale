package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.Utente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository delle notifiche, con le ricerche per destinatario — complete o
 * limitate a quelle non lette, anche in forma paginata — e il controllo di
 * esistenza usato per evitare notifiche di scadenza duplicate nella giornata.
 */
public interface NotificaRepo extends JpaRepository<Notifica, Long> {
    List<Notifica> findByDestinatario(Utente utente);
    List<Notifica> findByDestinatarioAndLettaFalse(Utente utente);

    // Varianti paginate delle ricerche per destinatario
    Page<Notifica> findByDestinatario(Utente utente, Pageable pageable);
    Page<Notifica> findByDestinatarioAndLettaFalse(Utente utente, Pageable pageable);

    /** Usato dallo scheduler per evitare notifiche scadenza duplicate nello stesso giorno. */
    boolean existsByDestinatarioAndTipoAndDataCreazioneBetween(
            Utente destinatario, TipoNotifica tipo,
            LocalDateTime from, LocalDateTime to);
}
