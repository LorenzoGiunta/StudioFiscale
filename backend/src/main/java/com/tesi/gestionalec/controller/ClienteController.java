package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.ClienteUpdateRequest;
import com.tesi.gestionalec.dto.response.ClienteResponse;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.mapper.ClienteMapper;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.mapper.PraticaMapper;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dell'area riservata al cliente.
 *
 * Consente al cliente autenticato di consultare le proprie pratiche e i propri
 * documenti e di visualizzare e aggiornare il proprio profilo anagrafico e
 * fiscale. L'accesso è riservato agli utenti con ruolo cliente.
 */
@RestController
@RequestMapping("/api/cliente")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_CLIENTE')")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping("/pratiche")
    public ResponseEntity<List<PraticaResponse>> miePratiche(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(clienteService.trovaPratiche(utente.getId())
                .stream()
                .map(PraticaMapper::toResponse)
                .toList());
    }

    @GetMapping("/documenti")
    public ResponseEntity<List<DocumentoResponse>> mieDocumenti(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(clienteService.trovaDocumenti(utente.getId())
                .stream()
                .map(DocumentoMapper::toResponse)
                .toList());
    }

    /**
     * Restituisce il profilo del cliente autenticato, con anagrafica e dati
     * fiscali.
     */
    @GetMapping("/profilo")
    public ResponseEntity<ClienteResponse> mioProfilo(@AuthenticationPrincipal Utente utente) {
        Cliente cliente = clienteService.trovaClientePerId(utente.getId());
        return ResponseEntity.ok(ClienteMapper.toResponse(cliente));
    }

    /** Aggiorna il profilo del cliente autenticato con i dati forniti. */
    @PutMapping("/profilo")
    public ResponseEntity<ClienteResponse> aggiornaProfilo(
            @AuthenticationPrincipal Utente utente,
            @Valid @RequestBody ClienteUpdateRequest request) {
        Cliente dati = new Cliente();
        dati.setNome(request.getNome());
        dati.setCognome(request.getCognome());
        dati.setEmail(request.getEmail());
        dati.setCodFiscale(request.getCodFiscale());
        dati.setPIVA(request.getPartitaIva());
        dati.setRegime(request.getRegime());
        dati.setRedditoAnnuo(request.getRedditoAnnuo());
        Cliente aggiornato = clienteService.aggiorna(utente.getId(), dati);
        return ResponseEntity.ok(ClienteMapper.toResponse(aggiornato));
    }
}
