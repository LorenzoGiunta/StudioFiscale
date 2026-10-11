package com.tesi.gestionalec.repository;

import com.tesi.gestionalec.model.TokenResetPassword;
import com.tesi.gestionalec.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository dei token di recupero password: ricerca per valore, invalidazione
 * dei token pendenti di un utente e pulizia di quelli scaduti.
 */
public interface TokenResetPasswordRepo extends JpaRepository<TokenResetPassword, Long> {

    Optional<TokenResetPassword> findByToken(String token);

    /** Elimina i token non ancora usati dell'utente: ne resta valido uno solo alla volta. */
    long deleteByUtenteAndUsatoIlIsNull(Utente utente);

    /** Pulizia periodica dei token la cui scadenza è già passata. */
    long deleteByScadeIlBefore(LocalDateTime istante);
}
