package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.response.InvitoResponse;
import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.InvitoCollaborazione;

/**
 * Converte l'entità {@link InvitoCollaborazione} nel relativo DTO di risposta,
 * appiattendo i dati di commercialista ed eventuale collaboratore destinatario.
 */
public class InvitoMapper {

    private InvitoMapper() {}

    public static InvitoResponse toResponse(InvitoCollaborazione invito) {
        InvitoResponse dto = new InvitoResponse();

        dto.setId(invito.getId());
        dto.setToken(invito.getToken());
        dto.setEmailDestinatario(invito.getEmailDestinatario());
        dto.setCommercialistaId(invito.getCommercialista().getId());
        dto.setNomeCommercialista(
                invito.getCommercialista().getNome() + " " + invito.getCommercialista().getCognome()
        );
        dto.setStudioCommercialista(invito.getCommercialista().getNumeroAlbo());

        // Il collaboratore manca finché il destinatario non risulta registrato
        Collaboratore collab = invito.getCollaboratore();
        if (collab != null) {
            dto.setCollaboratoreId(collab.getId());
            dto.setNomeCollaboratore(collab.getNome() + " " + collab.getCognome());
        }

        dto.setStato(invito.getStato());
        dto.setCreatoIl(invito.getCreatoIl());
        dto.setScadeIl(invito.getScadeIl());

        return dto;
    }
}
