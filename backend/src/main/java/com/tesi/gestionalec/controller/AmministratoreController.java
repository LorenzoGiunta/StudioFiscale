package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.response.StatisticheResponse;
import com.tesi.gestionalec.dto.response.UltimaAzioneResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.mapper.UtenteMapper;
import com.tesi.gestionalec.model.Amministratore;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.AmministratoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller dell'area di amministrazione.
 *
 * Consente la gestione degli utenti (consultazione paginata, abilitazione,
 * disabilitazione, cancellazione logica e ripristino) e la consultazione delle
 * statistiche di sistema. L'accesso è riservato agli amministratori.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_AMMINISTRATORE')")
public class AmministratoreController {

    private final AmministratoreService amministratoreService;

    @GetMapping("/utenti")
    public ResponseEntity<Page<UtenteResponse>> trovaTuttiUtenti(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                new Sort.Order(Sort.Direction.fromString(sort.length > 1 ? sort[1] : "asc"), sort[0])));
        Page<UtenteResponse> risultato = amministratoreService.trovaTutti(pageable)
                .map(UtenteMapper::toResponse);
        return ResponseEntity.ok(risultato);
    }

    // Statistiche aggregate di sistema per la dashboard dell'amministratore
    @GetMapping("/statistiche")
    public ResponseEntity<StatisticheResponse> statistiche() {
        return ResponseEntity.ok(amministratoreService.calcolaStatistiche());
    }

    // Istante dell'ultima azione amministrativa svolta dall'amministratore corrente
    @GetMapping("/ultima-azione")
    public ResponseEntity<UltimaAzioneResponse> ultimaAzione(
            @AuthenticationPrincipal Amministratore amministratore) {
        return ResponseEntity.ok(
                new UltimaAzioneResponse(amministratore.getUltimaAzioneAmministrativa()));
    }

    // Utenti cancellati logicamente, per la sezione di ripristino
    @GetMapping("/utenti/eliminati")
    public ResponseEntity<List<UtenteResponse>> trovaEliminati() {
        List<UtenteResponse> eliminati = amministratoreService.trovaEliminati()
                .stream().map(UtenteMapper::toResponse).toList();
        return ResponseEntity.ok(eliminati);
    }

    @GetMapping("/utenti/{id}")
    public ResponseEntity<UtenteResponse> trovaPerId(@PathVariable Long id) {
        return ResponseEntity.ok(UtenteMapper.toResponse(amministratoreService.trovaPerId(id)));
    }

    @PutMapping("/utenti/{id}/abilita")
    public ResponseEntity<Void> abilita(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        amministratoreService.abilitaUtente(id, utente);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/utenti/{id}/disabilita")
    public ResponseEntity<Void> disabilita(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Vincolo di sicurezza: non si disabilita sé stessi né un altro amministratore
        amministratoreService.disabilitaUtente(id, utente);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/utenti/{id}")
    public ResponseEntity<Void> elimina(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Vincolo di sicurezza: non si elimina sé stessi né un altro amministratore
        amministratoreService.eliminaUtente(id, utente);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/utenti/{id}/ripristina")
    public ResponseEntity<Void> ripristina(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        amministratoreService.ripristinaUtente(id, utente);
        return ResponseEntity.ok().build();
    }
}
