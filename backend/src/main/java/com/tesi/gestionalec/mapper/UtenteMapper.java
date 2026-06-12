package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.model.Utente;

/**
 * Converte l'entità {@link Utente} nel relativo DTO di risposta, esponendo i
 * dati anagrafici essenziali insieme a ruolo e stato dell'account.
 */
public class UtenteMapper {

    public static UtenteResponse toResponse(Utente u){
        UtenteResponse dto = new UtenteResponse();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setCognome(u.getCognome());
        dto.setEmail(u.getEmail());
        dto.setRuolo(u.getRuolo());
        dto.setEnabled(u.isEnabled());
        dto.setDeleted(u.isDeleted());
        return dto;
    }
}
