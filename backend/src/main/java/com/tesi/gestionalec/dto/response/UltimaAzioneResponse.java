package com.tesi.gestionalec.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO di risposta con l'istante dell'ultima operazione amministrativa svolta
 * dall'amministratore autenticato. Vale {@code null} finché non ne ha compiuta
 * alcuna.
 */
@Data
@AllArgsConstructor
public class UltimaAzioneResponse {
    private LocalDateTime ultimaAzione;
}
