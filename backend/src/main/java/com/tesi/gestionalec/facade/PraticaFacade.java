package com.tesi.gestionalec.facade;

import com.tesi.gestionalec.dto.request.PraticaRequest;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.mapper.PraticaMapper;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.interfaces.ClienteService;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade per la gestione delle pratiche.
 *
 * Raggruppa dietro un'interfaccia semplice le operazioni composte sul ciclo di
 * vita di una pratica — creazione con eventuale assegnazione e avanzamento di
 * stato — nascondendo ai chiamanti il coordinamento tra i servizi coinvolti e
 * le notifiche emesse di conseguenza. L'utente richiedente viene propagato ai
 * servizi, che lo usano per i controlli di ownership.
 */
@Component
@RequiredArgsConstructor
public class PraticaFacade {

    private final PraticaService praticaService;
    private final ClienteService clienteService;

    /**
     * Crea una nuova pratica per il cliente indicato e, se noto, la assegna
     * subito a un collaboratore.
     *
     * @param collaboratoreId collaboratore incaricato, oppure {@code null}
     * @param richiedente     commercialista che esegue l'operazione
     */
    public PraticaResponse creaEAssegna(PraticaRequest request, Long collaboratoreId, Utente richiedente) {
        Cliente cliente = clienteService.trovaClientePerId(request.getClienteId());

        // La creazione verifica l'ownership e notifica il cliente (Observer)
        Pratica pratica = PraticaMapper.toModel(request, cliente);
        Pratica salvata = praticaService.creaPratica(pratica, richiedente);

        // Assegnazione immediata se il collaboratore è già individuato
        if (collaboratoreId != null) {
            praticaService.assegnaCollaboratore(salvata.getId(), collaboratoreId, richiedente);
            salvata = praticaService.trovaPerId(salvata.getId(), richiedente);
        }

        return PraticaMapper.toResponse(salvata);
    }

    /**
     * Fa avanzare lo stato della pratica e ne restituisce la versione
     * aggiornata. La transizione notifica il cliente tramite il pattern Observer.
     */
    public PraticaResponse avanzaERecupera(Long praticaId, Utente richiedente) {
        praticaService.avanzaStato(praticaId, richiedente);
        return PraticaMapper.toResponse(praticaService.trovaPerId(praticaId, richiedente));
    }
}
