package com.tesi.gestionalec.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Segnala il tentativo di un utente autenticato di operare su una risorsa che
 * non gli appartiene o di cui non è il destinatario legittimo.
 *
 * Rappresenta un controllo di proprietà a livello applicativo, distinto dalla
 * verifica del ruolo, e viene tradotta in una risposta HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
