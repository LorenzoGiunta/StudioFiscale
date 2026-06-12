package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Credenziali inviate per l'autenticazione: email e password. */
@Data
public class LoginRequest {

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    private String password;
}
