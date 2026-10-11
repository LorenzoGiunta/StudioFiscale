package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Corpo (facoltativo) della richiesta con cui l'amministratore rifiuta
 * un'iscrizione: la motivazione, se presente, viene riportata all'utente
 * nella notifica di rifiuto.
 */
@Data
public class RifiutoRichiestaRequest {

    @Size(max = 500, message = "La motivazione non può superare i 500 caratteri")
    private String motivazione;
}
