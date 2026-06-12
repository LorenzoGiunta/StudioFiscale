package com.tesi.gestionalec.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO di risposta dell'autenticazione: contiene il token JWT e i dati essenziali
 * dell'utente, restituiti sia al login sia alla registrazione.
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
}
