package com.tesi.gestionalec.dto.response;

import com.tesi.gestionalec.model.StatoPratica;
import com.tesi.gestionalec.model.TipoPratica;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO di risposta con i dati essenziali di una pratica; cliente e collaboratore
 * sono rappresentati dal solo nome anziché dall'intera entità.
 */
@Data
public class PraticaResponse {
    private Long id;
    private TipoPratica tipoPratica;
    private StatoPratica stato;
    private LocalDateTime dataCreazione;
    private LocalDate scadenza;
    private String nomeCliente;
    private String nomeCollaboratore;   // assente finché la pratica non è assegnata
}