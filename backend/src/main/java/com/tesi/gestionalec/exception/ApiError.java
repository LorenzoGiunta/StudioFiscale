package com.tesi.gestionalec.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Rappresentazione uniforme delle risposte di errore esposte dall'API.
 *
 * Tutte le eccezioni gestite vengono serializzate in questo formato, che
 * riporta l'istante, il codice di stato, una descrizione sintetica, il messaggio
 * e il percorso della richiesta, senza mai includere stacktrace o dettagli
 * interni. I campi nulli sono omessi dalla serializzazione.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /**
     * Valorizzato solo per gli errori di validazione: associa a ciascun campo
     * non valido il relativo messaggio. Negli altri casi resta nullo e viene
     * quindi omesso dalla risposta.
     */
    private Map<String, String> campiInvalidi;

    public ApiError(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Metodi di accesso utilizzati per la serializzazione JSON

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getCampiInvalidi() {
        return campiInvalidi;
    }

    public void setCampiInvalidi(Map<String, String> campiInvalidi) {
        this.campiInvalidi = campiInvalidi;
    }
}
