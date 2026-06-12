package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.LoginRequest;
import com.tesi.gestionalec.dto.request.RegistrazioneRequest;
import com.tesi.gestionalec.dto.response.AuthResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.mapper.RegistrazioneMapper;
import com.tesi.gestionalec.mapper.UtenteMapper;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.security.GestoreTokenService;
import com.tesi.gestionalec.service.interfaces.UtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Controller per l'autenticazione e la registrazione degli utenti.
 *
 * Espone gli endpoint pubblici per creare un account e accedere, restituendo in
 * entrambi i casi un token JWT da utilizzare nelle richieste successive. Mette
 * inoltre a disposizione l'elenco dei commercialisti disponibili, necessario in
 * fase di registrazione di un cliente.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtenteService utenteService;
    private final GestoreTokenService gestoreToken;
    private final AuthenticationManager authManager;
    private final CommercialistaRepo commercialistaRepo;
    private final GestoreNotifiche gestoreNotifiche;

    @PostMapping("/registra")
    public ResponseEntity<AuthResponse> registra(@Valid @RequestBody RegistrazioneRequest request) {

        // Validazione dei campi obbligatori in funzione del ruolo
        utenteService.validaPerRuolo(request);

        Utente utente = RegistrazioneMapper.toModel(request);
        Utente salvato = utenteService.registra(utente, request.getCommercialistaId());

        // Notifica di benvenuto inoltrata tramite il pattern Observer:
        // viene persistita e, se l'invio email è abilitato, anche spedita.
        inviaNotificaBenvenuto(salvato);

        String token = gestoreToken.generaToken(salvato.getEmail());

        return ResponseEntity.ok(new AuthResponse(
                token,
                salvato.getId(),
                salvato.getRuolo().name(),
                salvato.getEmail(),
                salvato.getNome(),
                salvato.getCognome()));
    }

    private void inviaNotificaBenvenuto(Utente utente) {
        Notifica notifica = new Notifica();
        notifica.setDestinatario(utente);
        notifica.setMessaggio("Benvenuto " + utente.getNome() + "! Il tuo account è stato creato.");
        notifica.setTipo(TipoNotifica.ACCOUNT_CREATO);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    /**
     * Endpoint pubblico che restituisce i commercialisti abilitati della
     * piattaforma, utilizzato dal modulo di registrazione del cliente per
     * scegliere il professionista di riferimento. Non richiede autenticazione.
     */
    @GetMapping("/commercialisti")
    public ResponseEntity<List<UtenteResponse>> commercialisti() {
        List<UtenteResponse> lista = commercialistaRepo.findAll()
                .stream()
                .filter(Commercialista::isEnabled)
                .map(UtenteMapper::toResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // La verifica di email e password è affidata a Spring Security
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        Utente utente = (Utente) auth.getPrincipal();
        String token = gestoreToken.generaToken(utente.getEmail());

        return ResponseEntity.ok(new AuthResponse(
                token,
                utente.getId(),
                utente.getRuolo().name(),
                utente.getEmail(),
                utente.getNome(),
                utente.getCognome()));
    }
}
