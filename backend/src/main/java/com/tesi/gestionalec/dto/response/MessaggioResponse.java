package com.tesi.gestionalec.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Risposta minimale con un solo messaggio descrittivo per il client. */
@Data
@AllArgsConstructor
public class MessaggioResponse {
    private String message;
}
