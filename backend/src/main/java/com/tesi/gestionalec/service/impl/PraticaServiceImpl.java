package com.tesi.gestionalec.service.impl;


import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.repository.PraticaRepo;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servizio per la gestione del ciclo di vita delle pratiche.
 *
 * Coordina creazione, ricerca, assegnazione e avanzamento di stato delle
 * pratiche. Le transizioni di stato sono affidate al pattern State, mentre gli
 * eventi rilevanti (creazione, cambio di stato, assegnazione) vengono propagati
 * agli interessati tramite il pattern Observer. La cancellazione è logica, per
 * conservare lo storico ai fini di audit fiscale.
 */
@Service
@RequiredArgsConstructor
public class PraticaServiceImpl implements PraticaService {

    private final PraticaRepo praticaRepo;
    private final CollaboratoreRepo collaboratoreRepo;
    private final InvitoCollaborazioneRepo invitoRepo;
    private final GestoreNotifiche gestoreNotifiche;

    @Override
    public Pratica creaPratica(Pratica pratica, Utente richiedente) {
        // Ownership check: il cliente intestatario deve essere del commercialista
        // richiedente. La relazione cliente→commercialista è @ManyToOne (EAGER),
        // quindi è già disponibile sull'entità caricata dal controller.
        Cliente cliente = pratica.getCliente();
        if (cliente == null
                || cliente.getCommercialista() == null
                || !cliente.getCommercialista().getId().equals(richiedente.getId())) {
            throw new ForbiddenOperationException(
                    "Non puoi creare una pratica per un cliente di un altro studio");
        }
        return creaPratica(pratica);
    }

    @Override
    public Pratica creaPratica(Pratica pratica) {

        pratica.setStato(StatoPratica.BOZZA);
        Pratica salvata = praticaRepo.save(pratica);

        // Avvisa il cliente dell'avvenuta creazione della pratica
        Notifica notifica = new Notifica();
        notifica.setDestinatario(pratica.getCliente());
        notifica.setMessaggio("La tua pratica è stata creata con stato: " + pratica.getStato());
        notifica.setTipo(TipoNotifica.CAMBIO_STATO);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);

        return salvata;
    }

    @Override
    public Pratica trovaPerId(Long id) {
        return praticaRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pratica", "id", id));
    }

    /**
     * Ricerca con ownership check in base al ruolo dell'utente richiedente.
     * - CLIENTE: deve essere il cliente intestatario della pratica.
     * - COLLABORATORE: deve essere il collaboratore assegnato alla pratica.
     * - COMMERCIALISTA: deve essere il commercialista del cliente della pratica.
     * In tutti gli altri casi (ruolo sconosciuto) viene negato l'accesso per sicurezza.
     */
    @Override
    public Pratica trovaPerId(Long id, Utente richiedente) {
        Pratica pratica = trovaPerId(id);

        switch (richiedente.getRuolo()) {
            case CLIENTE -> {
                if (!pratica.getCliente().getId().equals(richiedente.getId())) {
                    throw new ForbiddenOperationException(
                            "Non sei autorizzato ad accedere a questa pratica");
                }
            }
            case COLLABORATORE -> {
                if (pratica.getAssegnataA() == null
                        || !pratica.getAssegnataA().getId().equals(richiedente.getId())) {
                    throw new ForbiddenOperationException(
                            "Questa pratica non è assegnata a te");
                }
            }
            case COMMERCIALISTA -> {
                Commercialista comm = pratica.getCliente().getCommercialista();
                if (comm == null || !comm.getId().equals(richiedente.getId())) {
                    throw new ForbiddenOperationException(
                            "Questa pratica non appartiene a un tuo cliente");
                }
            }
            default -> throw new ForbiddenOperationException(
                    "Accesso non consentito per questo ruolo");
        }

        return pratica;
    }

    @Override
    public List<Pratica> trovaTutte() {
        return praticaRepo.findAll();
    }

    @Override
    public Page<Pratica> trovaTutte(Pageable pageable) {
        return praticaRepo.findAll(pageable);
    }

    @Override
    public List<Pratica> trovaPerCliente(Cliente cliente) {
        return praticaRepo.findByCliente(cliente);
    }

    @Override
    public List<Pratica> trovaPerCollaboratore(Collaboratore collaboratore) {
        return praticaRepo.findByAssegnataA(collaboratore);
    }

    @Override
    public void avanzaStato(Long praticaId) {
        avanzaStato(trovaPerId(praticaId));
    }

    @Override
    public void avanzaStato(Long praticaId, Utente richiedente) {
        // trovaPerId(id, richiedente) applica l'ownership check per ruolo
        avanzaStato(trovaPerId(praticaId, richiedente));
    }

    private void avanzaStato(Pratica pratica) {
        // La transizione è delegata allo stato corrente (pattern State)
        pratica.getStatoCorrente().avanza(pratica);
        praticaRepo.save(pratica);

        // Avvisa il cliente del cambio di stato
        Notifica notifica = new Notifica();
        notifica.setDestinatario(pratica.getCliente());
        notifica.setMessaggio("La tua pratica è passata allo stato: " + pratica.getStato().getEtichetta());
        notifica.setTipo(TipoNotifica.CAMBIO_STATO);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    @Override
    public void assegnaCollaboratore(Long praticaId, Long collaboratoreId) {
        assegnaCollaboratore(trovaPerId(praticaId), collaboratoreId);
    }

    @Override
    public void assegnaCollaboratore(Long praticaId, Long collaboratoreId, Utente richiedente) {
        // 1) la pratica deve appartenere al commercialista richiedente
        Pratica pratica = trovaPerId(praticaId, richiedente);
        // 2) il collaboratore deve far parte dello studio del richiedente
        if (!invitoRepo.existsByCommercialista_IdAndCollaboratore_IdAndStato(
                richiedente.getId(), collaboratoreId, StatoInvito.ACCEPTED)) {
            throw new ForbiddenOperationException(
                    "Questo collaboratore non fa parte del tuo studio");
        }
        assegnaCollaboratore(pratica, collaboratoreId);
    }

    private void assegnaCollaboratore(Pratica pratica, Long collaboratoreId) {
        Collaboratore collaboratore = collaboratoreRepo.findById(collaboratoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboratore", "id", collaboratoreId));

        pratica.setAssegnataA(collaboratore);
        praticaRepo.save(pratica);

        // Avvisa il collaboratore della nuova assegnazione
        Notifica notifica = new Notifica();
        notifica.setDestinatario(collaboratore);
        notifica.setMessaggio("Ti è stata assegnata una nuova pratica con id: " + pratica.getId());
        notifica.setTipo(TipoNotifica.CAMBIO_STATO);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    @Override
    public List<Pratica> trovaPerStato(StatoPratica stato) {
        return praticaRepo.findByStato(stato);
    }

    /**
     * Cancellazione logica della pratica: il record viene marcato come eliminato
     * con il relativo timestamp anziché essere rimosso. Pratica e documenti
     * collegati restano disponibili per lo storico e l'audit fiscale, pur
     * risultando esclusi dalle query ordinarie grazie al filtro sull'entità.
     */
    @Override
    public void eliminaPratica(Long id) {
        eliminaPratica(trovaPerId(id));
    }

    @Override
    public void eliminaPratica(Long id, Utente richiedente) {
        // trovaPerId(id, richiedente) applica l'ownership check per ruolo
        eliminaPratica(trovaPerId(id, richiedente));
    }

    private void eliminaPratica(Pratica pratica) {
        pratica.setDeleted(true);
        pratica.setDeletedAt(LocalDateTime.now());
        praticaRepo.save(pratica);
    }
}
