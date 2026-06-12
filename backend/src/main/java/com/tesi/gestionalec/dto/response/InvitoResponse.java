package com.tesi.gestionalec.dto.response;

import com.tesi.gestionalec.model.StatoInvito;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO di risposta che descrive un invito di collaborazione con i dati di
 * commercialista ed eventuale collaboratore destinatario.
 */
@Data
public class InvitoResponse {

    private Long id;
    private String token;
    private String emailDestinatario;

    private Long commercialistaId;
    private String nomeCommercialista;
    private String studioCommercialista;

    // Assenti finché il destinatario non risulta registrato
    private Long collaboratoreId;
    private String nomeCollaboratore;

    private StatoInvito stato;
    private LocalDateTime creatoIl;
    private LocalDateTime scadeIl;
}
