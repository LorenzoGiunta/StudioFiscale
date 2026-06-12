package com.tesi.gestionalec.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tesi.gestionalec.model.RegimeFiscale;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Dati di aggiornamento del profilo di un cliente. La modifica della password non
 * è inclusa, essendo gestita da un endpoint dedicato.
 */
@Data
public class ClienteUpdateRequest {

    @NotBlank(message = "Il nome e' obbligatorio")
    private String nome;

    @NotBlank(message = "Il cognome e' obbligatorio")
    private String cognome;

    @NotBlank(message = "L'email e' obbligatoria")
    @Email(message = "Email non valida")
    private String email;

    @Pattern(regexp = "^$|^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$", message = "Codice fiscale non valido")
    private String codFiscale;

    @Pattern(regexp = "^$|^[0-9]{11}$", message = "Partita IVA non valida")
    @JsonProperty("pIVA")   // chiave JSON "pIVA" attesa dal frontend (vedi ClienteResponse)
    private String partitaIva;

    private RegimeFiscale regime;

    @PositiveOrZero(message = "Il reddito annuo non puo' essere negativo")
    private Double redditoAnnuo;
}
