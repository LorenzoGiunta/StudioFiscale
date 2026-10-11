package com.tesi.gestionalec.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO di risposta dell'autenticazione: contiene il token JWT e i dati essenziali
 * dell'utente, restituiti sia al login sia alla registrazione.
 *
 * Quando l'iscrizione richiede la verifica di un amministratore il token è
 * assente e {@code statoRegistrazione} vale {@code IN_ATTESA}: il client non
 * deve autenticare l'utente ma informarlo che la richiesta è stata presa in
 * carico.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long id;
    private String ruolo;
    private String email;
    private String nome;
    private String cognome;
    private String statoRegistrazione;
}
