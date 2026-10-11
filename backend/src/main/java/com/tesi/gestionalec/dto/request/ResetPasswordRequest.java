package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Reimpostazione della password tramite il token ricevuto via email. La nuova
 * password è soggetta agli stessi requisiti di robustezza della registrazione.
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Il token è obbligatorio")
    private String token;

    @NotBlank(message = "La nuova password è obbligatoria")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,}$",
             message = "La password deve contenere almeno 8 caratteri, una lettera maiuscola, una minuscola, un numero e un carattere speciale")
    private String nuovaPassword;
}
