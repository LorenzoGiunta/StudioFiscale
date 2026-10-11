package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.exception.TokenResetNonValidoException;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.TokenResetPassword;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.TokenResetPasswordRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementazione del recupero password via email.
 *
 * Il link contiene un token casuale (UUID) valido per un tempo limitato e
 * utilizzabile una sola volta. A ogni nuova richiesta i token pendenti dello
 * stesso utente vengono invalidati, così ne circola al più uno. Dopo il
 * cambio di password l'utente riceve una conferma tramite il pattern
 * Observer (notifica in-app + email), utile ad accorgersi di un uso indebito.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UtenteRepo utenteRepo;
    private final TokenResetPasswordRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final GestoreNotifiche gestoreNotifiche;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.password-reset.durata-minuti:60}")
    private long durataMinuti;

    @Override
    public void richiediReset(String email) {
        // Gli utenti cancellati logicamente non vengono trovati (filtro sull'entità)
        Utente utente = utenteRepo.findByEmail(email).orElse(null);
        if (utente == null) {
            // Nessun errore verso l'esterno: la risposta deve essere identica a
            // quella del caso positivo per non rivelare quali email esistono.
            log.info("Richiesta reset password per email non registrata");
            return;
        }

        // Un solo link valido alla volta: i precedenti non ancora usati decadono
        tokenRepo.deleteByUtenteAndUsatoIlIsNull(utente);

        TokenResetPassword token = new TokenResetPassword();
        token.setUtente(utente);
        token.setToken(UUID.randomUUID().toString());
        token.setScadeIl(LocalDateTime.now().plusMinutes(durataMinuti));
        tokenRepo.save(token);

        inviaEmailReset(utente, token.getToken());
        log.info("Link di reset password inviato all'utente {}", utente.getId());
    }

    @Override
    public void reimpostaPassword(String tokenValore, String nuovaPassword) {
        TokenResetPassword token = tokenRepo.findByToken(tokenValore)
                .orElseThrow(TokenResetNonValidoException::new);

        if (!token.isValido()) {
            throw new TokenResetNonValidoException();
        }

        Utente utente = token.getUtente();
        utente.setPassword(passwordEncoder.encode(nuovaPassword));
        utenteRepo.save(utente);

        // Il token si consuma: un secondo uso dello stesso link viene respinto
        token.setUsatoIl(LocalDateTime.now());
        tokenRepo.save(token);

        notificaCambioPassword(utente);
        log.info("Password reimpostata per l'utente {}", utente.getId());
    }

    /** Pulizia notturna dei token ormai scaduti, che non servono più a nulla. */
    @Scheduled(cron = "0 30 1 * * *")
    public void eliminaTokenScaduti() {
        long eliminati = tokenRepo.deleteByScadeIlBefore(LocalDateTime.now());
        if (eliminati > 0) {
            log.info("Eliminati {} token di reset password scaduti", eliminati);
        }
    }

    // L'email col link è inviata direttamente, senza passare dalle notifiche
    // in-app: il link è un segreto temporaneo e non ha senso conservarlo.
    private void inviaEmailReset(Utente utente, String token) {
        String link = frontendUrl + "/reset-password/" + token;

        String corpo = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <div style="background: #1a2744; padding: 24px; border-radius: 8px 8px 0 0;">
                    <h1 style="color: #f59e0b; margin: 0; font-size: 22px;">StudioFiscale</h1>
                  </div>
                  <div style="padding: 32px; background: #ffffff; border: 1px solid #e5e7eb;">
                    <h2 style="color: #1a2744; margin-top: 0;">Reimposta la tua password</h2>
                    <p style="color: #4b5563; line-height: 1.6;">
                      Ciao <strong>%s</strong>, abbiamo ricevuto una richiesta di recupero password
                      per il tuo account su <strong>StudioFiscale</strong>.
                    </p>
                    <p style="color: #4b5563;">Il link è valido per <strong>%d minuti</strong> e può essere usato una sola volta.</p>
                    <div style="margin: 32px 0;">
                      <a href="%s"
                         style="background: #1a2744; color: white; padding: 12px 24px;
                                text-decoration: none; border-radius: 6px; font-weight: bold;">
                        Scegli una nuova password
                      </a>
                    </div>
                    <p style="color: #6b7280; font-size: 13px;">
                      Se il pulsante non funziona, copia questo indirizzo nel browser:<br>
                      <span style="word-break: break-all;">%s</span>
                    </p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;">
                    <p style="color: #9ca3af; font-size: 12px;">
                      Se non hai richiesto tu il recupero, ignora questa email: la tua password resterà invariata.
                    </p>
                  </div>
                </div>
                """.formatted(utente.getNome(), durataMinuti, link, link);

        emailService.inviaEmail(utente.getEmail(), "Recupero password — StudioFiscale", corpo);
    }

    private void notificaCambioPassword(Utente utente) {
        Notifica notifica = new Notifica();
        notifica.setDestinatario(utente);
        notifica.setMessaggio("La password del tuo account è stata reimpostata. "
                + "Se non sei stato tu, contatta subito l'amministratore.");
        notifica.setTipo(TipoNotifica.PASSWORD_MODIFICATA);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }
}
