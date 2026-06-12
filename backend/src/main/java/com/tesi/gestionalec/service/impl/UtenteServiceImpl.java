package com.tesi.gestionalec.service.impl;


import com.tesi.gestionalec.dto.request.RegistrazioneRequest;
import com.tesi.gestionalec.exception.EmailAlreadyExistsException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.Ruolo;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.UtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servizio centrale per la gestione degli utenti.
 *
 * Concentra la registrazione, la ricerca e le operazioni amministrative sul
 * ciclo di vita degli account, adottando la cancellazione logica (soft delete)
 * per preservare lo storico. Funge inoltre da {@link UserDetailsService} per
 * Spring Security, fornendo l'utente a partire dall'email in fase di login.
 */
@Primary
@Service
@RequiredArgsConstructor
public class UtenteServiceImpl implements UtenteService, UserDetailsService {

    protected final UtenteRepo repo;
    protected final PasswordEncoder passwordEncoder;
    protected final CommercialistaRepo commercialistaRepo;

    @Override
    public Utente registra(Utente utente) {
        return registra(utente, null);
    }

    /**
     * Variante di registrazione che gestisce anche l'associazione tra cliente e
     * commercialista: se l'identificativo è valorizzato e l'utente è un cliente,
     * il commercialista viene caricato e collegato prima del salvataggio.
     */
    public Utente registra(Utente utente, Long commercialistaId) {
        // La verifica dell'email considera anche gli utenti cancellati
        // logicamente, che mantengono il vincolo di unicità sul database: in
        // questo modo si restituisce un conflitto esplicito anziché lasciar
        // emergere una violazione di integrità a basso livello.
        if (repo.countByEmailIncludeDeleted(utente.getEmail()) > 0) {
            throw new EmailAlreadyExistsException(utente.getEmail());
        }

        utente.setPassword(passwordEncoder.encode(utente.getPassword()));
        utente.setEnabled(true);

        if (utente instanceof Cliente cliente && commercialistaId != null) {
            Commercialista comm = commercialistaRepo.findById(commercialistaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commercialista", "id", commercialistaId));
            cliente.setCommercialista(comm);
        }

        return repo.save(utente);
    }

    /**
     * Valida i campi obbligatori che dipendono dal ruolo richiesto.
     *
     * Si tratta di vincoli condizionali (un dato è richiesto solo per certi
     * ruoli) che non sono esprimibili con le sole annotazioni di validazione
     * dichiarativa e vengono pertanto verificati esplicitamente nel servizio.
     */
    public void validaPerRuolo(RegistrazioneRequest request) {
        if (request.getRuolo() == Ruolo.CLIENTE) {
            if (request.getCodFiscale() == null || request.getCodFiscale().isBlank()) {
                throw new IllegalArgumentException("Il codice fiscale è obbligatorio per un cliente");
            }
            if (request.getRegime() == null || request.getRegime().isBlank()) {
                throw new IllegalArgumentException("Il regime fiscale è obbligatorio per un cliente");
            }
        }
        if (request.getRuolo() == Ruolo.COMMERCIALISTA) {
            if (request.getNumeroAlbo() == null || request.getNumeroAlbo().isBlank()) {
                throw new IllegalArgumentException("Il numero di albo è obbligatorio per un commercialista");
            }
        }
    }

    @Override
    public Utente trovaPerId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente", "id", id));
    }

    @Override
    public Utente trovaPerEmail(String email) {
        return repo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utente", "email", email));
    }

    @Override
    public List<Utente> trovaTutti() {
        return repo.findAll();
    }

    @Override
    public Page<Utente> trovaTutti(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override
    public void abilitaUtente(Long id) {
        Utente utente = trovaPerId(id);
        utente.setEnabled(true);
        repo.save(utente);
    }

    @Override
    public void disabilitaUtente(Long id) {
        Utente utente = trovaPerId(id);
        utente.setEnabled(false);
        repo.save(utente);
    }

    /**
     * Cancellazione logica dell'utente: il record non viene rimosso, ma marcato
     * come eliminato con il relativo timestamp. Il filtro applicato a livello di
     * entità lo rende invisibile alle query ordinarie, preservandone lo storico.
     */
    @Override
    public void eliminaUtente(Long id) {
        Utente utente = trovaPerId(id);
        utente.setDeleted(true);
        utente.setDeletedAt(LocalDateTime.now());
        utente.setEnabled(false);   // preclude il login anche se il filtro venisse aggirato
        repo.save(utente);
    }

    /**
     * Restituisce gli utenti cancellati logicamente, a uso dell'amministratore
     * per la consultazione e l'eventuale ripristino. Richiede una query nativa
     * che ignori il filtro di esclusione dei record eliminati.
     */
    @Override
    public List<Utente> trovaEliminati() {
        return repo.findAllDeleted();
    }

    /**
     * Annulla la cancellazione logica, riportando l'utente tra quelli attivi e
     * riabilitandone l'accesso. Il recupero avviene tramite query nativa, dato
     * che il filtro standard escluderebbe i record marcati come eliminati.
     */
    @Override
    public void ripristinaUtente(Long id) {
        Utente utente = repo.findByIdIncludeDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente", "id", id));
        utente.setDeleted(false);
        utente.setDeletedAt(null);
        utente.setEnabled(true);
        repo.save(utente);
    }

    /**
     * Punto di integrazione con Spring Security, invocato automaticamente in
     * fase di autenticazione. Gli utenti cancellati logicamente non vengono
     * trovati, così il tentativo di accesso fallisce in modo coerente.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + email));
    }

}
