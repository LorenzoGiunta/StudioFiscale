package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.mapper.PraticaMapper;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.CollaboratoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dell'area riservata al collaboratore.
 *
 * Consente di consultare le pratiche assegnate e i documenti da revisionare e
 * di
 * esprimere l'esito della revisione, approvando o rifiutando con motivazione.
 * L'accesso è riservato agli utenti con ruolo collaboratore.
 */
@RestController
@RequestMapping("/api/collaboratore")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_COLLABORATORE')")
public class CollaboratoreController {

    private final CollaboratoreService collaboratoreService;

    @GetMapping("/pratiche")
    public ResponseEntity<List<PraticaResponse>> miePratiche(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(collaboratoreService.trovaPraticheAssegnate(utente.getId())
                .stream()
                .map(PraticaMapper::toResponse)
                .toList());
    }

    @GetMapping("/documenti")
    public ResponseEntity<List<DocumentoResponse>> mieRevisioni(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(collaboratoreService.trovaDocumentiInRevisione(utente.getId())
                .stream()
                .map(DocumentoMapper::toResponse)
                .toList());
    }

    @PutMapping("/{id}/approva")
    public ResponseEntity<Void> approva(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        collaboratoreService.approvaDocumento(id, utente.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/rifiuta")
    public ResponseEntity<Void> rifiuta(@PathVariable Long id,
            @RequestParam String motivazione,
            @AuthenticationPrincipal Utente utente) {
        collaboratoreService.rifiutaDocumento(id, motivazione, utente.getId());
        return ResponseEntity.ok().build();
    }
}