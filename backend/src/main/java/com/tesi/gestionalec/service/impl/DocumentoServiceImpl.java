package com.tesi.gestionalec.service.impl;


import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;
import com.tesi.gestionalec.model.StatoInvito;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.ClienteRepo;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.DocumentoRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.service.interfaces.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servizio per la gestione dei documenti delle pratiche.
 *
 * Si occupa del caricamento, del versionamento e dell'assegnazione del revisore,
 * mantenendo lo stato di revisione di ciascun documento. A ogni caricamento — sia
 * della prima versione sia di una successiva — avvisa gli interessati (il cliente
 * autore, il commercialista di riferimento e il collaboratore assegnato alla
 * pratica) tramite il pattern Observer, che ne cura la persistenza e l'invio via
 * email. Anche in questo caso la cancellazione è logica: file e metadati restano
 * conservati per finalità di audit, pur risultando esclusi dalle interrogazioni
 * ordinarie.
 */
@Service
@RequiredArgsConstructor
public class DocumentoServiceImpl implements DocumentoService {


    private final DocumentoRepo documentoRepository;
    private final CollaboratoreRepo collaboratoreRepository;
    private final ClienteRepo clienteRepository;
    private final InvitoCollaborazioneRepo invitoRepository;
    private final GestoreNotifiche gestoreNotifiche;


    @Override
    public Documento caricaDocumento(Documento documento) {
        documento.setVersione(1);
        documento.setStato(StatoDocumento.IN_REVISIONE);
        Documento salvato = documentoRepository.save(documento);

        // A caricamento avvenuto, avvisa gli interessati tramite il pattern
        // Observer (notifica persistita + email): conferma all'autore, avviso al
        // commercialista di riferimento e al collaboratore assegnato alla pratica.
        inviaNotificheCaricamento(salvato, false);

        return salvato;
    }

    /**
     * Emette le notifiche conseguenti al caricamento di un documento. È
     * un'operazione best-effort e accessoria: il documento è già stato
     * persistito, quindi i destinatari mancanti vengono semplicemente saltati
     * senza compromettere l'esito del caricamento.
     *
     * Destinatari:
     * <ul>
     *   <li>il cliente che ha caricato — conferma dell'avvenuto caricamento;</li>
     *   <li>il commercialista di riferimento del cliente;</li>
     *   <li>il collaboratore assegnato alla pratica, se presente.</li>
     * </ul>
     *
     * @param nuovaVersione {@code true} se si tratta del caricamento di una
     *                      nuova versione di un documento esistente, così da
     *                      adattare il testo dei messaggi.
     */
    private void inviaNotificheCaricamento(Documento documento, boolean nuovaVersione) {
        Cliente autore = documento.getCaricatoDa();
        Pratica pratica = documento.getPratica();
        if (autore == null || pratica == null) {
            return; // contesto incompleto: nessun destinatario individuabile
        }

        String nomeDoc   = documento.getNome();
        Long   praticaId = pratica.getId();
        String nomeAutore = (safe(autore.getNome()) + " " + safe(autore.getCognome())).trim();

        String oggetto = nuovaVersione
                ? "una nuova versione del documento «" + nomeDoc + "»"
                : "il documento «" + nomeDoc + "»";
        String avviso = "Il cliente " + nomeAutore + " ha caricato " + oggetto
                + " nella pratica #" + praticaId + ".";
        String conferma = nuovaVersione
                ? "La nuova versione del documento «" + nomeDoc
                        + "» è stata caricata correttamente nella pratica #" + praticaId + "."
                : "Il tuo documento «" + nomeDoc
                        + "» è stato caricato correttamente nella pratica #" + praticaId + ".";

        // 1) Conferma all'autore del caricamento
        notifica(autore, conferma);

        // 2) Avviso al commercialista di riferimento del cliente.
        //    La relazione cliente→commercialista è LAZY: la si carica con una
        //    query dedicata per evitare LazyInitializationException.
        clienteRepository.findByIdConCommercialista(autore.getId())
                .map(Cliente::getCommercialista)
                .ifPresent(commercialista -> notifica(commercialista, avviso));

        // 3) Avviso al collaboratore assegnato alla pratica, se presente
        Collaboratore assegnatario = pratica.getAssegnataA();
        if (assegnatario != null) {
            notifica(assegnatario, avviso);
        }
    }

    /** Crea e propaga una notifica di tipo DOCUMENTO_CARICATO al destinatario. */
    private void notifica(Utente destinatario, String messaggio) {
        notifica(destinatario, messaggio, TipoNotifica.DOCUMENTO_CARICATO);
    }

    /** Crea e propaga una notifica del tipo indicato al destinatario. */
    private void notifica(Utente destinatario, String messaggio, TipoNotifica tipo) {
        Notifica notifica = new Notifica();
        notifica.setDestinatario(destinatario);
        notifica.setMessaggio(messaggio);
        notifica.setTipo(tipo);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    public Documento trovaPerId(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", "id", id));
    }

    /**
     * Variante sicura con ownership check in base al ruolo:
     * - CLIENTE: deve essere l'autore del documento
     * - COLLABORATORE: deve essere il revisore assegnato
     * - COMMERCIALISTA: deve essere il commercialista del cliente proprietario
     */
    @Override
    public Documento trovaPerId(Long id, Utente richiedente) {
        Documento doc = trovaPerId(id);

        boolean autorizzato = switch (richiedente) {
            case com.tesi.gestionalec.model.Cliente c ->
                    doc.getCaricatoDa() != null
                    && doc.getCaricatoDa().getId().equals(c.getId());
            case com.tesi.gestionalec.model.Collaboratore col ->
                    doc.getRevisore() != null
                    && doc.getRevisore().getId().equals(col.getId());
            case Commercialista comm ->
                    doc.getPratica() != null
                    && doc.getPratica().getCliente() != null
                    && doc.getPratica().getCliente().getCommercialista() != null
                    && doc.getPratica().getCliente().getCommercialista().getId().equals(comm.getId());
            default -> false;
        };

        if (!autorizzato) {
            throw new ForbiddenOperationException(
                    "Non sei autorizzato ad accedere a questo documento");
        }
        return doc;
    }

    @Override
    public List<Documento> trovaPerPratica(Pratica pratica) {
        return documentoRepository.findByPratica(pratica);
    }

    @Override
    public Documento nuovaVersione(Long documentoId, Documento nuovoDocumento) {
        Documento vecchio = trovaPerId(documentoId);
        nuovoDocumento.setVersione(vecchio.getVersione() + 1);  // numero di versione incrementato
        nuovoDocumento.setStato(StatoDocumento.IN_REVISIONE);    // la nuova versione torna in revisione
        nuovoDocumento.setPratica(vecchio.getPratica());         // pratica invariata
        nuovoDocumento.setCaricatoDa(vecchio.getCaricatoDa());   // cliente invariato
        // Il tipo di file è facoltativo: se assente, si eredita dalla versione precedente
        if (nuovoDocumento.getTipoFile() == null || nuovoDocumento.getTipoFile().isBlank()) {
            nuovoDocumento.setTipoFile(vecchio.getTipoFile());
        }
        Documento salvato = documentoRepository.save(nuovoDocumento);

        // Stessa notifica del primo caricamento, con testo dedicato alla nuova versione
        inviaNotificheCaricamento(salvato, true);

        return salvato;
    }

    /**
     * Variante sicura: verifica che il richiedente sia il cliente che ha
     * originariamente caricato il documento prima di permettere il versionamento.
     */
    @Override
    public Documento nuovaVersione(Long documentoId, Documento nuovoDocumento, Utente richiedente) {
        Documento vecchio = trovaPerId(documentoId);
        if (vecchio.getCaricatoDa() == null
                || !vecchio.getCaricatoDa().getId().equals(richiedente.getId())) {
            throw new ForbiddenOperationException(
                    "Non sei autorizzato a caricare una nuova versione di questo documento");
        }
        return nuovaVersione(documentoId, nuovoDocumento);
    }

    @Override
    public void assegnaRevisore(Long documentoId, Long collaboratoreId) {
        Documento documento = trovaPerId(documentoId);
        Collaboratore collaboratore = collaboratoreRepository.findById(collaboratoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboratore", "id", collaboratoreId));
        documento.setRevisore(collaboratore);
        documentoRepository.save(documento);
    }

    /**
     * Variante sicura: il documento deve appartenere allo studio del
     * commercialista richiedente e il collaboratore-revisore deve far parte
     * dello stesso studio, così da impedire assegnazioni cross-studio (IDOR).
     */
    @Override
    public void assegnaRevisore(Long documentoId, Long collaboratoreId, Utente richiedente) {
        // 1) il documento deve appartenere allo studio del richiedente
        trovaPerId(documentoId, richiedente);
        // 2) il collaboratore deve far parte dello studio del richiedente
        if (!invitoRepository.existsByCommercialista_IdAndCollaboratore_IdAndStato(
                richiedente.getId(), collaboratoreId, StatoInvito.ACCEPTED)) {
            throw new ForbiddenOperationException(
                    "Questo collaboratore non fa parte del tuo studio");
        }
        assegnaRevisore(documentoId, collaboratoreId);
    }

    /**
     * Approva un documento dopo aver verificato l'ownership in base al ruolo del
     * richiedente (commercialista dello studio o revisore assegnato), quindi
     * avvisa il cliente autore dell'esito.
     */
    @Override
    public void approvaDocumento(Long id, Utente richiedente) {
        Documento documento = trovaPerId(id, richiedente);
        documento.setStato(StatoDocumento.APPROVATO);
        documentoRepository.save(documento);
        notificaEsitoRevisione(documento);
    }

    /**
     * Rifiuta un documento con la relativa motivazione, con la stessa verifica di
     * ownership dell'approvazione e l'avviso al cliente autore.
     */
    @Override
    public void rifiutaDocumento(Long id, String motivazione, Utente richiedente) {
        Documento documento = trovaPerId(id, richiedente);
        documento.setStato(StatoDocumento.RIFIUTATO);
        documento.setMotivazioneRifiuto(motivazione);
        documentoRepository.save(documento);
        notificaEsitoRevisione(documento);
    }

    /**
     * Avvisa il cliente autore dell'esito della revisione del suo documento
     * (in-app + email tramite Observer), includendo la motivazione in caso di
     * rifiuto. Operazione best-effort: se l'autore non è disponibile la notifica
     * viene omessa.
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

        notifica(autore, messaggio,
                approvato ? TipoNotifica.DOCUMENTO_APPROVATO : TipoNotifica.DOCUMENTO_RIFIUTATO);
    }

    /**
     * Cancellazione logica del documento: né il file né il record vengono
     * rimossi. Il documento viene marcato come eliminato con il relativo
     * timestamp e risulta escluso dalle query ordinarie, mentre file fisico e
     * metadati restano conservati per l'audit fiscale.
     */
    @Override
    public void eliminaDocumento(Long id) {
        eliminaDocumento(trovaPerId(id));
    }

    /**
     * Variante sicura: il documento deve appartenere allo studio del
     * commercialista richiedente (ownership check delegato a trovaPerId).
     */
    @Override
    public void eliminaDocumento(Long id, Utente richiedente) {
        eliminaDocumento(trovaPerId(id, richiedente));
    }

    private void eliminaDocumento(Documento documento) {
        documento.setDeleted(true);
        documento.setDeletedAt(LocalDateTime.now());
        documentoRepository.save(documento);
    }

}
