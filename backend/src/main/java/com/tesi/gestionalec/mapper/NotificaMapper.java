package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.response.NotificaResponse;
import com.tesi.gestionalec.model.Notifica;

/**
 * Converte l'entità {@link Notifica} nel relativo DTO di risposta.
 */
public class NotificaMapper {

    // Da entità a DTO di risposta
    public static NotificaResponse toResponse(Notifica notifica) {
        NotificaResponse dto = new NotificaResponse();
        dto.setId(notifica.getId());
        dto.setMessaggio(notifica.getMessaggio());
        dto.setTipo(notifica.getTipo());
        dto.setLetta(notifica.isLetta());
        dto.setDataCreazione(notifica.getDataCreazione());
        return dto;
    }
}
