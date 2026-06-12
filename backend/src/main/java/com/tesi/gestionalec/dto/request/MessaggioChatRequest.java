package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Dati per l'invio di un messaggio di chat: destinatario e testo. */
@Data
public class MessaggioChatRequest {
    @NotNull(message = "Il destinatario è obbligatorio")
    private Long destinatarioId;

    @NotBlank(message = "Il testo del messaggio è obbligatorio")
    private String testo;
}