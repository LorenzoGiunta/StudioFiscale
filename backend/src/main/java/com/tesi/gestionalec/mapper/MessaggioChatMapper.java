package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.response.MessaggioChatResponse;
import com.tesi.gestionalec.model.MessaggioChat;

/**
 * Converte l'entità {@link MessaggioChat} nel relativo DTO di risposta,
 * riducendo mittente e destinatario a identificativo e nome leggibile.
 */
public class MessaggioChatMapper {

    public static MessaggioChatResponse toResponse(MessaggioChat m) {
        MessaggioChatResponse r = new MessaggioChatResponse();
        r.setId(m.getId());
        r.setMittenteId(m.getMittente().getId());
        r.setMittenteNome(m.getMittente().getNome() + " " + m.getMittente().getCognome());
        r.setDestinatarioId(m.getDestinatario().getId());
        r.setDestinatarioNome(m.getDestinatario().getNome() + " " + m.getDestinatario().getCognome());
        r.setTesto(m.getTesto());
        r.setLetto(m.isLetto());
        r.setDataInvio(m.getDataInvio());
        return r;
    }
}