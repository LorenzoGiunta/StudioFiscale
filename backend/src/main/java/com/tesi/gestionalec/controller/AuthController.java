package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.LoginRequest;
import com.tesi.gestionalec.dto.request.RegistrazioneRequest;
import com.tesi.gestionalec.dto.request.ResetPasswordRequest;
import com.tesi.gestionalec.dto.request.RichiestaResetPasswordRequest;
import com.tesi.gestionalec.dto.response.AuthResponse;
import com.tesi.gestionalec.dto.response.CommercialistaPubblicoResponse;
import com.tesi.gestionalec.dto.response.MessaggioResponse;
import com.tesi.gestionalec.exception.RegistrazioneNonApprovataException;
import com.tesi.gestionalec.mapper.RegistrazioneMapper;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.StatoRegistrazione;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.AmministratoreRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.security.GestoreTokenService;
import com.tesi.gestionalec.service.interfaces.PasswordResetService;
import com.tesi.gestionalec.service.interfaces.UtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Controller per l'autenticazione e la registrazione degli utenti.
 *
 * Espone gli endpoint pubblici per creare un account e accedere, restituendo in
 * entrambi i casi un token JWT da utilizzare nelle richieste successive. Fa
 * eccezione l'iscrizione di un commercialista, che resta in attesa della
 * verifica manuale del numero di albo da parte di un amministratore: in quel
 * caso non viene emesso alcun token. Gestisce inoltre il recupero della
 * password via email e mette a disposizione l'elenco dei commercialisti
 * disponibili, necessario in fase di registrazione di un cliente.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtenteService utenteService;
    private final GestoreTokenService gestoreToken;
    private final AuthenticationManager authManager;
    private final CommercialistaRepo commercialistaRepo;
    private final AmministratoreRepo amministratoreRepo;
    private final GestoreNotifiche gestoreNotifiche;
    private final PasswordResetService passwordResetService;

    @PostMapping("/registra")
    public ResponseEntity<AuthResponse> registra(@Valid @RequestBody RegistrazioneRequest request) {

        // Validazione dei campi obbligatori in funzione del ruolo
        utenteService.validaPerRuolo(request);

        Utente utente = RegistrazioneMapper.toModel(request);
        Utente salvato = utenteService.registra(utente, request.getCommercialistaId());

        // Iscrizione soggetta a verifica: nessun token, si avvisano gli
        // amministratori e si conferma al richiedente la presa in carico.
        if (salvato.getStatoRegistrazione() == StatoRegistrazione.IN_ATTESA) {
            inviaNotificaPresaInCarico(salvato);
            inviaNotificaAgliAmministratori(salvato);

            return ResponseEntity.accepted().body(new AuthResponse(
                    null,
                    salvato.getId(),
                    salvato.getRuolo().name(),
                    salvato.getEmail(),
                    salvato.getNome(),
                    salvato.getCognome(),
                    salvato.getStatoRegistrazione().name()));
        }

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
                salvato.getCognome(),
                salvato.getStatoRegistrazione().name()));
    }

    private void inviaNotificaBenvenuto(Utente utente) {
        inviaNotifica(utente,
                "Benvenuto " + utente.getNome() + "! Il tuo account è stato creato.",
                TipoNotifica.ACCOUNT_CREATO);
    }

    private void inviaNotificaPresaInCarico(Utente utente) {
        inviaNotifica(utente,
                "Grazie " + utente.getNome() + ", la tua richiesta di iscrizione è stata ricevuta. "
                        + "Un amministratore verificherà il numero di albo indicato: riceverai "
                        + "un'email non appena l'account sarà attivato.",
                TipoNotifica.ACCOUNT_CREATO);
    }

    /**
     * Segnala a tutti gli amministratori attivi che c'è un'iscrizione da
     * verificare, riportando nel messaggio i dati utili al controllo.
     */
    private void inviaNotificaAgliAmministratori(Utente richiedente) {
        String dettaglio = richiedente instanceof Commercialista c
                ? " (numero di albo: " + c.getNumeroAlbo() + ")"
                : "";
        String messaggio = "Nuova richiesta di iscrizione come " + richiedente.getRuolo().name().toLowerCase()
                + " da " + richiedente.getNome() + " " + richiedente.getCognome()
                + " <" + richiedente.getEmail() + ">" + dettaglio
                + ". Verifica i dati e approva o rifiuta la richiesta dall'area di amministrazione.";

        amministratoreRepo.findAll().stream()
                .filter(Utente::isEnabled)
                .forEach(admin -> inviaNotifica(admin, messaggio, TipoNotifica.RICHIESTA_ISCRIZIONE));
    }

    private void inviaNotifica(Utente destinatario, String messaggio, TipoNotifica tipo) {
        Notifica notifica = new Notifica();
        notifica.setDestinatario(destinatario);
        notifica.setMessaggio(messaggio);
        notifica.setTipo(tipo);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    /**
     * Endpoint pubblico che restituisce i commercialisti abilitati della
     * piattaforma, utilizzato dal modulo di registrazione del cliente per
     * scegliere il professionista di riferimento. Non richiede autenticazione,
     * quindi espone il minimo indispensabile (id, nome e cognome): niente email
     * né altri dati dell'account a chi non è loggato.
     */
    @GetMapping("/commercialisti")
    public ResponseEntity<List<CommercialistaPubblicoResponse>> commercialisti() {
        List<CommercialistaPubblicoResponse> lista = commercialistaRepo.findAll()
                .stream()
                .filter(Commercialista::isEnabled)
                .map(c -> new CommercialistaPubblicoResponse(c.getId(), c.getNome(), c.getCognome()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    // ─── Recupero password ───────────────────────────────────────────────────

    /**
     * Primo passo: invia il link di reimpostazione. La risposta è la stessa sia
     * che l'email esista sia che no, per non rivelare quali indirizzi hanno un
     * account.
     */
    @PostMapping("/password/richiedi-reset")
    public ResponseEntity<MessaggioResponse> richiediResetPassword(
            @Valid @RequestBody RichiestaResetPasswordRequest request) {
        passwordResetService.richiediReset(request.getEmail());
        return ResponseEntity.ok(new MessaggioResponse(
                "Se l'indirizzo è registrato riceverai un'email con le istruzioni per reimpostare la password."));
    }

    /** Secondo passo: imposta la nuova password consumando il token ricevuto via email. */
    @PostMapping("/password/reset")
    public ResponseEntity<MessaggioResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reimpostaPassword(request.getToken(), request.getNuovaPassword());
        return ResponseEntity.ok(new MessaggioResponse(
                "Password aggiornata. Ora puoi accedere con la nuova password."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // La verifica di email e password è affidata a Spring Security
        Authentication auth;
        try {
            auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (DisabledException ex) {
            // Spring segnala genericamente "disabilitato": se il motivo è
            // un'iscrizione non ancora approvata (o rifiutata) lo si dice
            // esplicitamente, altrimenti si lascia il caso al gestore standard.
            StatoRegistrazione stato = utenteService.trovaPerEmail(request.getEmail()).getStatoRegistrazione();
            if (stato != StatoRegistrazione.APPROVATA) {
                throw new RegistrazioneNonApprovataException(stato);
            }
            throw ex;
        }

        Utente utente = (Utente) auth.getPrincipal();
        String token = gestoreToken.generaToken(utente.getEmail());

        return ResponseEntity.ok(new AuthResponse(
                token,
                utente.getId(),
                utente.getRuolo().name(),
                utente.getEmail(),
                utente.getNome(),
                utente.getCognome(),
                utente.getStatoRegistrazione().name()));
    }
}
