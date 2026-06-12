package com.tesi.gestionalec.service;

import com.tesi.gestionalec.dto.request.MessaggioChatRequest;
import com.tesi.gestionalec.dto.response.MessaggioChatResponse;
import com.tesi.gestionalec.dto.response.UtenteResponse;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.ClienteRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.repository.MessaggioChatRepo;
import com.tesi.gestionalec.service.impl.ChatServiceImpl;
import com.tesi.gestionalec.service.interfaces.UtenteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari per ChatServiceImpl.
 * Copre: invio messaggi tra ruoli validi, blocco dell'Amministratore,
 * blocco di combinazioni di ruoli non permesse, e recupero storico.
 *
 * Nota: getRuolo() è hardcoded nelle sottoclassi concrete di Utente
 * (es. new Cliente() → Ruolo.CLIENTE), quindi si usano direttamente quelle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService – Unit Tests")
class ChatServiceImplTest {

    @Mock MessaggioChatRepo repo;
    @Mock UtenteService utenteService;
    @Mock ClienteRepo clienteRepo;
    @Mock InvitoCollaborazioneRepo invitoRepo;

    @InjectMocks
    ChatServiceImpl chatService;

    private MessaggioChatRequest request;

    @BeforeEach
    void setUp() {
        request = new MessaggioChatRequest();
        request.setDestinatarioId(2L);
        request.setTesto("Ciao!");
    }

    // ─── Helper per costruire MessaggioChat salvato ───────────────────────────
    private void mockSave() {
        when(repo.save(any())).thenAnswer(inv -> {
            MessaggioChat m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
    }

    private Cliente cliente(long id) {
        Cliente c = new Cliente(); c.setId(id); c.setNome("M"); c.setCognome("R"); return c;
    }
    private Commercialista commercialista(long id) {
        Commercialista c = new Commercialista(); c.setId(id); c.setNome("G"); c.setCognome("B"); return c;
    }
    private Collaboratore collaboratore(long id) {
        Collaboratore c = new Collaboratore(); c.setId(id); c.setNome("L"); c.setCognome("V"); return c;
    }
    private Amministratore amministratore(long id) {
        Amministratore a = new Amministratore(); a.setId(id); a.setNome("A"); a.setCognome("A"); return a;
    }

    // ─── Combinazioni VALIDE ──────────────────────────────────────────────────

    @Test
    @DisplayName("Cliente → Commercialista (collegato): messaggio salvato con successo")
    void salvaEInvia_clienteACommercialista_ok() {
        Commercialista comm = commercialista(2L);
        Cliente clienteConComm = cliente(1L);
        clienteConComm.setCommercialista(comm);

        when(utenteService.trovaPerId(2L)).thenReturn(comm);
        when(clienteRepo.findById(1L)).thenReturn(java.util.Optional.of(clienteConComm));
        when(invitoRepo.findCollaboratoriAttiviByCommercialista(2L)).thenReturn(List.of());
        mockSave();

        MessaggioChatResponse resp = chatService.salvaEInvia(request, cliente(1L));

        assertThat(resp).isNotNull();
        verify(repo).save(any(MessaggioChat.class));
    }

    @Test
    @DisplayName("Cliente → Collaboratore (del suo commercialista): messaggio salvato con successo")
    void salvaEInvia_clienteACollaboratore_ok() {
        Collaboratore collab = collaboratore(2L);
        Commercialista comm = commercialista(5L);
        Cliente clienteConComm = cliente(1L);
        clienteConComm.setCommercialista(comm);

        when(utenteService.trovaPerId(2L)).thenReturn(collab);
        when(clienteRepo.findById(1L)).thenReturn(java.util.Optional.of(clienteConComm));
        when(invitoRepo.findCollaboratoriAttiviByCommercialista(5L))
                .thenReturn(List.of(invitoAccettato(comm, collab)));
        mockSave();

        assertThatCode(() -> chatService.salvaEInvia(request, cliente(1L))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Commercialista → suo Cliente: messaggio salvato con successo")
    void salvaEInvia_commercialistaACliente_ok() {
        Cliente cli = cliente(2L);

        when(utenteService.trovaPerId(2L)).thenReturn(cli);
        when(clienteRepo.findByCommercialistaId(1L)).thenReturn(List.of(cli));
        when(invitoRepo.findCollaboratoriAttiviByCommercialista(1L)).thenReturn(List.of());
        mockSave();

        assertThatCode(() -> chatService.salvaEInvia(request, commercialista(1L))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Collaboratore → Commercialista (con cui collabora): messaggio salvato con successo")
    void salvaEInvia_collaboratoreACommercialista_ok() {
        Commercialista comm = commercialista(2L);
        Collaboratore mittente = collaboratore(1L);

        when(utenteService.trovaPerId(2L)).thenReturn(comm);
        when(invitoRepo.findCommercialistiAttiviByCollaboratore(1L))
                .thenReturn(List.of(invitoAccettato(comm, mittente)));
        when(clienteRepo.findByCommercialistaId(2L)).thenReturn(List.of());
        mockSave();

        assertThatCode(() -> chatService.salvaEInvia(request, mittente)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cliente → Commercialista NON collegato → AccessDeniedException")
    void salvaEInvia_destinatarioNonCollegato_lanciaAccessDenied() {
        // Ruolo compatibile ma nessun legame: il cliente non ha commercialista
        Cliente clienteSenzaComm = cliente(1L);
        clienteSenzaComm.setCommercialista(null);

        when(utenteService.trovaPerId(2L)).thenReturn(commercialista(2L));
        when(clienteRepo.findById(1L)).thenReturn(java.util.Optional.of(clienteSenzaComm));

        assertThatThrownBy(() -> chatService.salvaEInvia(request, cliente(1L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    // ─── Combinazioni NON VALIDE ──────────────────────────────────────────────

    @Test
    @DisplayName("Amministratore come mittente → AccessDeniedException, repo non chiamato")
    void salvaEInvia_amministratoreComeMittente_lanciaAccessDenied() {
        when(utenteService.trovaPerId(2L)).thenReturn(cliente(2L));

        assertThatThrownBy(() -> chatService.salvaEInvia(request, amministratore(1L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Cliente → Cliente: combinazione non valida → AccessDeniedException")
    void salvaEInvia_clienteACliente_lanciaAccessDenied() {
        when(utenteService.trovaPerId(2L)).thenReturn(cliente(2L));

        assertThatThrownBy(() -> chatService.salvaEInvia(request, cliente(1L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Cliente → Amministratore: combinazione non valida → AccessDeniedException")
    void salvaEInvia_clienteAAmministratore_lanciaAccessDenied() {
        when(utenteService.trovaPerId(2L)).thenReturn(amministratore(2L));

        assertThatThrownBy(() -> chatService.salvaEInvia(request, cliente(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Commercialista → Commercialista: combinazione non valida → AccessDeniedException")
    void salvaEInvia_commercialistaACommercialista_lanciaAccessDenied() {
        when(utenteService.trovaPerId(2L)).thenReturn(commercialista(2L));

        assertThatThrownBy(() -> chatService.salvaEInvia(request, commercialista(1L)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    // ─── storico ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("storico: delega al repo e mappa correttamente i messaggi in DTO")
    void storico_delegaAlRepoEMappa() {
        Cliente mitt = cliente(1L);
        Commercialista dest = commercialista(2L);

        MessaggioChat msg = new MessaggioChat();
        msg.setId(10L);
        msg.setMittente(mitt);
        msg.setDestinatario(dest);
        msg.setTesto("Ciao");
        msg.setLetto(true);

        when(repo.trovaStotico(1L, 2L)).thenReturn(List.of(msg));

        List<MessaggioChatResponse> storico = chatService.storico(1L, 2L);

        assertThat(storico).hasSize(1);
        assertThat(storico.get(0).getTesto()).isEqualTo("Ciao");
        verify(repo).trovaStotico(1L, 2L);
    }

    @Test
    @DisplayName("storico: nessun messaggio → lista vuota")
    void storico_vuoto_listaVuota() {
        when(repo.trovaStotico(1L, 2L)).thenReturn(List.of());

        assertThat(chatService.storico(1L, 2L)).isEmpty();
    }

    // ─── trovaContatti ────────────────────────────────────────────────────────

    // Helper: invito ACCEPTED che collega un commercialista a un collaboratore
    private InvitoCollaborazione invitoAccettato(Commercialista comm, Collaboratore collab) {
        InvitoCollaborazione i = new InvitoCollaborazione();
        i.setCommercialista(comm);
        i.setCollaboratore(collab);
        i.setStato(StatoInvito.ACCEPTED);
        return i;
    }
    private Cliente clienteAbilitato(long id) {
        Cliente c = cliente(id); c.setEmail("cli" + id + "@test.it"); c.setEnabled(true); return c;
    }
    private Commercialista commAbilitato(long id) {
        Commercialista c = commercialista(id); c.setEmail("comm" + id + "@test.it"); c.setEnabled(true); return c;
    }
    private Collaboratore collabAbilitato(long id) {
        Collaboratore c = collaboratore(id); c.setEmail("collab" + id + "@test.it"); c.setEnabled(true); return c;
    }

    @Test
    @DisplayName("trovaContatti: COMMERCIALISTA → i propri clienti + i collaboratori con invito ACCEPTED")
    void trovaContatti_commercialista_clientiECollaboratori() {
        Commercialista mittente = commercialista(1L);
        Cliente cli = clienteAbilitato(2L);
        Collaboratore collab = collabAbilitato(3L);

        when(clienteRepo.findByCommercialistaId(1L)).thenReturn(List.of(cli));
        when(invitoRepo.findCollaboratoriAttiviByCommercialista(1L))
                .thenReturn(List.of(invitoAccettato(mittente, collab)));

        List<UtenteResponse> contatti = chatService.trovaContatti(mittente);

        assertThat(contatti).extracting(UtenteResponse::getId).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("trovaContatti: CLIENTE → il proprio commercialista + i collaboratori di quest'ultimo")
    void trovaContatti_cliente_commercialistaECollaboratori() {
        Cliente mittente = cliente(1L);
        Commercialista comm = commAbilitato(2L);
        Collaboratore collab = collabAbilitato(3L);

        Cliente clienteCaricato = clienteAbilitato(1L);
        clienteCaricato.setCommercialista(comm);

        when(clienteRepo.findById(1L)).thenReturn(java.util.Optional.of(clienteCaricato));
        when(invitoRepo.findCollaboratoriAttiviByCommercialista(2L))
                .thenReturn(List.of(invitoAccettato(comm, collab)));

        List<UtenteResponse> contatti = chatService.trovaContatti(mittente);

        assertThat(contatti).extracting(UtenteResponse::getId).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("trovaContatti: CLIENTE senza commercialista → lista vuota")
    void trovaContatti_clienteSenzaCommercialista_listaVuota() {
        Cliente mittente = cliente(1L);
        Cliente clienteCaricato = clienteAbilitato(1L);
        clienteCaricato.setCommercialista(null);

        when(clienteRepo.findById(1L)).thenReturn(java.util.Optional.of(clienteCaricato));

        assertThat(chatService.trovaContatti(mittente)).isEmpty();
    }

    @Test
    @DisplayName("trovaContatti: COLLABORATORE → i commercialisti con cui collabora + i loro clienti")
    void trovaContatti_collaboratore_commercialistiEClienti() {
        Collaboratore mittente = collaboratore(1L);
        Commercialista comm = commAbilitato(2L);
        Cliente cli = clienteAbilitato(3L);

        when(invitoRepo.findCommercialistiAttiviByCollaboratore(1L))
                .thenReturn(List.of(invitoAccettato(comm, mittente)));
        when(clienteRepo.findByCommercialistaId(2L)).thenReturn(List.of(cli));

        List<UtenteResponse> contatti = chatService.trovaContatti(mittente);

        assertThat(contatti).extracting(UtenteResponse::getId).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("trovaContatti: utenti disabilitati vengono esclusi")
    void trovaContatti_utenteDisabilitato_escluso() {
        Commercialista mittente = commercialista(1L);
        Cliente cliDisabilitato = cliente(2L);
        cliDisabilitato.setEnabled(false); // disabilitato → escluso

        when(clienteRepo.findByCommercialistaId(1L)).thenReturn(List.of(cliDisabilitato));
        when(invitoRepo.findCollaboratoriAttiviByCommercialista(1L)).thenReturn(List.of());

        assertThat(chatService.trovaContatti(mittente)).isEmpty();
    }

    @Test
    @DisplayName("trovaContatti: AMMINISTRATORE → lista vuota senza query ai repo")
    void trovaContatti_amministratore_listaVuota() {
        Amministratore admin = amministratore(10L);

        List<UtenteResponse> contatti = chatService.trovaContatti(admin);

        assertThat(contatti).isEmpty();
        verify(clienteRepo, never()).findByCommercialistaId(any());
        verify(invitoRepo, never()).findCommercialistiAttiviByCollaboratore(any());
    }
}
