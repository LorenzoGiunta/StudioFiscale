package com.tesi.gestionalec.dto.response;

import com.tesi.gestionalec.model.TipoNotifica;
import lombok.Data;

import java.time.LocalDateTime;

/** DTO di risposta che descrive una notifica destinata all'utente. */
@Data
public class NotificaResponse {
    private Long id;
    private String messaggio;
    private TipoNotifica tipo;
    private boolean letta;
    private LocalDateTime dataCreazione;
}