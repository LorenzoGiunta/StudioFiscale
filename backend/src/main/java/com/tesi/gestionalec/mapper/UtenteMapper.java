package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.response.RichiestaIscrizioneResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.model.Commercialista;
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
        dto.setStatoRegistrazione(u.getStatoRegistrazione());
        return dto;
    }

    /**
     * Vista di un utente come richiesta di iscrizione da verificare: aggiunge
     * la data della richiesta e il numero di albo (per i commercialisti), che è
     * il dato che l'amministratore controlla a mano.
     */
    public static RichiestaIscrizioneResponse toRichiestaIscrizione(Utente u) {
        RichiestaIscrizioneResponse dto = new RichiestaIscrizioneResponse();
        dto.setId(u.getId());
        dto.setNome(u.getNome());
        dto.setCognome(u.getCognome());
        dto.setEmail(u.getEmail());
        dto.setRuolo(u.getRuolo());
        dto.setDataRegistrazione(u.getDataRegistrazione());
        dto.setStatoRegistrazione(u.getStatoRegistrazione());
        if (u instanceof Commercialista c) {
            dto.setNumeroAlbo(c.getNumeroAlbo());
        }
        return dto;
    }
}
