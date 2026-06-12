package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.exception.DuplicateInviteException;
import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.service.interfaces.InvitoCollaborazioneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servizio per la gestione degli inviti di collaborazione.
 *
 * Governa l'intero ciclo di vita dell'invito tra commercialista e
 * collaboratore: emissione (con invio dell'email e collegamento di eventuali
 * utenti già registrati), accettazione, rifiuto, revoca e scadenza automatica.
 * Applica le regole di coerenza sullo stato e di proprietà sull'operazione, e
 * notifica le parti interessate degli esiti.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvitoCollaborazioneServiceImpl implements InvitoCollaborazioneService {

    private final InvitoCollaborazioneRepo invitoRepo;
    private final CommercialistaRepo commercialistaRepo;
    private final CollaboratoreRepo collaboratoreRepo;
    private final GestoreNotifiche gestoreNotifiche;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public InvitoCollaborazione invita(Long commercialistaId, String emailDestinatario) {
        Commercialista comm = commercialistaRepo.findById(commercialistaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commercialista", "id", commercialistaId));

        // È ammesso un solo invito in attesa per coppia commercialista-email
        if (invitoRepo.existsByCommercialista_IdAndEmailDestinatarioAndStato(
                commercialistaId, emailDestinatario, StatoInvito.PENDING)) {
            throw new DuplicateInviteException(emailDestinatario);
        }

        InvitoCollaborazione invito = new InvitoCollaborazione();
        invito.setCommercialista(comm);
        invito.setEmailDestinatario(emailDestinatario);
        invito.setToken(UUID.randomUUID().toString());
        invito.setStato(StatoInvito.PENDING);
        invito.setScadeIl(LocalDateTime.now().plusDays(7));
        // La data di creazione è valorizzata automaticamente dalla persistenza

        // Se il destinatario è già un utente registrato, lo si collega subito
        Collaboratore collabEsistente = collaboratoreRepo.findByEmail(emailDestinatario).orElse(null);
        if (collabEsistente != null) {
            invito.setCollaboratore(collabEsistente);
            log.info("Destinatario {} già registrato — collegato all'invito", emailDestinatario);
        }

        InvitoCollaborazione salvato = invitoRepo.save(invito);

        String nomeCommercialista = comm.getNome() + " " + comm.getCognome();

        // Notifica in-app al collaboratore già registrato (oltre all'email di invito)
        if (collabEsistente != null) {
            creaNotifica(
                    collabEsistente,
                    "Il commercialista " + nomeCommercialista + " ti ha invitato a collaborare. "
                            + "Vai nella sezione Inviti per accettare o rifiutare.",
                    TipoNotifica.INVITO_COLLABORAZIONE);
        }

        inviaEmailInvito(emailDestinatario, nomeCommercialista, salvato.getToken());

        log.info("Invito creato [id={}] da commercialista {} verso {}",
                salvato.getId(), commercialistaId, emailDestinatario);

        return salvato;
    }

    @Override
    public void accetta(String token, Long collaboratoreId) {
        InvitoCollaborazione invito = findInvitoValidoByToken(token);

        Collaboratore collab = collaboratoreRepo.findById(collaboratoreId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Collaboratore", "id", collaboratoreId));

        // L'email del collaboratore deve corrispondere al destinatario
        if (!collab.getEmail().equalsIgnoreCase(invito.getEmailDestinatario())) {
            throw new ForbiddenOperationException(
                    "La tua email non corrisponde al destinatario dell'invito");
        }

        invito.setCollaboratore(collab);
        invito.setStato(StatoInvito.ACCEPTED);

        creaNotifica(
                invito.getCommercialista(),
                collab.getNome() + " " + collab.getCognome() + " ha accettato il tuo invito di collaborazione.",
                TipoNotifica.INVITO_COLLABORAZIONE);

        log.info("Invito [id={}] accettato dal collaboratore {}", invito.getId(), collaboratoreId);
    }

    @Override
    public void rifiuta(String token) {
        InvitoCollaborazione invito = findInvitoValidoByToken(token);
        invito.setStato(StatoInvito.DECLINED);

        String msg = "L'invito inviato a " + invito.getEmailDestinatario() + " è stato rifiutato.";
        creaNotifica(invito.getCommercialista(), msg, TipoNotifica.INVITO_COLLABORAZIONE);

        log.info("Invito [id={}] rifiutato (email destinatario: {})",
                invito.getId(), invito.getEmailDestinatario());
    }

    @Override
    public void revoca(Long invitoId, Long commercialistaId) {
        InvitoCollaborazione invito = invitoRepo.findById(invitoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invito", "id", invitoId));

        if (!invito.getCommercialista().getId().equals(commercialistaId)) {
            throw new ForbiddenOperationException(
                    "Non sei autorizzato a revocare questo invito");
        }

        if (invito.getStato() == StatoInvito.DECLINED || invito.getStato() == StatoInvito.EXPIRED) {
            throw new InvalidStateException(
                    "Invito", invito.getStato().name(), "revoca");
        }

        // Un invito già accettato non è revocabile direttamente: il collaboratore
        // potrebbe avere pratiche o documenti assegnati, la cui gestione
        // richiederebbe un'operazione dedicata che ne risolva le dipendenze.
        if (invito.getStato() == StatoInvito.ACCEPTED) {
            throw new InvalidStateException(
                    "Impossibile revocare un invito già accettato: il collaboratore potrebbe avere "
                            + "pratiche o documenti assegnati. Rimuovere prima le assegnazioni.");
        }

        invito.setStato(StatoInvito.DECLINED);

        log.info("Invito [id={}] revocato dal commercialista {}", invitoId, commercialistaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitoCollaborazione> trovaPerCommercialista(Long commercialistaId) {
        return invitoRepo.findByCommercialista_Id(commercialistaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitoCollaborazione> trovaPendingPerEmail(String email) {
        return invitoRepo.findByEmailDestinatarioAndStato(email, StatoInvito.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitoCollaborazione> trovaAccettatiPerEmail(String email) {
        return invitoRepo.findByEmailDestinatarioAndStato(email, StatoInvito.ACCEPTED);
    }

    /**
     * Processo notturno che marca come scaduti gli inviti ancora in attesa oltre
     * il termine previsto. La soluzione è adeguata a un singolo nodo; in un
     * deployment distribuito occorrerebbe un meccanismo di lock condiviso.
     */
    @Override
    @Scheduled(cron = "0 0 1 * * *")
    public void scadenzaAutomatica() {
        List<InvitoCollaborazione> scaduti = invitoRepo
                .findByStatoAndScadeIlBefore(StatoInvito.PENDING, LocalDateTime.now());

        if (!scaduti.isEmpty()) {
            scaduti.forEach(i -> i.setStato(StatoInvito.EXPIRED));
            invitoRepo.saveAll(scaduti);
            log.info("Scadenza automatica: {} inviti marcati EXPIRED", scaduti.size());
        }
    }

    // Recupera un invito ancora valido (in attesa e non scaduto) dato il token,
    // sollevando un'eccezione descrittiva quando la condizione non è soddisfatta
    private InvitoCollaborazione findInvitoValidoByToken(String token) {
        InvitoCollaborazione invito = invitoRepo.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invito", "token", token));

        if (invito.getStato() != StatoInvito.PENDING) {
            throw new InvalidStateException(
                    "Invito", invito.getStato().name(), "accetta/rifiuta");
        }

        if (invito.getScadeIl().isBefore(LocalDateTime.now())) {
            invito.setStato(StatoInvito.EXPIRED);
            throw new InvalidStateException(
                    "L'invito è scaduto il " + invito.getScadeIl());
        }

        return invito;
    }

    // Propaga la notifica tramite il pattern Observer: viene così sia persistita
    // (notifica in-app) sia inoltrata via email, come per gli altri eventi del
    // sistema.
    private void creaNotifica(Utente destinatario, String messaggio, TipoNotifica tipo) {
        Notifica notifica = new Notifica();
        notifica.setDestinatario(destinatario);
        notifica.setMessaggio(messaggio);
        notifica.setTipo(tipo);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    private void inviaEmailInvito(String emailDestinatario, String nomeCommercialista, String token) {
        String linkAccetta = frontendUrl + "/invito/" + token + "/accetta";
        String linkRifiuta = frontendUrl + "/invito/" + token + "/rifiuta";

        String corpo = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <div style="background: #1a2744; padding: 24px; border-radius: 8px 8px 0 0;">
                    <h1 style="color: #f59e0b; margin: 0; font-size: 22px;">StudioFiscale</h1>
                  </div>
                  <div style="padding: 32px; background: #ffffff; border: 1px solid #e5e7eb;">
                    <h2 style="color: #1a2744; margin-top: 0;">Hai ricevuto un invito di collaborazione</h2>
                    <p style="color: #4b5563; line-height: 1.6;">
                      Il commercialista <strong>%s</strong> ti ha invitato a collaborare
                      tramite la piattaforma <strong>StudioFiscale</strong>.
                    </p>
                    <p style="color: #4b5563;">L'invito scade tra <strong>7 giorni</strong>.</p>
                    <div style="margin: 32px 0; display: flex; gap: 12px;">
                      <a href="%s"
                         style="background: #1a2744; color: white; padding: 12px 24px;
                                text-decoration: none; border-radius: 6px; font-weight: bold;">
                        ✓ Accetta Invito
                      </a>
                      &nbsp;&nbsp;
                      <a href="%s"
                         style="background: #f3f4f6; color: #374151; padding: 12px 24px;
                                text-decoration: none; border-radius: 6px;">
                        ✗ Rifiuta
                      </a>
                    </div>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;">
                    <p style="color: #9ca3af; font-size: 12px;">
                      Se non ti aspettavi questo invito, puoi ignorare questa email.
                    </p>
                  </div>
                </div>
                """.formatted(nomeCommercialista, linkAccetta, linkRifiuta);

        emailService.inviaEmail(
                emailDestinatario,
                "Invito di collaborazione — StudioFiscale",
                corpo);
    }
}
