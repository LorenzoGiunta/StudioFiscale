package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Richiesta di recupero password: l'indirizzo a cui inviare il link di reimpostazione. */
@Data
public class RichiestaResetPasswordRequest {

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;
}
