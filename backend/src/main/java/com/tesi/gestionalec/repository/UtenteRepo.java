package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Repository degli utenti.
 *
 * Oltre alle ricerche ordinarie, fornisce alcune query native che ignorano
 * deliberatamente il filtro di esclusione dei record cancellati logicamente,
 * necessarie per la verifica di unicità dell'email e per il ripristino degli
 * account eliminati.
 */
public interface UtenteRepo extends JpaRepository<Utente, Long> {

    Optional<Utente> findByEmail(String email);

    // Conteggio per email che include anche gli utenti cancellati logicamente:
    // un'email occupata da un account eliminato mantiene comunque il vincolo di
    // unicità sul database. La query nativa scalare evita problemi di
    // idratazione dell'entità polimorfica.
    @Query(value = "SELECT COUNT(*) FROM utente WHERE email = :email", nativeQuery = true)
    long countByEmailIncludeDeleted(@Param("email") String email);

    // Recupera anche gli utenti cancellati logicamente (necessario per il
    // ripristino). Per via dell'ereditarietà mappata su tabelle distinte, la
    // query nativa deve recuperare con join anche le colonne delle sottoclassi,
    // altrimenti l'entità polimorfica non potrebbe essere ricostruita.
    @Query(value = """
            SELECT u.*, c.cod_fiscale, c.piva, c.regime, c.reddito_annuo, c.commercialista_id,
                   cm.numero_albo, a.ultima_azione_amministrativa
            FROM utente u
            LEFT JOIN cliente c         ON c.id = u.id
            LEFT JOIN commercialista cm ON cm.id = u.id
            LEFT JOIN amministratore a  ON a.id = u.id
            WHERE u.id = :id
            """, nativeQuery = true)
    Optional<Utente> findByIdIncludeDeleted(@Param("id") Long id);

    @Query(value = """
            SELECT u.*, c.cod_fiscale, c.piva, c.regime, c.reddito_annuo, c.commercialista_id,
                   cm.numero_albo, a.ultima_azione_amministrativa
            FROM utente u
            LEFT JOIN cliente c         ON c.id = u.id
            LEFT JOIN commercialista cm ON cm.id = u.id
            LEFT JOIN amministratore a  ON a.id = u.id
            WHERE u.deleted = true
            ORDER BY u.deleted_at DESC
            """, nativeQuery = true)
    List<Utente> findAllDeleted();
}
