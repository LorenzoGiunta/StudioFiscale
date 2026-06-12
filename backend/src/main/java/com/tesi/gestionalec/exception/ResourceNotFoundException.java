package com.tesi.gestionalec.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Segnala l'assenza nel database di una risorsa identificata da un certo
 * criterio. Conserva nome della risorsa, campo e valore ricercati per
 * comporre un messaggio descrittivo, e viene tradotta in una risposta
 * HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String nomeRisorsa;
    private final String nomeCampo;
    private final Object valoreCampo;

    public ResourceNotFoundException(String nomeRisorsa, String nomeCampo, Object valoreCampo) {
        super(String.format("%s non trovato/a con %s: '%s'", nomeRisorsa, nomeCampo, valoreCampo));
        this.nomeRisorsa = nomeRisorsa;
        this.nomeCampo = nomeCampo;
        this.valoreCampo = valoreCampo;
    }

    public String getNomeRisorsa() {
        return nomeRisorsa;
    }

    public String getNomeCampo() {
        return nomeCampo;
    }

    public Object getValoreCampo() {
        return valoreCampo;
    }
}
