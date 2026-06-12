package com.tesi.gestionalec.dto.response;

import com.tesi.gestionalec.model.Ruolo;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/** DTO di risposta con i dati essenziali di un utente, ruolo e stato dell'account. */
@Data
@RequiredArgsConstructor
public class UtenteResponse {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private Ruolo ruolo;
    private boolean enabled;
    private boolean deleted;
}
