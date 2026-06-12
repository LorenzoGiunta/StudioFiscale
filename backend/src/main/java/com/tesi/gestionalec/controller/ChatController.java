package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.MessaggioChatRequest;
import com.tesi.gestionalec.dto.response.MessaggioChatResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.model.Ruolo;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.security.UtentePrincipal;
import com.tesi.gestionalec.service.interfaces.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Controller per la messaggistica interna.
 *
 * Affianca due canali: uno WebSocket/STOMP per la consegna dei messaggi in
 * tempo reale e uno REST per lo storico, i contatti e la gestione dei messaggi
 * non letti. L'amministratore è escluso dalla chat.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    // Invio in tempo reale via WebSocket. Il mittente è ricavato dal principal
    // STOMP stabilito al CONNECT: nei metodi @MessageMapping non è disponibile
    // l'iniezione automatica del principal autenticato.
    @MessageMapping("/chat.invia")
    public void inviaMessaggio(@Valid @Payload MessaggioChatRequest request,
            Principal principal) {
        if (!(principal instanceof UtentePrincipal up)) {
            throw new AccessDeniedException("Sessione WebSocket non autenticata");
        }
        Utente mittente = up.utente();
        if (mittente.getRuolo() == Ruolo.AMMINISTRATORE) {
            throw new AccessDeniedException("L'amministratore non può usare la chat");
        }

        MessaggioChatResponse response = chatService.salvaEInvia(request, mittente);

        // Recapito immediato del messaggio al destinatario
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getDestinatarioId()),
                "/queue/messaggi",
                response);
    }

    // REST — recupera storico messaggi
    @GetMapping("/storico/{altroUtenteId}")
    public ResponseEntity<List<MessaggioChatResponse>> storico(
            @AuthenticationPrincipal Utente utente,
            @PathVariable Long altroUtenteId) {
        return ResponseEntity.ok(chatService.storico(utente.getId(), altroUtenteId));
    }

    // REST — restituisce i contatti con cui l'utente può chattare, filtrati per
    // ruolo
    @GetMapping("/contatti")
    public ResponseEntity<List<UtenteResponse>> contatti(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(chatService.trovaContatti(utente));
    }

    // REST — conteggio messaggi non letti per ogni mittente { mittenteId: count }
    @GetMapping("/non-letti")
    public ResponseEntity<Map<Long, Long>> nonLetti(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(chatService.nonLettiPerMittente(utente.getId()));
    }

    // REST — segna come letti i messaggi ricevuti da un dato contatto
    @PutMapping("/{altroUtenteId}/letti")
    public ResponseEntity<Void> segnaLetti(@AuthenticationPrincipal Utente utente,
            @PathVariable Long altroUtenteId) {
        chatService.segnaLetti(utente.getId(), altroUtenteId);
        return ResponseEntity.ok().build();
    }
}