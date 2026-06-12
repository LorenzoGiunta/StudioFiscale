package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.MessaggioChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repository dei messaggi di chat, con le query per la conversazione tra due
 * utenti, il conteggio dei messaggi non letti e la loro marcatura come letti.
 */
public interface MessaggioChatRepo extends JpaRepository<MessaggioChat, Long> {

    // Tutti i messaggi scambiati tra due utenti, ordinati cronologicamente
    @Query("""
        SELECT m FROM MessaggioChat m
        WHERE (m.mittente.id = :utenteA AND m.destinatario.id = :utenteB)
           OR (m.mittente.id = :utenteB AND m.destinatario.id = :utenteA)
        ORDER BY m.dataInvio ASC
    """)
    List<MessaggioChat> trovaStotico(Long utenteA, Long utenteB);

    // Messaggi non letti ricevuti, raggruppati per mittente: ogni riga [mittenteId, count]
    @Query("""
        SELECT m.mittente.id, COUNT(m) FROM MessaggioChat m
        WHERE m.destinatario.id = :utenteId AND m.letto = false
        GROUP BY m.mittente.id
    """)
    List<Object[]> contaNonLettiPerMittente(Long utenteId);

    // Segna come letti i messaggi ricevuti da un dato mittente
    @Modifying
    @Query("""
        UPDATE MessaggioChat m SET m.letto = true
        WHERE m.destinatario.id = :me AND m.mittente.id = :altro AND m.letto = false
    """)
    int segnaLetti(Long me, Long altro);
}