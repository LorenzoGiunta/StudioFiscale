package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.dto.request.MessaggioChatRequest;
import com.tesi.gestionalec.dto.response.MessaggioChatResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.model.Utente;

import java.util.List;
import java.util.Map;

/**
 * Contratto per la messaggistica interna: invio e storico dei messaggi,
 * individuazione dei contatti ammessi per ruolo e gestione dei messaggi non letti.
 */
public interface ChatService {
    MessaggioChatResponse salvaEInvia(MessaggioChatRequest request, Utente mittente);
    List<MessaggioChatResponse> storico(Long utenteAId, Long utenteBId);
    /** Restituisce i contatti con cui l'utente può chattare in base al suo ruolo. */
    List<UtenteResponse> trovaContatti(Utente utente);
    /** Messaggi non letti dell'utente raggruppati per id del mittente. */
    Map<Long, Long> nonLettiPerMittente(Long utenteId);
    /** Segna come letti i messaggi ricevuti da un certo mittente. */
    void segnaLetti(Long utenteId, Long altroUtenteId);
}