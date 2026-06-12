package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.repository.ClienteRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.ClienteService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servizio specifico per i clienti, estensione del servizio utenti.
 *
 * Aggiunge le operazioni proprie del cliente — ricerca per codice fiscale,
 * accesso a pratiche e documenti, aggiornamento del profilo — riusando la
 * gestione comune ereditata. Le collezioni a caricamento differito vengono
 * inizializzate entro la transazione, così da restare leggibili anche dopo.
 */
@Service
public class ClienteServiceImpl extends UtenteServiceImpl implements ClienteService {

    private final ClienteRepo clienteRepository;

    public ClienteServiceImpl(
            UtenteRepo utenteRepository,
            PasswordEncoder passwordEncoder,
            ClienteRepo clienteRepository,
            CommercialistaRepo commercialistaRepo) {
        super(utenteRepository, passwordEncoder, commercialistaRepo);
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Cliente trovaPerCodFiscale(String codFiscale) {
        return clienteRepository.findByCodFiscale(codFiscale)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "codice fiscale", codFiscale));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pratica> trovaPratiche(Long clienteId) {
        Cliente cliente = trovaClientePerId(clienteId);
        // Inizializza la collezione differita entro la transazione, altrimenti
        // non sarebbe più caricabile a livello di controller.
        return new ArrayList<>(cliente.getPratiche());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Documento> trovaDocumenti(Long clienteId) {
        Cliente cliente = trovaClientePerId(clienteId);
        return new ArrayList<>(cliente.getDocumenti());
    }

    @Override
    public Cliente aggiorna(Long id, Cliente dati) {
        Cliente cliente = trovaClientePerId(id);
        cliente.setNome(dati.getNome());
        cliente.setCognome(dati.getCognome());
        cliente.setEmail(dati.getEmail());
        cliente.setCodFiscale(dati.getCodFiscale());
        cliente.setPIVA(dati.getPIVA());
        cliente.setRegime(dati.getRegime());
        cliente.setRedditoAnnuo(dati.getRedditoAnnuo());
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente trovaClientePerId(Long id) {
        // La query carica contestualmente il commercialista associato, così da
        // renderlo leggibile in fase di mapping anche al di fuori della transazione.
        return clienteRepository.findByIdConCommercialista(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", id));
    }
}
