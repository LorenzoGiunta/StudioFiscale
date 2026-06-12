package com.tesi.gestionalec.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Segnala il tentativo di inviare un invito a un indirizzo per cui lo stesso
 * commercialista ha già un invito in attesa. Conserva l'email del destinatario
 * e viene tradotta in una risposta HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateInviteException extends RuntimeException {

    private final String emailDestinatario;

    public DuplicateInviteException(String emailDestinatario) {
        super(String.format(
                "Esiste già un invito in attesa (PENDING) per l'email: '%s'.", emailDestinatario
        ));
        this.emailDestinatario = emailDestinatario;
    }

    public String getEmailDestinatario() {
        return emailDestinatario;
    }
}
