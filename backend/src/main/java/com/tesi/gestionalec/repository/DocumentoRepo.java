package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository dei documenti, con le ricerche per pratica, per stato di revisione,
 * per cliente che ha effettuato il caricamento e per commercialista dello studio.
 */
public interface DocumentoRepo extends JpaRepository<Documento, Long> {
    List<Documento> findByPratica(Pratica pratica);
    List<Documento> findByStato(StatoDocumento stato);
    List<Documento> findByCaricatoDa(Cliente cliente);

    /**
     * Documenti il cui cliente appartiene al commercialista indicato.
     * Percorre la relazione Documento → Pratica → Cliente → Commercialista.
     */
    @Query("SELECT d FROM Documento d " +
           "WHERE d.pratica.cliente.commercialista.id = :commercialistaId " +
           "AND d.deleted = false")
    List<Documento> findByCommercialista(@Param("commercialistaId") Long commercialistaId);
}
