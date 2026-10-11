package com.tesi.gestionalec.exception;

/**
 * Segnala l'uso di un token di recupero password inesistente, già consumato o
 * scaduto. Il messaggio è deliberatamente generico: non distingue i tre casi,
 * per non dare indizi a chi provasse token a caso. Tradotta in HTTP 400.
 */
public class TokenResetNonValidoException extends RuntimeException {

    public TokenResetNonValidoException() {
        super("Il link per reimpostare la password non è valido o è scaduto. Richiedine uno nuovo.");
    }
}
