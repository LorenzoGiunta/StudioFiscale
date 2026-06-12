package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.dto.request.MessaggioChatRequest;
import com.tesi.gestionalec.dto.response.MessaggioChatResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.mapper.MessaggioChatMapper;
import com.tesi.gestionalec.mapper.UtenteMapper;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.ClienteRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.repository.MessaggioChatRepo;
import com.tesi.gestionalec.service.interfaces.ChatService;
import com.tesi.gestionalec.service.interfaces.UtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servizio di messaggistica interna tra gli utenti del gestionale.
 *
 * Oltre a persistere e recuperare i messaggi, applica le regole di
 * visibilità della chat: due utenti possono scriversi solo se i loro ruoli
 * sono compatibili e se esiste un collegamento effettivo tra loro (rapporto
 * cliente-commercialista o collaborazione attiva). Questo impedisce
 * comunicazioni tra utenti non correlati.
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final MessaggioChatRepo repo;
    private final UtenteService utenteService;
    private final ClienteRepo clienteRepo;
    private final InvitoCollaborazioneRepo invitoRepo;

    @Override
    @Transactional
    public MessaggioChatResponse salvaEInvia(MessaggioChatRequest request, Utente mittente) {
        Utente destinatario = utenteService.trovaPerId(request.getDestinatarioId());

        // Primo controllo, rapido: compatibilità tra i ruoli di mittente e destinatario
        if (!combinazioneValida(mittente.getRuolo(), destinatario.getRuolo())) {
            throw new AccessDeniedException("Non puoi inviare messaggi a questo utente");
        }

        // Secondo controllo: il destinatario deve essere tra i contatti collegati al
        // mittente
        boolean collegato = contattiCollegati(mittente).stream()
                .anyMatch(u -> u != null && u.getId().equals(destinatario.getId()));
        if (!collegato) {
            throw new AccessDeniedException("Puoi scrivere solo agli utenti a cui sei collegato");
        }

        MessaggioChat messaggio = new MessaggioChat();
        messaggio.setMittente(mittente);
        messaggio.setDestinatario(destinatario);
        messaggio.setTesto(request.getTesto());
        messaggio.setLetto(false);

        return MessaggioChatMapper.toResponse(repo.save(messaggio));
    }

    @Override
    public List<MessaggioChatResponse> storico(Long utenteAId, Long utenteBId) {
        return repo.trovaStotico(utenteAId, utenteBId)
                .stream()
                .map(MessaggioChatMapper::toResponse)
                .toList();
    }

    @Override
    public Map<Long, Long> nonLettiPerMittente(Long utenteId) {
        Map<Long, Long> risultato = new LinkedHashMap<>();
        for (Object[] riga : repo.contaNonLettiPerMittente(utenteId)) {
            risultato.put((Long) riga[0], (Long) riga[1]);
        }
        return risultato;
    }

    @Override
    @Transactional
    public void segnaLetti(Long utenteId, Long altroUtenteId) {
        repo.segnaLetti(utenteId, altroUtenteId);
    }

    /**
     * Restituisce i contatti di chat dell'utente, cioè le sole persone con cui
     * esiste un collegamento reale e non tutti gli utenti di ruolo compatibile.
     * In funzione del ruolo:
     * <ul>
     * <li>il commercialista vede i propri clienti e i collaboratori con
     * collaborazione accettata;</li>
     * <li>il cliente vede il proprio commercialista e i collaboratori di
     * quest'ultimo;</li>
     * <li>il collaboratore vede i commercialisti con cui collabora e i loro
     * clienti;</li>
     * <li>l'amministratore non utilizza la chat.</li>
     * </ul>
     */
    @Override
    @Transactional(readOnly = true)
    public List<UtenteResponse> trovaContatti(Utente utente) {
        // Rimuove i duplicati per id (un contatto può emergere da più relazioni),
        // esclude l'utente stesso e gli account disabilitati, preservando l'ordine.
        return contattiCollegati(utente).stream()
                .filter(u -> u != null && u.isEnabled() && !u.getId().equals(utente.getId()))
                .collect(Collectors.toMap(Utente::getId, u -> u, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .map(UtenteMapper::toResponse)
                .toList();
    }

    // Entità collegate all'utente in base al ruolo, senza filtri né deduplicazione
    private List<Utente> contattiCollegati(Utente utente) {
        return switch (utente.getRuolo()) {
            case COMMERCIALISTA -> contattiDelCommercialista(utente.getId());
            case CLIENTE -> contattiDelCliente(utente.getId());
            case COLLABORATORE -> contattiDelCollaboratore(utente.getId());
            case AMMINISTRATORE -> List.of();
        };
    }

    private List<Utente> contattiDelCommercialista(Long commId) {
        List<Utente> out = new ArrayList<>(clienteRepo.findByCommercialistaId(commId));
        invitoRepo.findCollaboratoriAttiviByCommercialista(commId)
                .forEach(i -> out.add(i.getCollaboratore()));
        return out;
    }

    private List<Utente> contattiDelCliente(Long clienteId) {
        Cliente cliente = clienteRepo.findById(clienteId).orElse(null);
        if (cliente == null || cliente.getCommercialista() == null)
            return List.of();
        Commercialista comm = cliente.getCommercialista();
        List<Utente> out = new ArrayList<>();
        out.add(comm);
        invitoRepo.findCollaboratoriAttiviByCommercialista(comm.getId())
                .forEach(i -> out.add(i.getCollaboratore()));
        return out;
    }

    private List<Utente> contattiDelCollaboratore(Long collabId) {
        List<Utente> out = new ArrayList<>();
        invitoRepo.findCommercialistiAttiviByCollaboratore(collabId).forEach(i -> {
            Commercialista comm = i.getCommercialista();
            out.add(comm);
            out.addAll(clienteRepo.findByCommercialistaId(comm.getId()));
        });
        return out;
    }

    private boolean combinazioneValida(Ruolo mittente, Ruolo destinatario) {
        return switch (mittente) {
            case CLIENTE -> destinatario == Ruolo.COMMERCIALISTA || destinatario == Ruolo.COLLABORATORE;
            case COMMERCIALISTA -> destinatario == Ruolo.CLIENTE || destinatario == Ruolo.COLLABORATORE;
            case COLLABORATORE -> destinatario == Ruolo.CLIENTE || destinatario == Ruolo.COMMERCIALISTA;
            case AMMINISTRATORE -> false;
        };
    }
}