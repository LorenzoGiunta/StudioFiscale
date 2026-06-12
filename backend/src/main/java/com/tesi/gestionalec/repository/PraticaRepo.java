package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoPratica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository delle pratiche, con le ricerche per cliente, per collaboratore
 * assegnatario e per stato, oltre alla consultazione paginata.
 */
public interface PraticaRepo extends JpaRepository<Pratica, Long> {
    List<Pratica> findByCliente(Cliente cliente);
    List<Pratica> findByAssegnataA(Collaboratore c);
    List<Pratica> findByStato(StatoPratica stato);
    Page<Pratica> findAll(Pageable pageable);
}
