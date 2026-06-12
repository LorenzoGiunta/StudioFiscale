package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.InvitoRequest;
import com.tesi.gestionalec.dto.response.InvitoResponse;
import com.tesi.gestionalec.mapper.InvitoMapper;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.InvitoCollaborazioneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller per la gestione degli inviti di collaborazione.
 *
 * Lato commercialista consente di emettere, elencare e revocare gli inviti;
 * lato collaboratore di consultare quelli ricevuti e di accettarli. Il rifiuto
 * è
 * accessibile anche senza autenticazione, tramite il link diretto inviato via
 * email.
 */
@RestController
@RequestMapping("/api/inviti")
@RequiredArgsConstructor
public class InvitoCollaborazioneController {

    private final InvitoCollaborazioneService invitoService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<InvitoResponse> invita(
            @AuthenticationPrincipal Utente utente,
            @Valid @RequestBody InvitoRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(InvitoMapper.toResponse(
                        invitoService.invita(utente.getId(), request.getEmailDestinatario())));
    }

    @GetMapping("/miei")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<List<InvitoResponse>> miei(@AuthenticationPrincipal Utente utente) {
        List<InvitoResponse> lista = invitoService.trovaPerCommercialista(utente.getId())
                .stream()
                .map(InvitoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> revoca(
            @AuthenticationPrincipal Utente utente,
            @PathVariable Long id) {

        invitoService.revoca(id, utente.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_COLLABORATORE')")
    public ResponseEntity<List<InvitoResponse>> pending(@AuthenticationPrincipal Utente utente) {
        List<InvitoResponse> lista = invitoService.trovaPendingPerEmail(utente.getEmail())
                .stream()
                .map(InvitoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/accettati")
    @PreAuthorize("hasAuthority('ROLE_COLLABORATORE')")
    public ResponseEntity<List<InvitoResponse>> accettati(@AuthenticationPrincipal Utente utente) {
        List<InvitoResponse> lista = invitoService.trovaAccettatiPerEmail(utente.getEmail())
                .stream()
                .map(InvitoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{token}/accetta")
    @PreAuthorize("hasAuthority('ROLE_COLLABORATORE')")
    public ResponseEntity<Void> accetta(
            @PathVariable String token,
            @AuthenticationPrincipal Utente utente) {

        invitoService.accetta(token, utente.getId());
        return ResponseEntity.ok().build();
    }

    // Endpoint pubblico raggiungibile dal link nell'email, senza autenticazione
    @PostMapping("/{token}/rifiuta")
    public ResponseEntity<Void> rifiuta(@PathVariable String token) {
        invitoService.rifiuta(token);
        return ResponseEntity.ok().build();
    }
}
