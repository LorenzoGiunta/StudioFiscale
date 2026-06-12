package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.response.NotificaResponse;
import com.tesi.gestionalec.mapper.NotificaMapper;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.NotificaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la consultazione delle notifiche dell'utente autenticato.
 *
 * Restituisce le notifiche in forma paginata, con le più recenti in testa, sia
 * complete sia limitate a quelle non lette, e consente di marcarle come lette
 * singolarmente o tutte insieme.
 */
@RestController
@RequestMapping("/api/notifiche")
@RequiredArgsConstructor
public class NotificaController {

    private final NotificaService notificaService;

    /** Notifiche dell'utente autenticato, paginate e ordinate dalla più recente. */
    @GetMapping("/mie")
    public ResponseEntity<Page<NotificaResponse>> mie(
            @AuthenticationPrincipal Utente utente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<NotificaResponse> risultato = notificaService.trovaPerUtente(utente, pageable)
                .map(NotificaMapper::toResponse);
        return ResponseEntity.ok(risultato);
    }

    /** Solo le notifiche non ancora lette, in forma paginata. */
    @GetMapping("/non-lette")
    public ResponseEntity<Page<NotificaResponse>> nonLette(
            @AuthenticationPrincipal Utente utente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<NotificaResponse> risultato = notificaService.trovaNonLette(utente, pageable)
                .map(NotificaMapper::toResponse);
        return ResponseEntity.ok(risultato);
    }

    @PutMapping("/{id}/letta")
    public ResponseEntity<Void> segnaComeLetta(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: solo il destinatario può segnare la propria notifica
        notificaService.segnaComeLetta(id, utente);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/letta-tutte")
    public ResponseEntity<Void> segnaComeLetteTutte(@AuthenticationPrincipal Utente utente) {
        notificaService.segnaComeLetteTutte(utente.getId());
        return ResponseEntity.ok().build();
    }
}