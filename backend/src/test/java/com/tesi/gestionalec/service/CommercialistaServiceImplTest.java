package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.*;
import com.tesi.gestionalec.service.impl.CalcoloImposteServiceImpl;
import com.tesi.gestionalec.service.impl.CommercialistaServiceImpl;
import com.tesi.gestionalec.state.BozzaState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari per CommercialistaServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommercialistaService – Unit Tests")
class CommercialistaServiceImplTest {

    @Mock UtenteRepo utenteRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PraticaRepo praticaRepo;
    @Mock CollaboratoreRepo collaboratoreRepo;
    @Mock ClienteRepo clienteRepo;
    @Mock com.tesi.gestionalec.repository.CommercialistaRepo commercialistaRepo;
    @Mock com.tesi.gestionalec.repository.DocumentoRepo documentoRepo;
    @Mock CalcoloImposteServiceImpl calcoloImposte;

    CommercialistaServiceImpl commercialistaService;

    private Pratica pratica;
    private Collaboratore collaboratore;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        commercialistaService = new CommercialistaServiceImpl(
                utenteRepo, passwordEncoder, praticaRepo,
                collaboratoreRepo, clienteRepo, commercialistaRepo,
                documentoRepo, calcoloImposte);

        collaboratore = new Collaboratore();
        collaboratore.setId(5L);
        collaboratore.setNome("Luca");
        collaboratore.setCognome("Bianchi");

        cliente = new Cliente();
        cliente.setId(3L);
        cliente.setNome("Anna");
        cliente.setCognome("Verdi");
        cliente.setRegime(RegimeFiscale.ORDINARIO);
        cliente.setRedditoAnnuo(40000.0);

        pratica = new Pratica();
        pratica.setId(10L);
        pratica.setCliente(cliente);
        pratica.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        pratica.setStato(StatoPratica.BOZZA);
        pratica.setStatoCorrente(new BozzaState());
    }

    // ─── trovaTutteLePratiche ─────────────────────────────────────────────────

    @Test
    @DisplayName("trovaTutteLePratiche: delega findAll al repo")
    void trovaTutteLePratiche_delegaAlRepo() {
        when(praticaRepo.findAll()).thenReturn(List.of(pratica));

        List<Pratica> risultato = commercialistaService.trovaTutteLePratiche();

        assertThat(risultato).hasSize(1);
        verify(praticaRepo).findAll();
    }

    // ─── assegnaCollaboratore ─────────────────────────────────────────────────

    @Test
    @DisplayName("assegnaCollaboratore: setta collaboratore sulla pratica e salva")
    void assegnaCollaboratore_settaESalva() {
        when(praticaRepo.findById(10L)).thenReturn(Optional.of(pratica));
        when(collaboratoreRepo.findById(5L)).thenReturn(Optional.of(collaboratore));
        when(praticaRepo.save(any())).thenReturn(pratica);

        commercialistaService.assegnaCollaboratore(10L, 5L);

        assertThat(pratica.getAssegnataA()).isEqualTo(collaboratore);
        verify(praticaRepo).save(pratica);
    }

    @Test
    @DisplayName("assegnaCollaboratore: pratica non trovata → ResourceNotFoundException")
    void assegnaCollaboratore_praticaNonTrovata_lanciaEccezione() {
        when(praticaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commercialistaService.assegnaCollaboratore(99L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("assegnaCollaboratore: collaboratore non trovato → ResourceNotFoundException")
    void assegnaCollaboratore_collaboratoreNonTrovato_lanciaEccezione() {
        when(praticaRepo.findById(10L)).thenReturn(Optional.of(pratica));
        when(collaboratoreRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commercialistaService.assegnaCollaboratore(10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── avanzaStatoPratica ───────────────────────────────────────────────────

    @Test
    @DisplayName("avanzaStatoPratica: avanza stato da BOZZA a IN_LAVORAZIONE")
    void avanzaStatoPratica_daBozzaAInLavorazione() {
        when(praticaRepo.findById(10L)).thenReturn(Optional.of(pratica));
        when(praticaRepo.save(any())).thenReturn(pratica);

        commercialistaService.avanzaStatoPratica(10L);

        assertThat(pratica.getStato()).isEqualTo(StatoPratica.IN_LAVORAZIONE);
        verify(praticaRepo).save(pratica);
    }

    @Test
    @DisplayName("avanzaStatoPratica: pratica non trovata → ResourceNotFoundException")
    void avanzaStatoPratica_nonTrovata_lanciaEccezione() {
        when(praticaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commercialistaService.avanzaStatoPratica(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── calcolaImposteCliente ────────────────────────────────────────────────

    @Test
    @DisplayName("calcolaImposteCliente: delega al CalcoloImposteService")
    void calcolaImposteCliente_delegaAlServizio() {
        when(clienteRepo.findById(3L)).thenReturn(Optional.of(cliente));
        when(calcoloImposte.CalcolaPerCliente(cliente)).thenReturn(8500.0);

        double imposte = commercialistaService.calcolaImposteCliente(3L);

        assertThat(imposte).isEqualTo(8500.0);
        verify(calcoloImposte).CalcolaPerCliente(cliente);
    }

    @Test
    @DisplayName("calcolaImposteCliente: cliente non trovato → ResourceNotFoundException")
    void calcolaImposteCliente_clienteNonTrovato_lanciaEccezione() {
        when(clienteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commercialistaService.calcolaImposteCliente(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── trovaTuttiClienti ────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaTuttiClienti: delega findAll a ClienteRepo")
    void trovaTuttiClienti_delegaAlRepo() {
        when(clienteRepo.findAll()).thenReturn(List.of(cliente));

        List<Cliente> risultato = commercialistaService.trovaTuttiClienti();

        assertThat(risultato).hasSize(1).contains(cliente);
        verify(clienteRepo).findAll();
    }

    @Test
    @DisplayName("trovaClientiDelCommercialista: filtra per commercialista_id")
    void trovaClientiDelCommercialista_filtraPerId() {
        when(clienteRepo.findByCommercialistaId(7L)).thenReturn(List.of(cliente));

        List<Cliente> risultato = commercialistaService.trovaClientiDelCommercialista(7L);

        assertThat(risultato).hasSize(1).contains(cliente);
        verify(clienteRepo).findByCommercialistaId(7L);
    }

    @Test
    @DisplayName("trovaClientiDelCommercialista: commercialista senza clienti → lista vuota")
    void trovaClientiDelCommercialista_nessunCliente_listaVuota() {
        when(clienteRepo.findByCommercialistaId(99L)).thenReturn(List.of());

        List<Cliente> risultato = commercialistaService.trovaClientiDelCommercialista(99L);

        assertThat(risultato).isEmpty();
    }

    // ─── trovaMieiCollaboratori ───────────────────────────────────────────────

    @Test
    @DisplayName("trovaMieiCollaboratori: ritorna lista filtrata ACCEPTED dal Commercialista")
    void trovaMieiCollaboratori_filtraACCEPTED() {
        com.tesi.gestionalec.model.Commercialista comm = new com.tesi.gestionalec.model.Commercialista();
        comm.setId(7L);
        com.tesi.gestionalec.model.InvitoCollaborazione inv1 = new com.tesi.gestionalec.model.InvitoCollaborazione();
        inv1.setStato(com.tesi.gestionalec.model.StatoInvito.ACCEPTED);
        inv1.setCollaboratore(collaboratore);
        com.tesi.gestionalec.model.InvitoCollaborazione inv2 = new com.tesi.gestionalec.model.InvitoCollaborazione();
        inv2.setStato(com.tesi.gestionalec.model.StatoInvito.PENDING);
        inv2.setCollaboratore(collaboratore);
        comm.setInviti(new java.util.ArrayList<>(List.of(inv1, inv2)));

        when(commercialistaRepo.findById(7L)).thenReturn(Optional.of(comm));

        List<Collaboratore> risultato = commercialistaService.trovaMieiCollaboratori(7L);

        assertThat(risultato).hasSize(1);   // solo ACCEPTED
        verify(commercialistaRepo).findById(7L);
    }

    @Test
    @DisplayName("trovaMieiCollaboratori: commercialista non trovato → ResourceNotFoundException")
    void trovaMieiCollaboratori_nonTrovato_lanciaEccezione() {
        when(commercialistaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commercialistaService.trovaMieiCollaboratori(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── trovaDocumentiStudio ─────────────────────────────────────────────────

    @Test
    @DisplayName("trovaDocumentiStudio: delega findByCommercialista a DocumentoRepo")
    void trovaDocumentiStudio_delegaAlRepo() {
        com.tesi.gestionalec.model.Documento doc = new com.tesi.gestionalec.model.Documento();
        doc.setId(1L);
        when(documentoRepo.findByCommercialista(7L)).thenReturn(List.of(doc));

        var risultato = commercialistaService.trovaDocumentiStudio(7L);

        assertThat(risultato).hasSize(1);
        verify(documentoRepo).findByCommercialista(7L);
    }

    // ─── verificaAppartenenzaCliente ──────────────────────────────────────────

    @Test
    @DisplayName("verificaAppartenenzaCliente: cliente del commercialista → nessuna eccezione")
    void verificaAppartenenzaCliente_clienteCorretto_nessunEccezione() {
        Commercialista comm = new Commercialista();
        comm.setId(7L);
        Cliente c = new Cliente();
        c.setId(3L);
        c.setCommercialista(comm);
        when(clienteRepo.findByIdConCommercialista(3L)).thenReturn(Optional.of(c));

        assertThatNoException().isThrownBy(
                () -> commercialistaService.verificaAppartenenzaCliente(3L, 7L));
    }

    @Test
    @DisplayName("verificaAppartenenzaCliente: cliente di altro commercialista → ForbiddenOperationException")
    void verificaAppartenenzaCliente_clienteAltrui_lanciaForbidden() {
        Commercialista altroComm = new Commercialista();
        altroComm.setId(99L);
        Cliente c = new Cliente();
        c.setId(3L);
        c.setCommercialista(altroComm);
        when(clienteRepo.findByIdConCommercialista(3L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commercialistaService.verificaAppartenenzaCliente(3L, 7L))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class)
                .hasMessageContaining("studio");
    }

    @Test
    @DisplayName("verificaAppartenenzaCliente: cliente inesistente → ResourceNotFoundException")
    void verificaAppartenenzaCliente_clienteInesistente_lanciaNotFound() {
        when(clienteRepo.findByIdConCommercialista(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commercialistaService.verificaAppartenenzaCliente(99L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
