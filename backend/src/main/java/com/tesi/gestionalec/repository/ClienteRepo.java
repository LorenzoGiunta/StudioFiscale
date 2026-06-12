package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository dei clienti.
 *
 * Le query di ricerca caricano contestualmente il commercialista associato, così
 * da renderlo disponibile in fase di mapping anche al di fuori della transazione.
 */
public interface ClienteRepo extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCodFiscale(String codiceFiscale);

    // Carica contestualmente il commercialista, relazione altrimenti differita
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.commercialista WHERE c.commercialista.id = :commId")
    List<Cliente> findByCommercialistaId(@Param("commId") Long commercialistaId);

    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.commercialista WHERE c.id = :id")
    Optional<Cliente> findByIdConCommercialista(@Param("id") Long id);
}
