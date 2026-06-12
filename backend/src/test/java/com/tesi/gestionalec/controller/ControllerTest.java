package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.MessaggioChatRequest;
import com.tesi.gestionalec.dto.request.DocumentoRequest;
import com.tesi.gestionalec.dto.request.PraticaRequest;
import com.tesi.gestionalec.dto.response.MessaggioChatResponse;
import com.tesi.gestionalec.dto.response.NotificaResponse;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.dto.response.UltimaAzioneResponse;
import com.tesi.gestionalec.facade.DocumentoFacade;
import com.tesi.gestionalec.facade.PraticaFacade;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.mapper.PraticaMapper;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.security.GestoreTokenService;
import com.tesi.gestionalec.security.UtentePrincipal;
import com.tesi.gestionalec.service.FileStorageService;
import com.tesi.gestionalec.service.interfaces.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per i controller — invocazione diretta dei metodi.
 * Nessun contesto Spring necessario: si testano le deleghe verso i service
 * e i codici di risposta HTTP. La logica di sicurezza (@PreAuthorize) è
 * verificata separatamente a livello di integration test.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controller – Unit Tests")
class ControllerTest {

    // ─── Mocks comuni ────────────────────────────────────────────────────────

    @Mock PraticaService praticaService;
    @Mock ClienteService clienteService;
    @Mock NotificaService notificaService;
    @Mock CollaboratoreService collaboratoreService;
    @Mock AmministratoreService amministratoreService;
    @Mock UtenteService utenteService;
    @Mock CommercialistaRepo commercialistaRepo;
    @Mock DocumentoService documentoService;
    @Mock FileStorageService fileStorageService;
    @Mock PraticaFacade praticaFacade;
    @Mock DocumentoFacade documentoFacade;
    @Mock ChatService chatService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock InvitoCollaborazioneService invitoService;
    @Mock CommercialistaService commercialistaService;
    @Mock GestoreTokenService gestoreTokenService;
    @Mock com.tesi.gestionalec.observer.GestoreNotifiche gestoreNotifiche;
    @Mock AuthenticationManager authManager;
    @Mock Authentication authentication;

    private Pratica pratica;
    private Cliente cliente;
    private Collaboratore collaboratore;
    private Cliente utenteAutenticato;
    private Documento documento;
    private InvitoCollaborazione invito;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Mario");
        cliente.setCognome("Rossi");

        collaboratore = new Collaboratore();
        collaboratore.setId(5L);
        collaboratore.setNome("Luca");
        collaboratore.setCognome("Bianchi");

        pratica = new Pratica();
        pratica.setId(10L);
        pratica.setCliente(cliente);
        pratica.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        pratica.setStato(StatoPratica.BOZZA);
        pratica.setScadenza(LocalDate.of(2025, 12, 31));
        pratica.setStatoCorrente(new com.tesi.gestionalec.state.BozzaState());

        utenteAutenticato = new Cliente();
        utenteAutenticato.setId(1L);
        utenteAutenticato.setNome("Mario");
        utenteAutenticato.setCognome("Rossi");

        documento = new Documento();
        documento.setId(20L);
        documento.setNome("test.pdf");
        documento.setPercorsoFile("uploads/test.pdf");
        documento.setStato(StatoDocumento.IN_REVISIONE);
        documento.setPratica(pratica);
        documento.setCaricatoDa(cliente);
        documento.setVersione(1);

        Commercialista comm = new Commercialista();
        comm.setId(3L);
        comm.setNome("Giulia");
        comm.setCognome("Bianchi");
        comm.setNumeroAlbo("ALB123");

        invito = new InvitoCollaborazione();
        invito.setId(50L);
        invito.setToken("tok-abc");
        invito.setEmailDestinatario("luca@test.it");
        invito.setStato(StatoInvito.PENDING);
        invito.setCommercialista(comm);
        invito.setScadeIl(java.time.LocalDateTime.now().plusDays(7));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PraticaController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PraticaController.trovaPerId → 200 OK con DTO pratica")
    void pratica_trovaPerId_restituisce200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);
        when(praticaService.trovaPerId(10L, utenteAutenticato)).thenReturn(pratica);

        ResponseEntity<PraticaResponse> resp = controller.trovaPerId(10L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("PraticaController.avanzaStato → 200 OK e delega al service")
    void pratica_avanzaStato_200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);

        when(praticaFacade.avanzaERecupera(10L, utenteAutenticato)).thenReturn(PraticaMapper.toResponse(pratica));

        ResponseEntity<PraticaResponse> resp = controller.avanzaStato(10L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(praticaFacade).avanzaERecupera(10L, utenteAutenticato);
    }

    @Test
    @DisplayName("PraticaController.assegnaCollaboratore → 200 OK")
    void pratica_assegnaCollaboratore_200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);

        ResponseEntity<Void> resp = controller.assegnaCollaboratore(10L, 5L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(praticaService).assegnaCollaboratore(10L, 5L, utenteAutenticato);
    }

    @Test
    @DisplayName("PraticaController.elimina → 204 No Content")
    void pratica_elimina_204() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);

        ResponseEntity<Void> resp = controller.elimina(10L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(praticaService).eliminaPratica(10L, utenteAutenticato);
    }

    @Test
    @DisplayName("PraticaController.crea → 200 OK con DTO creato")
    void pratica_crea_200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);
        PraticaRequest request = new PraticaRequest();
        request.setClienteId(1L);
        request.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        request.setScadenza(LocalDate.of(2025, 12, 31));

        when(praticaFacade.creaEAssegna(any(), isNull(), eq(utenteAutenticato)))
                .thenReturn(PraticaMapper.toResponse(pratica));

        ResponseEntity<PraticaResponse> resp = controller.crea(request, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("PraticaController.trovaTutte → 200 OK con pagina")
    void pratica_trovaTutte_200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);
        Page<Pratica> pagina = new PageImpl<>(List.of(pratica));
        when(praticaService.trovaTutte(any(Pageable.class))).thenReturn(pagina);

        ResponseEntity<Page<PraticaResponse>> resp = controller.trovaTutte(0, 20, new String[]{"id", "desc"});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NotificaController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("NotificaController.segnaComeLetta → 200 OK")
    void notifica_segnaComeLetta_200() {
        NotificaController controller = new NotificaController(notificaService);

        ResponseEntity<Void> resp = controller.segnaComeLetta(5L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificaService).segnaComeLetta(5L, utenteAutenticato);
    }

    @Test
    @DisplayName("NotificaController.segnaComeLetteTutte → 200 OK")
    void notifica_segnaComeLetteTutte_200() {
        NotificaController controller = new NotificaController(notificaService);

        ResponseEntity<Void> resp = controller.segnaComeLetteTutte(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificaService).segnaComeLetteTutte(1L);
    }

    @Test
    @DisplayName("NotificaController.mie → 200 OK con pagina notifiche")
    void notifica_mie_200() {
        NotificaController controller = new NotificaController(notificaService);
        Page<Notifica> pagina = new PageImpl<>(List.of());
        when(notificaService.trovaPerUtente(eq(utenteAutenticato), any(Pageable.class))).thenReturn(pagina);

        ResponseEntity<Page<NotificaResponse>> resp = controller.mie(utenteAutenticato, 0, 20);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("NotificaController.nonLette → 200 OK")
    void notifica_nonLette_200() {
        NotificaController controller = new NotificaController(notificaService);
        Page<Notifica> pagina = new PageImpl<>(List.of());
        when(notificaService.trovaNonLette(eq(utenteAutenticato), any(Pageable.class))).thenReturn(pagina);

        ResponseEntity<Page<NotificaResponse>> resp = controller.nonLette(utenteAutenticato, 0, 20);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ClienteController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ClienteController.miePratiche → 200 OK con lista pratiche")
    void cliente_miePratiche_200() {
        ClienteController controller = new ClienteController(clienteService);
        when(clienteService.trovaPratiche(1L)).thenReturn(List.of(pratica));

        ResponseEntity<?> resp = controller.miePratiche(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("ClienteController.mieDocumenti → 200 OK con lista documenti")
    void cliente_mieDocumenti_200() {
        ClienteController controller = new ClienteController(clienteService);
        when(clienteService.trovaDocumenti(1L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.mieDocumenti(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("ClienteController.mioProfilo → 200 OK con dati cliente")
    void cliente_mioProfilo_200() {
        ClienteController controller = new ClienteController(clienteService);
        Cliente full = new Cliente();
        full.setId(1L);
        full.setNome("Mario");
        full.setCognome("Rossi");
        full.setEmail("mario@test.it");
        full.setCodFiscale("RSSMRO80A01H501Z");
        full.setRegime(com.tesi.gestionalec.model.RegimeFiscale.ORDINARIO);
        full.setRedditoAnnuo(40000.0);
        when(clienteService.trovaClientePerId(1L)).thenReturn(full);

        ResponseEntity<?> resp = controller.mioProfilo(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(clienteService).trovaClientePerId(1L);
    }

    @Test
    @DisplayName("ClienteController.aggiornaProfilo → 200 OK e delega al service")
    void cliente_aggiornaProfilo_200() {
        ClienteController controller = new ClienteController(clienteService);
        com.tesi.gestionalec.dto.request.ClienteUpdateRequest req =
                new com.tesi.gestionalec.dto.request.ClienteUpdateRequest();
        req.setNome("Mario");
        req.setCognome("Rossi");
        req.setEmail("mario@test.it");
        req.setCodFiscale("RSSMRO80A01H501Z");
        req.setPartitaIva("12345678901");
        req.setRegime(com.tesi.gestionalec.model.RegimeFiscale.FORFETTARIO);
        req.setRedditoAnnuo(30000.0);

        Cliente aggiornato = new Cliente();
        aggiornato.setId(1L);
        aggiornato.setNome("Mario");
        aggiornato.setCognome("Rossi");
        aggiornato.setEmail("mario@test.it");
        when(clienteService.aggiorna(eq(1L), any(Cliente.class))).thenReturn(aggiornato);

        ResponseEntity<?> resp = controller.aggiornaProfilo(utenteAutenticato, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(clienteService).aggiorna(eq(1L), any(Cliente.class));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CommercialistaController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CommercialistaController.calcolaImposte → 200 OK con importo")
    void commercialista_calcolaImposte_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(3L);
        when(commercialistaService.calcolaImposteCliente(3L)).thenReturn(1500.0);
        // verificaAppartenenzaCliente non lancia eccezioni (cliente appartiene al comm)
        doNothing().when(commercialistaService).verificaAppartenenzaCliente(3L, 3L);

        ResponseEntity<Double> resp = controller.calcolaImposte(3L, comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(1500.0);
        verify(commercialistaService).verificaAppartenenzaCliente(3L, 3L);
    }

    @Test
    @DisplayName("CommercialistaController.trovaClienti → 200 OK con i soli clienti del commercialista")
    void commercialista_trovaClienti_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(99L);
        Cliente c1 = new Cliente();
        c1.setId(1L);
        c1.setNome("Anna");
        c1.setCognome("Verdi");
        c1.setRegime(com.tesi.gestionalec.model.RegimeFiscale.ORDINARIO);
        when(commercialistaService.trovaClientiDelCommercialista(99L)).thenReturn(List.of(c1));

        ResponseEntity<?> resp = controller.trovaClienti(comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commercialistaService).trovaClientiDelCommercialista(99L);
    }

    @Test
    @DisplayName("CommercialistaController.mieiCollaboratori → 200 OK con lista mappata")
    void commercialista_mieiCollaboratori_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(99L);
        when(commercialistaService.trovaMieiCollaboratori(99L)).thenReturn(List.of(collaboratore));

        ResponseEntity<?> resp = controller.mieiCollaboratori(comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commercialistaService).trovaMieiCollaboratori(99L);
    }

    @Test
    @DisplayName("CommercialistaController.documentiStudio → 200 OK con lista documenti")
    void commercialista_documentiStudio_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(99L);
        Documento doc = new Documento();
        doc.setId(1L);
        doc.setNome("doc.pdf");
        doc.setTipoFile("PDF");
        doc.setCaricatoDa(cliente);
        when(commercialistaService.trovaDocumentiStudio(99L)).thenReturn(List.of(doc));

        ResponseEntity<?> resp = controller.documentiStudio(comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commercialistaService).trovaDocumentiStudio(99L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CollaboratoreController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CollaboratoreController.miePratiche → 200 OK")
    void collaboratore_miePratiche_200() {
        CollaboratoreController controller = new CollaboratoreController(collaboratoreService);
        when(collaboratoreService.trovaPraticheAssegnate(5L)).thenReturn(List.of(pratica));

        ResponseEntity<?> resp = controller.miePratiche(collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("CollaboratoreController.mieRevisioni → 200 OK")
    void collaboratore_mieRevisioni_200() {
        CollaboratoreController controller = new CollaboratoreController(collaboratoreService);
        when(collaboratoreService.trovaDocumentiInRevisione(5L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.mieRevisioni(collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("CollaboratoreController.approva → 200 OK")
    void collaboratore_approva_200() {
        CollaboratoreController controller = new CollaboratoreController(collaboratoreService);

        ResponseEntity<Void> resp = controller.approva(30L, collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(collaboratoreService).approvaDocumento(30L, 5L);
    }

    @Test
    @DisplayName("CollaboratoreController.rifiuta → 200 OK")
    void collaboratore_rifiuta_200() {
        CollaboratoreController controller = new CollaboratoreController(collaboratoreService);

        ResponseEntity<Void> resp = controller.rifiuta(30L, "Documento illeggibile", collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(collaboratoreService).rifiutaDocumento(30L, "Documento illeggibile", 5L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DocumentoController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DocumentoController.trovaPerId → 200 OK con DTO documento")
    void documento_trovaPerId_200() {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        when(documentoService.trovaPerId(20L, utenteAutenticato)).thenReturn(documento);

        ResponseEntity<?> resp = controller.trovaPerId(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
    }

    @Test
    @DisplayName("DocumentoController.assegnaRevisore → 200 OK")
    void documento_assegnaRevisore_200() {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);

        ResponseEntity<Void> resp = controller.assegnaRevisore(20L, 5L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoService).assegnaRevisore(20L, 5L, utenteAutenticato);
    }

    @Test
    @DisplayName("DocumentoController.approva → 200 OK")
    void documento_approva_200() {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);

        ResponseEntity<Void> resp = controller.approva(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoService).approvaDocumento(20L, utenteAutenticato);
    }

    @Test
    @DisplayName("DocumentoController.rifiuta → 200 OK con motivazione")
    void documento_rifiuta_200() {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);

        ResponseEntity<Void> resp = controller.rifiuta(20L, "Dati mancanti", utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoService).rifiutaDocumento(20L, "Dati mancanti", utenteAutenticato);
    }

    @Test
    @DisplayName("DocumentoController.elimina → 204 No Content")
    void documento_elimina_204() {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);

        ResponseEntity<Void> resp = controller.elimina(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(documentoService).eliminaDocumento(20L, utenteAutenticato);
    }

    @Test
    @DisplayName("DocumentoController.download → 200 OK con resource")
    void documento_download_200() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        Resource resource = mock(Resource.class);
        when(documentoService.trovaPerId(20L, utenteAutenticato)).thenReturn(documento);
        when(fileStorageService.carica("uploads/test.pdf")).thenReturn(resource);

        ResponseEntity<?> resp = controller.download(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AmministratoreController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("AmministratoreController.trovaTuttiUtenti → 200 OK con pagina")
    void admin_trovaTuttiUtenti_200() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        Page<Utente> page = new PageImpl<>(List.of(utenteAutenticato));
        when(amministratoreService.trovaTutti(any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> resp = controller.trovaTuttiUtenti(0, 20, new String[]{"id", "asc"});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("AmministratoreController.trovaPerId → 200 OK")
    void admin_trovaPerId_200() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        when(amministratoreService.trovaPerId(1L)).thenReturn(utenteAutenticato);

        ResponseEntity<?> resp = controller.trovaPerId(1L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("AmministratoreController.abilita → 200 OK")
    void admin_abilita_200() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        Amministratore adminCorrente = adminConId(9L);

        ResponseEntity<Void> resp = controller.abilita(1L, adminCorrente);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(amministratoreService).abilitaUtente(1L, adminCorrente);
    }

    @Test
    @DisplayName("AmministratoreController.disabilita → 200 OK")
    void admin_disabilita_200() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        Amministratore adminCorrente = adminConId(9L);

        ResponseEntity<Void> resp = controller.disabilita(1L, adminCorrente);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(amministratoreService).disabilitaUtente(1L, adminCorrente);
    }

    @Test
    @DisplayName("AmministratoreController.elimina → 204 No Content")
    void admin_elimina_204() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        Amministratore adminCorrente = adminConId(9L);

        ResponseEntity<Void> resp = controller.elimina(1L, adminCorrente);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(amministratoreService).eliminaUtente(1L, adminCorrente);
    }

    @Test
    @DisplayName("AmministratoreController.ripristina → 200 OK")
    void admin_ripristina_200() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        Amministratore adminCorrente = adminConId(9L);

        ResponseEntity<Void> resp = controller.ripristina(1L, adminCorrente);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(amministratoreService).ripristinaUtente(1L, adminCorrente);
    }

    @Test
    @DisplayName("AmministratoreController.ultimaAzione → 200 OK con il timestamp dell'admin")
    void admin_ultimaAzione_200() {
        AmministratoreController controller = new AmministratoreController(amministratoreService);
        Amministratore adminCorrente = adminConId(9L);
        java.time.LocalDateTime istante = java.time.LocalDateTime.of(2026, 6, 10, 9, 30);
        adminCorrente.setUltimaAzioneAmministrativa(istante);

        ResponseEntity<UltimaAzioneResponse> resp = controller.ultimaAzione(adminCorrente);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getUltimaAzione()).isEqualTo(istante);
    }

    // Costruisce un amministratore autenticato con l'id indicato
    private Amministratore adminConId(Long id) {
        Amministratore admin = new Amministratore();
        admin.setId(id);
        admin.setNome("Admin");
        admin.setCognome("System");
        return admin;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ChatController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ChatController.storico → 200 OK")
    void chat_storico_200() {
        ChatController controller = new ChatController(messagingTemplate, chatService);
        when(chatService.storico(1L, 2L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.storico(utenteAutenticato, 2L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("ChatController.contatti → 200 OK")
    void chat_contatti_200() {
        ChatController controller = new ChatController(messagingTemplate, chatService);
        when(chatService.trovaContatti(utenteAutenticato)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.contatti(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("ChatController.nonLetti → 200 OK con la mappa dei conteggi")
    void chat_nonLetti_200() {
        ChatController controller = new ChatController(messagingTemplate, chatService);
        when(chatService.nonLettiPerMittente(1L)).thenReturn(Map.of(2L, 3L));

        ResponseEntity<Map<Long, Long>> resp = controller.nonLetti(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry(2L, 3L);
    }

    @Test
    @DisplayName("ChatController.segnaLetti → 200 OK e delega al service")
    void chat_segnaLetti_200() {
        ChatController controller = new ChatController(messagingTemplate, chatService);

        ResponseEntity<Void> resp = controller.segnaLetti(utenteAutenticato, 2L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(chatService).segnaLetti(1L, 2L);
    }

    @Test
    @DisplayName("ChatController.inviaMessaggio → salva e inoltra via WebSocket al destinatario")
    void chat_inviaMessaggio_inoltraWebSocket() {
        ChatController controller = new ChatController(messagingTemplate, chatService);
        MessaggioChatRequest req = new MessaggioChatRequest();
        req.setDestinatarioId(2L);
        req.setTesto("Ciao");
        MessaggioChatResponse out = new MessaggioChatResponse();
        when(chatService.salvaEInvia(req, utenteAutenticato)).thenReturn(out);

        controller.inviaMessaggio(req, new UtentePrincipal(utenteAutenticato));

        verify(chatService).salvaEInvia(req, utenteAutenticato);
        verify(messagingTemplate).convertAndSendToUser("2", "/queue/messaggi", out);
    }

    @Test
    @DisplayName("ChatController.inviaMessaggio: amministratore → AccessDeniedException, niente invio")
    void chat_inviaMessaggio_amministratore_accessDenied() {
        ChatController controller = new ChatController(messagingTemplate, chatService);
        Amministratore admin = new Amministratore();
        admin.setId(9L);
        MessaggioChatRequest req = new MessaggioChatRequest();
        req.setDestinatarioId(2L);

        assertThatThrownBy(() -> controller.inviaMessaggio(req, new UtentePrincipal(admin)))
                .isInstanceOf(AccessDeniedException.class);
        verify(chatService, never()).salvaEInvia(any(), any());
        verifyNoInteractions(messagingTemplate);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // InvitoCollaborazioneController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("InvitoController.invita → 201 Created")
    void invito_invita_201() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);
        com.tesi.gestionalec.dto.request.InvitoRequest request = new com.tesi.gestionalec.dto.request.InvitoRequest();
        request.setEmailDestinatario("luca@test.it");
        when(invitoService.invita(1L, "luca@test.it")).thenReturn(invito);

        ResponseEntity<?> resp = controller.invita(utenteAutenticato, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DisplayName("InvitoController.miei → 200 OK lista vuota")
    void invito_miei_200() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);
        when(invitoService.trovaPerCommercialista(1L)).thenReturn(List.of());

        ResponseEntity<?> resp = controller.miei(utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("InvitoController.revoca → 204 No Content")
    void invito_revoca_204() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);

        ResponseEntity<Void> resp = controller.revoca(utenteAutenticato, 50L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(invitoService).revoca(50L, 1L);
    }

    @Test
    @DisplayName("InvitoController.pending → 200 OK")
    void invito_pending_200() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);
        when(invitoService.trovaPendingPerEmail(any())).thenReturn(List.of());

        ResponseEntity<?> resp = controller.pending(collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("InvitoController.accettati → 200 OK")
    void invito_accettati_200() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);
        when(invitoService.trovaAccettatiPerEmail(any())).thenReturn(List.of());

        ResponseEntity<?> resp = controller.accettati(collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("InvitoController.accetta → 200 OK")
    void invito_accetta_200() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);

        ResponseEntity<Void> resp = controller.accetta("tok-abc", collaboratore);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invitoService).accetta("tok-abc", 5L);
    }

    @Test
    @DisplayName("InvitoController.rifiuta → 200 OK")
    void invito_rifiuta_200() {
        InvitoCollaborazioneController controller = new InvitoCollaborazioneController(invitoService);

        ResponseEntity<Void> resp = controller.rifiuta("tok-abc");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invitoService).rifiuta("tok-abc");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AuthController
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("AuthController.registra → 200 OK con JWT")
    void auth_registra_200() {
        AuthController controller = new AuthController(utenteService, gestoreTokenService, authManager, commercialistaRepo, gestoreNotifiche);
        com.tesi.gestionalec.dto.request.RegistrazioneRequest request = new com.tesi.gestionalec.dto.request.RegistrazioneRequest();
        request.setNome("Mario");
        request.setCognome("Rossi");
        request.setEmail("mario@test.it");
        request.setPassword("Password1!");
        request.setRuolo(Ruolo.CLIENTE);
        request.setCodFiscale("RSSMRO80A01H501Z");
        request.setRegime("ORDINARIO");

        Cliente salvato = new Cliente();
        salvato.setId(1L);
        salvato.setNome("Mario");
        salvato.setCognome("Rossi");
        salvato.setEmail("mario@test.it");

        when(utenteService.registra(any(), any())).thenReturn(salvato);
        when(gestoreTokenService.generaToken("mario@test.it")).thenReturn("jwt-123");

        ResponseEntity<?> resp = controller.registra(request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gestoreTokenService).generaToken("mario@test.it");
        // La notifica di benvenuto (ex RegistrazioneFacade) viene ora inviata qui
        verify(gestoreNotifiche).notificaTutti(any(com.tesi.gestionalec.model.Notifica.class));
    }

    @Test
    @DisplayName("AuthController.login → 200 OK con JWT")
    void auth_login_200() {
        AuthController controller = new AuthController(utenteService, gestoreTokenService, authManager, commercialistaRepo, gestoreNotifiche);
        com.tesi.gestionalec.dto.request.LoginRequest request = new com.tesi.gestionalec.dto.request.LoginRequest();
        request.setEmail("mario@test.it");
        request.setPassword("Password1!");

        utenteAutenticato.setEmail("mario@test.it");
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(utenteAutenticato);
        when(gestoreTokenService.generaToken("mario@test.it")).thenReturn("jwt-456");

        ResponseEntity<?> resp = controller.login(request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gestoreTokenService).generaToken("mario@test.it");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DocumentoController — metodi non ancora coperti
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DocumentoController.carica → 200 OK con DTO documento")
    void documento_carica_200() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "test.pdf", "application/pdf", "contenuto".getBytes());

        when(fileStorageService.salva(file)).thenReturn("uploads/test.pdf");
        when(documentoFacade.caricaEAssegna(any(), isNull(), eq(utenteAutenticato)))
                .thenReturn(DocumentoMapper.toResponse(documento));

        ResponseEntity<?> resp = controller.carica(file, "test.pdf", "PDF", "10", utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoFacade).caricaEAssegna(any(), isNull(), eq(utenteAutenticato));
    }

    @Test
    @DisplayName("DocumentoController.carica → nome vuoto usa nome file originale")
    void documento_carica_nomeVuoto_usaNomeFile() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "originale.pdf", "application/pdf", "x".getBytes());

        when(fileStorageService.salva(file)).thenReturn("uploads/originale.pdf");
        when(documentoFacade.caricaEAssegna(any(), isNull(), eq(utenteAutenticato)))
                .thenReturn(DocumentoMapper.toResponse(documento));

        // nome = "" (blank) → deve usare file.getOriginalFilename()
        ResponseEntity<?> resp = controller.carica(file, "", "PDF", "10", utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<DocumentoRequest> cap = ArgumentCaptor.forClass(DocumentoRequest.class);
        verify(documentoFacade).caricaEAssegna(cap.capture(), isNull(), eq(utenteAutenticato));
        assertThat(cap.getValue().getNome()).isEqualTo("originale.pdf");
    }

    @Test
    @DisplayName("DocumentoController.nuovaVersione → 200 OK")
    void documento_nuovaVersione_200() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "v2.pdf", "application/pdf", "v2".getBytes());

        when(fileStorageService.salva(file)).thenReturn("uploads/v2.pdf");
        when(documentoService.nuovaVersione(eq(20L), any(), eq(utenteAutenticato))).thenReturn(documento);

        ResponseEntity<?> resp = controller.nuovaVersione(20L, file, "v2.pdf", "PDF", utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoService).nuovaVersione(eq(20L), any(), eq(utenteAutenticato));
    }

    @Test
    @DisplayName("DocumentoController.nuovaVersione → nome null usa nome file originale")
    void documento_nuovaVersione_nomeNull_usaNomeFile() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "originale.pdf", "application/pdf", "x".getBytes());

        when(fileStorageService.salva(file)).thenReturn("uploads/originale.pdf");
        when(documentoService.nuovaVersione(eq(20L), any(), eq(utenteAutenticato))).thenReturn(documento);

        ResponseEntity<?> resp = controller.nuovaVersione(20L, file, null, null, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("DocumentoController.download PDF → Content-Type application/pdf")
    void documento_download_pdf_contentType() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        documento.setNome("referto.pdf");
        Resource resource = mock(Resource.class);
        when(documentoService.trovaPerId(20L, utenteAutenticato)).thenReturn(documento);
        when(fileStorageService.carica("uploads/test.pdf")).thenReturn(resource);

        ResponseEntity<?> resp = controller.download(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType().toString()).contains("pdf");
    }

    @Test
    @DisplayName("DocumentoController.download DOCX → Content-Type application/msword")
    void documento_download_docx_contentType() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        documento.setNome("contratto.docx");
        Resource resource = mock(Resource.class);
        when(documentoService.trovaPerId(20L, utenteAutenticato)).thenReturn(documento);
        when(fileStorageService.carica("uploads/test.pdf")).thenReturn(resource);

        ResponseEntity<?> resp = controller.download(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType().toString()).contains("msword");
    }

    @Test
    @DisplayName("DocumentoController.download XLSX → Content-Type vnd.ms-excel")
    void documento_download_xlsx_contentType() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        documento.setNome("bilancio.xlsx");
        Resource resource = mock(Resource.class);
        when(documentoService.trovaPerId(20L, utenteAutenticato)).thenReturn(documento);
        when(fileStorageService.carica("uploads/test.pdf")).thenReturn(resource);

        ResponseEntity<?> resp = controller.download(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType().toString()).contains("excel");
    }

    @Test
    @DisplayName("DocumentoController.download tipo sconosciuto → application/octet-stream")
    void documento_download_sconosciuto_octetStream() throws Exception {
        DocumentoController controller = new DocumentoController(documentoService, fileStorageService, documentoFacade);
        documento.setNome("archivio.zip");
        Resource resource = mock(Resource.class);
        when(documentoService.trovaPerId(20L, utenteAutenticato)).thenReturn(documento);
        when(fileStorageService.carica("uploads/test.pdf")).thenReturn(resource);

        ResponseEntity<?> resp = controller.download(20L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType().toString()).contains("octet-stream");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PraticaController — metodi non ancora coperti
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PraticaController.documentiPratica → 200 OK con lista documenti")
    void pratica_documentiPratica_200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);
        when(praticaService.trovaPerId(10L, utenteAutenticato)).thenReturn(pratica);
        when(documentoService.trovaPerPratica(pratica)).thenReturn(List.of(documento));

        ResponseEntity<?> resp = controller.documentiPratica(10L, utenteAutenticato);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentoService).trovaPerPratica(pratica);
    }

    @Test
    @DisplayName("PraticaController.trovaTutte → 200 OK con sort multi-campo")
    void pratica_trovaTutte_multiSort_200() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);
        Page<Pratica> pagina = new PageImpl<>(List.of(pratica));
        when(praticaService.trovaTutte(any(Pageable.class))).thenReturn(pagina);

        // sort con 4 token = 2 campi (multi-sort)
        ResponseEntity<Page<PraticaResponse>> resp =
                controller.trovaTutte(0, 20, new String[]{"id", "desc", "scadenza", "asc"});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PraticaController.trovaTutte → direzione invalida usa ASC come fallback")
    void pratica_trovaTutte_direzioneInvalida_fallbackAsc() {
        PraticaController controller = new PraticaController(praticaService, documentoService, praticaFacade);
        Page<Pratica> pagina = new PageImpl<>(List.of());
        when(praticaService.trovaTutte(any(Pageable.class))).thenReturn(pagina);

        // direzione non valida → deve usare ASC senza eccezione
        ResponseEntity<Page<PraticaResponse>> resp =
                controller.trovaTutte(0, 20, new String[]{"id", "INVALIDA"});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CommercialistaController — metodi non ancora coperti
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CommercialistaController.trovaCliente → 200 OK con DTO cliente")
    void commercialista_trovaCliente_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(99L);
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Mario");
        c.setCognome("Rossi");
        c.setRegime(com.tesi.gestionalec.model.RegimeFiscale.ORDINARIO);
        doNothing().when(commercialistaService).verificaAppartenenzaCliente(1L, 99L);
        when(clienteService.trovaClientePerId(1L)).thenReturn(c);

        ResponseEntity<?> resp = controller.trovaCliente(1L, comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commercialistaService).verificaAppartenenzaCliente(1L, 99L);
        verify(clienteService).trovaClientePerId(1L);
    }

    @Test
    @DisplayName("CommercialistaController.praticheCliente → 200 OK con lista pratiche")
    void commercialista_praticheCliente_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(99L);
        doNothing().when(commercialistaService).verificaAppartenenzaCliente(1L, 99L);
        when(clienteService.trovaPratiche(1L)).thenReturn(List.of(pratica));

        ResponseEntity<?> resp = controller.praticheCliente(1L, comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commercialistaService).verificaAppartenenzaCliente(1L, 99L);
        verify(clienteService).trovaPratiche(1L);
    }

    @Test
    @DisplayName("CommercialistaController.documentiCliente → 200 OK con lista documenti")
    void commercialista_documentiCliente_200() {
        CommercialistaController controller = new CommercialistaController(commercialistaService, clienteService);
        Commercialista comm = new Commercialista();
        comm.setId(99L);
        doNothing().when(commercialistaService).verificaAppartenenzaCliente(1L, 99L);
        when(clienteService.trovaDocumenti(1L)).thenReturn(List.of(documento));

        ResponseEntity<?> resp = controller.documentiCliente(1L, comm);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(commercialistaService).verificaAppartenenzaCliente(1L, 99L);
        verify(clienteService).trovaDocumenti(1L);
    }
}
