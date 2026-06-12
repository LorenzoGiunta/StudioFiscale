package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Dato per l'emissione di un invito: l'email del collaboratore destinatario. */
@Data
public class InvitoRequest {

    @NotBlank(message = "L'email del destinatario è obbligatoria")
    @Email(message = "Formato email non valido")
    private String emailDestinatario;
}
