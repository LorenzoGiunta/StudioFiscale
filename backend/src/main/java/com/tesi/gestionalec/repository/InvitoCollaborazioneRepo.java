package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.InvitoCollaborazione;
import com.tesi.gestionalec.model.StatoInvito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository degli inviti di collaborazione.
 *
 * Le query caricano contestualmente commercialista ed eventuale collaboratore
 * per renderli disponibili al mapping fuori transazione ed evitare il problema
 * delle query N+1 nelle viste degli utenti collegati.
 */
public interface InvitoCollaborazioneRepo extends JpaRepository<InvitoCollaborazione, Long> {

    Optional<InvitoCollaborazione> findByToken(String token);

    // Carica contestualmente commercialista e collaboratore, relazioni differite
    @Query("""
            SELECT i FROM InvitoCollaborazione i
            JOIN FETCH i.commercialista
            LEFT JOIN FETCH i.collaboratore
            WHERE i.commercialista.id = :commercialistaId
            """)
    List<InvitoCollaborazione> findByCommercialista_Id(@Param("commercialistaId") Long commercialistaId);

    @Query("""
            SELECT i FROM InvitoCollaborazione i
            JOIN FETCH i.commercialista
            LEFT JOIN FETCH i.collaboratore
            WHERE i.emailDestinatario = :email AND i.stato = :stato
            """)
    List<InvitoCollaborazione> findByEmailDestinatarioAndStato(@Param("email") String email, @Param("stato") StatoInvito stato);

    boolean existsByCommercialista_IdAndEmailDestinatarioAndStato(
            Long commercialistaId, String emailDestinatario, StatoInvito stato);

    // Verifica che un collaboratore appartenga allo studio del commercialista
    // (esiste un invito ACCEPTED che li lega). Usata negli ownership check delle
    // assegnazioni per impedire di assegnare collaboratori di altri studi.
    boolean existsByCommercialista_IdAndCollaboratore_IdAndStato(
            Long commercialistaId, Long collaboratoreId, StatoInvito stato);

    // Usata dallo scheduler per marcare gli inviti scaduti
    List<InvitoCollaborazione> findByStatoAndScadeIlBefore(StatoInvito stato, LocalDateTime soglia);

    // Caricamento del collaboratore per evitare query N+1 sulla vista dei collaboratori attivi
    @Query("""
            SELECT i FROM InvitoCollaborazione i
            JOIN FETCH i.collaboratore
            WHERE i.commercialista.id = :commId
              AND i.stato = 'ACCEPTED'
            """)
    List<InvitoCollaborazione> findCollaboratoriAttiviByCommercialista(@Param("commId") Long commId);

    // Vista speculare: i commercialisti con cui un collaboratore ha una collaborazione attiva
    @Query("""
            SELECT i FROM InvitoCollaborazione i
            JOIN FETCH i.commercialista
            WHERE i.collaboratore.id = :collabId
              AND i.stato = 'ACCEPTED'
            """)
    List<InvitoCollaborazione> findCommercialistiAttiviByCollaboratore(@Param("collabId") Long collabId);
}
