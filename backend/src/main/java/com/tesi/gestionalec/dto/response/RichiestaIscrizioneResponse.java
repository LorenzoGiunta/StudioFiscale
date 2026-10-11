package com.tesi.gestionalec.dto.response;

import com.tesi.gestionalec.model.Ruolo;
import com.tesi.gestionalec.model.StatoRegistrazione;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO di risposta per una richiesta di iscrizione in attesa di verifica.
 * Oltre ai dati anagrafici espone il numero di albo, che è l'informazione che
 * l'amministratore deve controllare a mano prima di approvare.
 */
@Data
public class RichiestaIscrizioneResponse {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private Ruolo ruolo;
    private String numeroAlbo;              // valorizzato per i commercialisti
    private LocalDateTime dataRegistrazione;
    private StatoRegistrazione statoRegistrazione;
}
