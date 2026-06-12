package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Amministratore;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository degli amministratori, con le operazioni CRUD di base. */
public interface AmministratoreRepo extends JpaRepository<Amministratore, Long> {
}
