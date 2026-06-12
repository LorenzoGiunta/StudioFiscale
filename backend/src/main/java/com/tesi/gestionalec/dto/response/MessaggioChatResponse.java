package com.tesi.gestionalec.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/** DTO di risposta di un messaggio di chat, con i nomi leggibili di mittente e destinatario. */
@Data
public class MessaggioChatResponse {
    private Long id;
    private Long mittenteId;
    private String mittenteNome;
    private Long destinatarioId;
    private String destinatarioNome;
    private String testo;
    private boolean letto;
    private LocalDateTime dataInvio;
}