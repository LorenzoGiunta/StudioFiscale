package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.response.ClienteResponse;
import com.tesi.gestionalec.model.Cliente;

/**
 * Converte l'entità {@link Cliente} nel relativo DTO di risposta, esponendo solo
 * i dati necessari al client e riducendo il riferimento al commercialista a
 * identificativo e nome.
 */
public class ClienteMapper {

    public static ClienteResponse toResponse(Cliente c) {
        ClienteResponse dto = new ClienteResponse();
        dto.setId(c.getId());
        dto.setNome(c.getNome());
        dto.setCognome(c.getCognome());
        dto.setEmail(c.getEmail());
        dto.setEnabled(c.isEnabled());
        dto.setCodFiscale(c.getCodFiscale());
        dto.setPartitaIva(c.getPIVA());
        dto.setRegime(c.getRegime());
        dto.setRedditoAnnuo(c.getRedditoAnnuo());
        if (c.getCommercialista() != null) {
            dto.setCommercialistaId(c.getCommercialista().getId());
            dto.setNomeCommercialista(
                c.getCommercialista().getNome() + " " + c.getCommercialista().getCognome());
        }
        return dto;
    }
}
