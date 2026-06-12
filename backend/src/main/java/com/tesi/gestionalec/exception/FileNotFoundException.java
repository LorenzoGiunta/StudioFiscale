package com.tesi.gestionalec.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Segnala che un file richiesto non è presente nello storage o non è
 * leggibile. Conserva il percorso interessato e viene tradotta in una
 * risposta HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileNotFoundException extends RuntimeException {

    private final String percorso;

    public FileNotFoundException(String percorso) {
        super(String.format("File non trovato o non leggibile: '%s'", percorso));
        this.percorso = percorso;
    }

    public String getPercorso() {
        return percorso;
    }
}
