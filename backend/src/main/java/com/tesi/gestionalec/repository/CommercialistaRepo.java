package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Commercialista;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository dei commercialisti, con le operazioni CRUD di base. */
public interface CommercialistaRepo extends JpaRepository<Commercialista, Long> {
}
