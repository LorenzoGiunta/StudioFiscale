package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.repository.NotificaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.NotificaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificaServiceImpl implements NotificaService {


    private final NotificaRepo notificaRepo;
    private final UtenteRepo utenteRepository;

    @Override
    public List<Notifica> trovaPerUtente(Utente utente) {
        return notificaRepo.findByDestinatario(utente);
    }

    @Override
    public Page<Notifica> trovaPerUtente(Utente utente, Pageable pageable) {
        return notificaRepo.findByDestinatario(utente, pageable);
    }

    @Override
    public List<Notifica> trovaNonLette(Utente utente) {
        return notificaRepo.findByDestinatarioAndLettaFalse(utente);
    }

    @Override
    public Page<Notifica> trovaNonLette(Utente utente, Pageable pageable) {
        return notificaRepo.findByDestinatarioAndLettaFalse(utente, pageable);
    }

    @Override
    public void segnaComeLetta(Long notificaId) {
        Notifica notifica = notificaRepo.findById(notificaId)
                .orElseThrow(() -> new ResourceNotFoundException("Notifica", "id", notificaId));

        notifica.setLetta(true);
        notificaRepo.save(notifica);
    }

    /**
     * Variante sicura: verifica che la notifica appartenga all'utente loggato
     * prima di aggiornarla, impedendo che un utente segni come lette le
     * notifiche altrui (IDOR).
     */
    @Override
    public void segnaComeLetta(Long notificaId, Utente richiedente) {
        Notifica notifica = notificaRepo.findById(notificaId)
                .orElseThrow(() -> new ResourceNotFoundException("Notifica", "id", notificaId));

        if (!notifica.getDestinatario().getId().equals(richiedente.getId())) {
            throw new com.tesi.gestionalec.exception.ForbiddenOperationException(
                    "Non puoi modificare notifiche di altri utenti");
        }

        notifica.setLetta(true);
        notificaRepo.save(notifica);
    }

    @Override
    public void segnaComeLetteTutte(Long utenteId) {
        Utente utente = utenteRepository.findById(utenteId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente", "id", utenteId));

        List<Notifica> nonLette = notificaRepo.findByDestinatarioAndLettaFalse(utente);
        nonLette.forEach(n -> n.setLetta(true));
        notificaRepo.saveAll(nonLette);
    }
}
