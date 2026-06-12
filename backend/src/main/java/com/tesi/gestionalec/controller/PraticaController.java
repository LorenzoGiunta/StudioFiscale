package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.PraticaRequest;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.facade.PraticaFacade;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.mapper.PraticaMapper;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.DocumentoService;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Controller per la gestione delle pratiche.
 *
 * Espone gli endpoint per la creazione, la consultazione (anche paginata e
 * ordinabile), l'avanzamento di stato, l'assegnazione ai collaboratori e la
 * cancellazione logica, oltre all'elenco dei documenti collegati a una pratica.
 */
@RestController
@RequestMapping("/api/pratiche")
@RequiredArgsConstructor
public class PraticaController {

    private final PraticaService praticaService;
    private final DocumentoService documentoService;
    private final PraticaFacade praticaFacade;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<PraticaResponse> crea(@Valid @RequestBody PraticaRequest request,
            @AuthenticationPrincipal Utente utente) {
        // L'orchestrazione (lookup cliente, mapping, creazione con ownership) è
        // affidata al Facade
        return ResponseEntity.ok(praticaFacade.creaEAssegna(request, null, utente));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COMMERCIALISTA', 'ROLE_COLLABORATORE', 'ROLE_CLIENTE')")
    public ResponseEntity<PraticaResponse> trovaPerId(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(PraticaMapper.toResponse(praticaService.trovaPerId(id, utente)));
    }

    // Documenti collegati a una pratica (per la vista dettaglio)
    @GetMapping("/{id}/documenti")
    @PreAuthorize("hasAnyAuthority('ROLE_COMMERCIALISTA', 'ROLE_COLLABORATORE', 'ROLE_CLIENTE')")
    public ResponseEntity<List<DocumentoResponse>> documentiPratica(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // L'ownership check è eseguito internamente da trovaPerId(id, utente)
        Pratica pratica = praticaService.trovaPerId(id, utente);
        List<DocumentoResponse> documenti = documentoService.trovaPerPratica(pratica)
                .stream().map(DocumentoMapper::toResponse).toList();
        return ResponseEntity.ok(documenti);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Page<PraticaResponse>> trovaTutte(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        Page<PraticaResponse> risultato = praticaService.trovaTutte(pageable)
                .map(PraticaMapper::toResponse);
        return ResponseEntity.ok(risultato);
    }

    @PutMapping("/{id}/avanza")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<PraticaResponse> avanzaStato(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Avanzamento + rilettura tramite Facade: restituisce la pratica aggiornata
        return ResponseEntity.ok(praticaFacade.avanzaERecupera(id, utente));
    }

    @PutMapping("/{praticaId}/assegna/{collaboratoreId}")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> assegnaCollaboratore(@PathVariable Long praticaId,
            @PathVariable Long collaboratoreId,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: pratica del proprio studio + collaboratore del proprio
        // studio
        praticaService.assegnaCollaboratore(praticaId, collaboratoreId, utente);
        return ResponseEntity.ok().build();
    }

    // Cancellazione logica: la pratica resta archiviata per lo storico fiscale
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> elimina(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: solo le pratiche dei propri clienti
        praticaService.eliminaPratica(id, utente);
        return ResponseEntity.noContent().build();
    }

    // Helper per il parsing dell'ordinamento
    private Sort.Order[] parseSort(String[] sort) {
        if (sort.length == 2) {
            Sort.Direction dir = parseDirezione(sort[1]);
            return new Sort.Order[] { new Sort.Order(dir, sort[0]) };
        }
        // Ordinamento su più campi: coppie successive campo/direzione
        Sort.Order[] orders = new Sort.Order[sort.length / 2];
        for (int i = 0; i < sort.length; i += 2) {
            Sort.Direction dir = (i + 1 < sort.length) ? parseDirezione(sort[i + 1]) : Sort.Direction.ASC;
            orders[i / 2] = new Sort.Order(dir, sort[i]);
        }
        return orders;
    }

    // In caso di direzione non valida si adotta l'ordine crescente come ripiego
    private Sort.Direction parseDirezione(String dir) {
        try {
            return Sort.Direction.fromString(dir);
        } catch (IllegalArgumentException e) {
            return Sort.Direction.ASC;
        }
    }
}