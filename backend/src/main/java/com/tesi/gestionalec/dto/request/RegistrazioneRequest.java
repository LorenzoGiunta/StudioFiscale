package com.tesi.gestionalec.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tesi.gestionalec.model.Ruolo;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Dati inviati per la registrazione di un nuovo utente.
 *
 * Raccoglie i campi comuni a tutti i ruoli e quelli specifici di cliente e
 * commercialista, valorizzati solo quando pertinenti al ruolo selezionato. La
 * coerenza tra ruolo e campi obbligatori è verificata a livello di servizio.
 */
@Data
public class RegistrazioneRequest {
    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    private String cognome;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,}$", 
             message = "La password deve contenere almeno 8 caratteri, una lettera maiuscola, una minuscola, un numero e un carattere speciale")
    private String password;

    @NotNull(message = "Il ruolo è obbligatorio")
    private Ruolo ruolo;

    /** Solo per CLIENTE: id del commercialista scelto in fase di registrazione. */
    private Long commercialistaId;

    // Campi specifici del cliente, valorizzati solo per quel ruolo
    @Pattern(regexp = "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$", message = "Codice fiscale non valido")
    private String codFiscale;

    @Pattern(regexp = "^[0-9]{11}$", message = "Partita IVA non valida")
    @JsonProperty("pIVA")   // chiave JSON "pIVA" attesa dal frontend (vedi ClienteResponse)
    private String partitaIva;
    
    private String regime;

    @PositiveOrZero(message = "Il reddito annuo non può essere negativo")
    private Double redditoAnnuo;

    // Campo specifico del commercialista, valorizzato solo per quel ruolo
    private String numeroAlbo;
}
