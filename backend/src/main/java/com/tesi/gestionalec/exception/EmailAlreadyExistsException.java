package com.tesi.gestionalec.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Segnala il tentativo di registrare un utente con un'email già presente nel
 * sistema. Conserva l'indirizzo in conflitto e viene tradotta in una risposta
 * HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyExistsException extends RuntimeException {

    private final String email;

    public EmailAlreadyExistsException(String email) {
        super(String.format("L'email '%s' è già registrata nel sistema.", email));
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
