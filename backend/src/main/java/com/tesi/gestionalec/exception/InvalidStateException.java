package com.tesi.gestionalec.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Segnala un'operazione incompatibile con lo stato corrente di una risorsa,
 * come l'avanzamento di una pratica già conclusa o la revoca di un invito
 * scaduto. Viene tradotta in una risposta HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String resourceName, String currentState, String operation) {
        super(String.format(
                "Operazione '%s' non consentita su '%s' nello stato '%s'.",
                operation, resourceName, currentState
        ));
    }
}
