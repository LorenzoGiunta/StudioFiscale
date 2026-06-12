package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.Collaboratore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository dei collaboratori, con la ricerca per email oltre alle operazioni CRUD. */
public interface CollaboratoreRepo extends JpaRepository<Collaboratore, Long> {

    Optional<Collaboratore> findByEmail(String email);
}
