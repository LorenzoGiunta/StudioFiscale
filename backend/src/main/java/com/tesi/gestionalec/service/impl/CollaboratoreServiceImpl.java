package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.DocumentoRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.CollaboratoreService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servizio specifico per i collaboratori, estensione del servizio utenti.
 *
 * Espone le attività di revisione: consultazione delle pratiche assegnate e dei
 * documenti da revisionare, con approvazione o rifiuto motivato di questi
 * ultimi.
 */
@Service
public class CollaboratoreServiceImpl extends UtenteServiceImpl implements CollaboratoreService {

    private final CollaboratoreRepo collaboratoreRepo;
    private final DocumentoRepo documentoRepo;
    private final GestoreNotifiche gestoreNotifiche;

    public CollaboratoreServiceImpl(
            UtenteRepo utenteRepository,
            PasswordEncoder passwordEncoder,
            CollaboratoreRepo collaboratoreRepository,
            CommercialistaRepo commercialistaRepo,
            DocumentoRepo documentoRepository,
            GestoreNotifiche gestoreNotifiche) {
        super(utenteRepository, passwordEncoder, commercialistaRepo);
        this.collaboratoreRepo = collaboratoreRepository;
        this.documentoRepo = documentoRepository;
        this.gestoreNotifiche = gestoreNotifiche;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pratica> trovaPraticheAssegnate(Long collaboratoreId) {
        // Inizializza la collezione differita entro la transazione
        return new ArrayList<>(trovaCollaboratorePerId(collaboratoreId).getPraticheAssegnate());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Documento> trovaDocumentiInRevisione(Long collaboratoreId) {
        return new ArrayList<>(trovaCollaboratorePerId(collaboratoreId).getDocumentiInRevisione());
    }

    @Override
    public void approvaDocumento(Long documentoId, Long collaboratoreId) {
        Documento doc = documentoRepo.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", documentoId));
        // Ownership check: solo il revisore assegnato può approvare
        if (doc.getRevisore() == null || !doc.getRevisore().getId().equals(collaboratoreId)) {
            throw new ForbiddenOperationException(
                    "Questo documento non è assegnato a te per la revisione");
        }
        doc.setStato(StatoDocumento.APPROVATO);
        documentoRepo.save(doc);

        // Avvisa il cliente autore dell'esito della revisione
        notificaEsitoRevisione(doc);
    }

    @Override
    public void rifiutaDocumento(Long documentoId, String motivazione, Long collaboratoreId) {
        Documento doc = documentoRepo.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", documentoId));
        // Ownership check: solo il revisore assegnato può rifiutare
        if (doc.getRevisore() == null || !doc.getRevisore().getId().equals(collaboratoreId)) {
            throw new ForbiddenOperationException(
                    "Questo documento non è assegnato a te per la revisione");
        }
        doc.setStato(StatoDocumento.RIFIUTATO);
        doc.setMotivazioneRifiuto(motivazione);
        documentoRepo.save(doc);

        // Avvisa il cliente autore dell'esito della revisione
        notificaEsitoRevisione(doc);
    }

    private Collaboratore trovaCollaboratorePerId(Long id) {
        return collaboratoreRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboratore", "id", id));
    }

    /**
     * Notifica al cliente autore l'esito (approvazione o rifiuto) della revisione
     * del suo documento, inoltrandola via Observer (in-app + email). In caso di
     * rifiuto include l'eventuale motivazione. Operazione best-effort: se l'autore
     * non è disponibile la notifica viene semplicemente omessa.
     */
    private void notificaEsitoRevisione(Documento documento) {
        Cliente autore = documento.getCaricatoDa();
        if (autore == null) {
            return;
        }
        boolean approvato = documento.getStato() == StatoDocumento.APPROVATO;

        String messaggio;
        if (approvato) {
            messaggio = "Il tuo documento «" + documento.getNome() + "» è stato approvato.";
        } else {
            messaggio = "Il tuo documento «" + documento.getNome() + "» è stato rifiutato.";
            if (documento.getMotivazioneRifiuto() != null && !documento.getMotivazioneRifiuto().isBlank()) {
                messaggio += " Motivazione: " + documento.getMotivazioneRifiuto();
            }
        }

        Notifica notifica = new Notifica();
        notifica.setDestinatario(autore);
        notifica.setMessaggio(messaggio);
        notifica.setTipo(approvato ? TipoNotifica.DOCUMENTO_APPROVATO : TipoNotifica.DOCUMENTO_RIFIUTATO);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }
}