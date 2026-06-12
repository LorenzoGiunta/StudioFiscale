package com.tesi.gestionalec.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * DTO di risposta con le metriche aggregate di sistema (utenti, pratiche e
 * documenti) presentate nella dashboard dell'amministratore.
 */
@Data
public class StatisticheResponse {
    private long utentiTotali;
    private long utentiAbilitati;
    private long utentiDisabilitati;
    private long utentiEliminati;
    private Map<String, Long> utentiPerRuolo;

    private long praticheTotali;
    private Map<String, Long> pratichePerStato;

    private long documentiTotali;
    private Map<String, Long> documentiPerStato;
}
