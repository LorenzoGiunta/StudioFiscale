package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Collaboratore;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.repository.*;
import com.tesi.gestionalec.service.interfaces.CalcoloImposteService;
import com.tesi.gestionalec.service.interfaces.CommercialistaService;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Servizio specifico per i commercialisti, estensione del servizio utenti.
 *
 * Offre le funzionalità di studio: gestione delle pratiche e delle relative
 * assegnazioni, accesso a clienti, collaboratori e documenti, e calcolo delle
 * imposte di un cliente delegato al pattern Strategy.
 */
@Service
public class CommercialistaServiceImpl extends UtenteServiceImpl implements CommercialistaService {

    private final PraticaRepo praticaRepository;
    private final CollaboratoreRepo collaboratoreRepository;
    private final ClienteRepo clienteRepo;
    private final CommercialistaRepo commercialistaRepo;
    private final DocumentoRepo documentoRepo;
    private final CalcoloImposteService calcoloImposte;
    private final PraticaService praticaService;

    public CommercialistaServiceImpl(
            UtenteRepo utenteRepository,
            PasswordEncoder passwordEncoder,
            PraticaRepo praticaRepository,
            CollaboratoreRepo collaboratoreRepository,
            ClienteRepo clienteRepo,
            CommercialistaRepo commercialistaRepo,
            DocumentoRepo documentoRepo,
            CalcoloImposteService calcoloImposte,
            PraticaService praticaService) {
        super(utenteRepository, passwordEncoder, commercialistaRepo);
        this.praticaRepository = praticaRepository;
        this.collaboratoreRepository = collaboratoreRepository;
        this.clienteRepo = clienteRepo;
        this.commercialistaRepo = commercialistaRepo;
        this.documentoRepo = documentoRepo;
        this.calcoloImposte = calcoloImposte;
        this.praticaService = praticaService;
    }

    @Override
    public List<Pratica> trovaTutteLePratiche() {
        return praticaRepository.findAll();
    }

    @Override
    public void assegnaCollaboratore(Long praticaId, Long collaboratoreId) {
        Pratica pratica = trovaById(praticaId);

        Collaboratore collaboratore = collaboratoreRepository.findById(collaboratoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboratore", "id", collaboratoreId));

        pratica.setAssegnataA(collaboratore);
        praticaRepository.save(pratica);
    }

    /**
     * Avanzamento di stato di una pratica. La logica non è duplicata qui: viene
     * delegata al servizio delle pratiche, che gestisce transazione, transizione
     * (pattern State) e notifica al cliente in modo uniforme per tutti i
     * chiamanti.
     */
    @Override
    public void avanzaStatoPratica(Long praticaId) {
        praticaService.avanzaStato(praticaId);
    }

    @Override
    public double calcolaImposteCliente(Long clienteId) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clienteId));
        return calcoloImposte.CalcolaPerCliente(cliente);
    }

    @Override
    public List<Cliente> trovaTuttiClienti() {
        return clienteRepo.findAll();
    }

    @Override
    public List<Cliente> trovaClientiDelCommercialista(Long commercialistaId) {
        return clienteRepo.findByCommercialistaId(commercialistaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Collaboratore> trovaMieiCollaboratori(Long commercialistaId) {
        Commercialista comm = commercialistaRepo.findById(commercialistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Commercialista", "id", commercialistaId));
        // I collaboratori sono caricati in modo differito: vanno inizializzati
        // entro la transazione, altrimenti il successivo mapping fallirebbe.
        List<Collaboratore> collaboratori = comm.getCollaboratori(); // solo collaborazioni accettate
        collaboratori.forEach(org.hibernate.Hibernate::initialize);
        return new ArrayList<>(collaboratori);
    }

    @Override
    public List<Documento> trovaDocumentiStudio(Long commercialistaId) {
        return documentoRepo.findByCommercialista(commercialistaId);
    }

    /**
     * Verifica che il cliente con clienteId sia effettivamente associato al
     * commercialista con commercialistaId. Lancia ForbiddenOperationException
     * se il cliente appartiene a un altro commercialista o non ha commercialista.
     */
    @Override
    public void verificaAppartenenzaCliente(Long clienteId, Long commercialistaId) {
        Cliente cliente = clienteRepo.findByIdConCommercialista(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clienteId));
        if (cliente.getCommercialista() == null
                || !cliente.getCommercialista().getId().equals(commercialistaId)) {
            throw new ForbiddenOperationException(
                    "Questo cliente non appartiene al tuo studio");
        }
    }

    private Pratica trovaById(Long id) {
        return praticaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pratica", "id", id));
    }
}
