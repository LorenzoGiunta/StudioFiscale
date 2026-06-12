package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.response.ClienteResponse;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.mapper.ClienteMapper;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.mapper.PraticaMapper;
import com.tesi.gestionalec.mapper.UtenteMapper;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.ClienteService;
import com.tesi.gestionalec.service.interfaces.CommercialistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dell'area riservata al commercialista.
 *
 * Raccoglie le operazioni di studio: calcolo delle imposte di un cliente,
 * consultazione dei propri clienti con relative pratiche e documenti, elenco
 * dei
 * collaboratori attivi e dei documenti dello studio. L'accesso è riservato agli
 * utenti con ruolo commercialista.
 */
@RestController
@RequestMapping("/api/commercialista")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
public class CommercialistaController {

    private final CommercialistaService commercialistaService;
    private final ClienteService clienteService;

    @GetMapping("/imposte/{clienteId}")
    public ResponseEntity<Double> calcolaImposte(@PathVariable Long clienteId,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: il cliente deve appartenere al commercialista loggato
        commercialistaService.verificaAppartenenzaCliente(clienteId, utente.getId());
        return ResponseEntity.ok(commercialistaService.calcolaImposteCliente(clienteId));
    }

    @GetMapping("/clienti")
    public ResponseEntity<List<ClienteResponse>> trovaClienti(@AuthenticationPrincipal Utente utente) {
        List<ClienteResponse> clienti = commercialistaService.trovaClientiDelCommercialista(utente.getId())
                .stream().map(ClienteMapper::toResponse).toList();
        return ResponseEntity.ok(clienti);
    }

    // Scheda singolo cliente (profilo fiscale)
    @GetMapping("/clienti/{id}")
    public ResponseEntity<ClienteResponse> trovaCliente(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: il cliente deve appartenere al commercialista loggato
        commercialistaService.verificaAppartenenzaCliente(id, utente.getId());
        return ResponseEntity.ok(ClienteMapper.toResponse(clienteService.trovaClientePerId(id)));
    }

    @GetMapping("/clienti/{id}/pratiche")
    public ResponseEntity<List<PraticaResponse>> praticheCliente(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: il cliente deve appartenere al commercialista loggato
        commercialistaService.verificaAppartenenzaCliente(id, utente.getId());
        List<PraticaResponse> pratiche = clienteService.trovaPratiche(id)
                .stream().map(PraticaMapper::toResponse).toList();
        return ResponseEntity.ok(pratiche);
    }

    @GetMapping("/clienti/{id}/documenti")
    public ResponseEntity<List<DocumentoResponse>> documentiCliente(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: il cliente deve appartenere al commercialista loggato
        commercialistaService.verificaAppartenenzaCliente(id, utente.getId());
        List<DocumentoResponse> documenti = clienteService.trovaDocumenti(id)
                .stream().map(DocumentoMapper::toResponse).toList();
        return ResponseEntity.ok(documenti);
    }

    // Solo i collaboratori con collaborazione accettata del commercialista
    // autenticato
    @GetMapping("/collaboratori")
    public ResponseEntity<List<UtenteResponse>> mieiCollaboratori(@AuthenticationPrincipal Utente utente) {
        List<UtenteResponse> collaboratori = commercialistaService.trovaMieiCollaboratori(utente.getId())
                .stream().map(UtenteMapper::toResponse).toList();
        return ResponseEntity.ok(collaboratori);
    }

    @GetMapping("/documenti")
    public ResponseEntity<List<DocumentoResponse>> documentiStudio(@AuthenticationPrincipal Utente utente) {
        // Ownership: solo i documenti dei clienti del commercialista loggato
        List<DocumentoResponse> documenti = commercialistaService.trovaDocumentiStudio(utente.getId())
                .stream().map(DocumentoMapper::toResponse).toList();
        return ResponseEntity.ok(documenti);
    }
}
