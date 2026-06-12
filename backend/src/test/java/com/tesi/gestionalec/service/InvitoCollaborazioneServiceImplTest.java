package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.DuplicateInviteException;
import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.service.impl.EmailService;
import com.tesi.gestionalec.service.impl.InvitoCollaborazioneServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per InvitoCollaborazioneServiceImpl.
 * Copre: invio invito, accettazione, rifiuto, revoca, query, scadenza automatica.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvitoCollaborazioneService – Unit Tests")
class InvitoCollaborazioneServiceImplTest {

    @Mock InvitoCollaborazioneRepo invitoRepo;
    @Mock CommercialistaRepo commercialistaRepo;
    @Mock CollaboratoreRepo collaboratoreRepo;
    @Mock GestoreNotifiche gestoreNotifiche;
    @Mock EmailService emailService;

    @InjectMocks
    InvitoCollaborazioneServiceImpl invitoService;

    private Commercialista commercialista;
    private Collaboratore collaboratore;
    private InvitoCollaborazione invitoPending;

    @BeforeEach
    void setUp() {
        commercialista = new Commercialista();
        commercialista.setId(1L);
        commercialista.setNome("Giovanni");
        commercialista.setCognome("Verdi");
        commercialista.setNumeroAlbo("ALB001");

        collaboratore = new Collaboratore();
        collaboratore.setId(2L);
        collaboratore.setNome("Luca");
        collaboratore.setCognome("Bianchi");
        collaboratore.setEmail("luca@studio.it");
        collaboratore.setEnabled(true);

        invitoPending = new InvitoCollaborazione();
        invitoPending.setId(10L);
        invitoPending.setCommercialista(commercialista);
        invitoPending.setEmailDestinatario("luca@studio.it");
        invitoPending.setToken("tok-abc-123");
        invitoPending.setStato(StatoInvito.PENDING);
        invitoPending.setScadeIl(LocalDateTime.now().plusDays(6));
    }

    // ─── invita ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("invita: crea invito PENDING e lo salva nel repo")
    void invita_creaInvitoPendingESalva() {
        when(commercialistaRepo.findById(1L)).thenReturn(Optional.of(commercialista));
        when(invitoRepo.existsByCommercialista_IdAndEmailDestinatarioAndStato(
                1L, "mario@test.it", StatoInvito.PENDING)).thenReturn(false);
        when(collaboratoreRepo.findByEmail("mario@test.it")).thenReturn(Optional.empty());
        when(invitoRepo.save(any())).thenAnswer(inv -> {
            InvitoCollaborazione i = inv.getArgument(0);
            i.setId(99L);
            return i;
        });

        InvitoCollaborazione risultato = invitoService.invita(1L, "mario@test.it");

        assertThat(risultato.getStato()).isEqualTo(StatoInvito.PENDING);
        assertThat(risultato.getEmailDestinatario()).isEqualTo("mario@test.it");
        assertThat(risultato.getToken()).isNotBlank();
        verify(invitoRepo).save(any(InvitoCollaborazione.class));
    }

    @Test
    @DisplayName("invita: se collaboratore già registrato, lo collega subito all'invito")
    void invita_collaboratoreGiaRegistrato_collegaSubito() {
        when(commercialistaRepo.findById(1L)).thenReturn(Optional.of(commercialista));
        when(invitoRepo.existsByCommercialista_IdAndEmailDestinatarioAndStato(
                anyLong(), anyString(), any())).thenReturn(false);
        when(collaboratoreRepo.findByEmail("luca@studio.it")).thenReturn(Optional.of(collaboratore));
        when(invitoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvitoCollaborazione risultato = invitoService.invita(1L, "luca@studio.it");

        assertThat(risultato.getCollaboratore()).isEqualTo(collaboratore);
    }

    @Test
    @DisplayName("invita: invito duplicato (PENDING già esistente) → DuplicateInviteException")
    void invita_duplicato_lanciaDuplicateInviteException() {
        when(commercialistaRepo.findById(1L)).thenReturn(Optional.of(commercialista));
        when(invitoRepo.existsByCommercialista_IdAndEmailDestinatarioAndStato(
                1L, "luca@studio.it", StatoInvito.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> invitoService.invita(1L, "luca@studio.it"))
                .isInstanceOf(DuplicateInviteException.class);
        verify(invitoRepo, never()).save(any());
    }

    @Test
    @DisplayName("invita: commercialista non trovato → ResourceNotFoundException")
    void invita_commercialistaNonTrovato_lanciaEccezione() {
        when(commercialistaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitoService.invita(99L, "test@test.it"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("invita: invia email asincrona al destinatario")
    void invita_inviaEmail() {
        when(commercialistaRepo.findById(1L)).thenReturn(Optional.of(commercialista));
        when(invitoRepo.existsByCommercialista_IdAndEmailDestinatarioAndStato(
                anyLong(), anyString(), any())).thenReturn(false);
        when(collaboratoreRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(invitoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        invitoService.invita(1L, "nuovo@test.it");

        verify(emailService).inviaEmail(eq("nuovo@test.it"), anyString(), anyString());
    }

    // ─── accetta ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("accetta: imposta stato ACCEPTED e collega il collaboratore")
    void accetta_impostaAcceptedECollegaCollaboratore() {
        when(invitoRepo.findByToken("tok-abc-123")).thenReturn(Optional.of(invitoPending));
        when(collaboratoreRepo.findById(2L)).thenReturn(Optional.of(collaboratore));

        invitoService.accetta("tok-abc-123", 2L);

        assertThat(invitoPending.getStato()).isEqualTo(StatoInvito.ACCEPTED);
        assertThat(invitoPending.getCollaboratore()).isEqualTo(collaboratore);
    }

    @Test
    @DisplayName("accetta: notifica in-app al commercialista")
    void accetta_notificaAlCommercialista() {
        when(invitoRepo.findByToken("tok-abc-123")).thenReturn(Optional.of(invitoPending));
        when(collaboratoreRepo.findById(2L)).thenReturn(Optional.of(collaboratore));

        invitoService.accetta("tok-abc-123", 2L);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(commercialista);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.INVITO_COLLABORAZIONE);
    }

    @Test
    @DisplayName("accetta: email non corrispondente → ForbiddenOperationException")
    void accetta_emailNonCorrispondente_lanciaEccezione() {
        collaboratore.setEmail("altro@test.it"); // email diversa dal destinatario
        when(invitoRepo.findByToken("tok-abc-123")).thenReturn(Optional.of(invitoPending));
        when(collaboratoreRepo.findById(2L)).thenReturn(Optional.of(collaboratore));

        assertThatThrownBy(() -> invitoService.accetta("tok-abc-123", 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("accetta: token non trovato → ResourceNotFoundException")
    void accetta_tokenNonTrovato_lanciaEccezione() {
        when(invitoRepo.findByToken("tok-inesistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitoService.accetta("tok-inesistente", 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("accetta: invito non PENDING (già ACCEPTED) → InvalidStateException")
    void accetta_invitoNonPending_lanciaInvalidState() {
        invitoPending.setStato(StatoInvito.ACCEPTED);
        when(invitoRepo.findByToken("tok-abc-123")).thenReturn(Optional.of(invitoPending));

        assertThatThrownBy(() -> invitoService.accetta("tok-abc-123", 2L))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("accetta: invito scaduto → stato EXPIRED e InvalidStateException")
    void accetta_invitoScaduto_lanciaInvalidState() {
        invitoPending.setScadeIl(LocalDateTime.now().minusDays(1)); // già scaduto
        when(invitoRepo.findByToken("tok-abc-123")).thenReturn(Optional.of(invitoPending));

        assertThatThrownBy(() -> invitoService.accetta("tok-abc-123", 2L))
                .isInstanceOf(InvalidStateException.class);
        assertThat(invitoPending.getStato()).isEqualTo(StatoInvito.EXPIRED);
    }

    // ─── rifiuta ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rifiuta: imposta stato DECLINED e notifica il commercialista")
    void rifiuta_impostaDECLINEDENotifica() {
        when(invitoRepo.findByToken("tok-abc-123")).thenReturn(Optional.of(invitoPending));

        invitoService.rifiuta("tok-abc-123");

        assertThat(invitoPending.getStato()).isEqualTo(StatoInvito.DECLINED);
        verify(gestoreNotifiche).notificaTutti(any(Notifica.class));
    }

    // ─── revoca ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("revoca: PENDING → DECLINED")
    void revoca_pendingToDeclined() {
        when(invitoRepo.findById(10L)).thenReturn(Optional.of(invitoPending));

        invitoService.revoca(10L, 1L);

        assertThat(invitoPending.getStato()).isEqualTo(StatoInvito.DECLINED);
    }

    @Test
    @DisplayName("revoca: commercialista non proprietario → ForbiddenOperationException")
    void revoca_nonProprietario_lanciaEccezione() {
        when(invitoRepo.findById(10L)).thenReturn(Optional.of(invitoPending));

        assertThatThrownBy(() -> invitoService.revoca(10L, 99L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("revoca: invito già DECLINED → InvalidStateException")
    void revoca_invitoDeclined_lanciaEccezione() {
        invitoPending.setStato(StatoInvito.DECLINED);
        when(invitoRepo.findById(10L)).thenReturn(Optional.of(invitoPending));

        assertThatThrownBy(() -> invitoService.revoca(10L, 1L))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("revoca: invito già EXPIRED → InvalidStateException")
    void revoca_invitoExpired_lanciaEccezione() {
        invitoPending.setStato(StatoInvito.EXPIRED);
        when(invitoRepo.findById(10L)).thenReturn(Optional.of(invitoPending));

        assertThatThrownBy(() -> invitoService.revoca(10L, 1L))
                .isInstanceOf(InvalidStateException.class);
    }

    // ─── query ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerCommercialista: delega al repo")
    void trovaPerCommercialista_delegaAlRepo() {
        when(invitoRepo.findByCommercialista_Id(1L)).thenReturn(List.of(invitoPending));

        List<InvitoCollaborazione> lista = invitoService.trovaPerCommercialista(1L);

        assertThat(lista).hasSize(1);
        verify(invitoRepo).findByCommercialista_Id(1L);
    }

    @Test
    @DisplayName("trovaPendingPerEmail: delega al repo con stato PENDING")
    void trovaPendingPerEmail_delegaConStatoPending() {
        when(invitoRepo.findByEmailDestinatarioAndStato("luca@studio.it", StatoInvito.PENDING))
                .thenReturn(List.of(invitoPending));

        List<InvitoCollaborazione> lista = invitoService.trovaPendingPerEmail("luca@studio.it");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getStato()).isEqualTo(StatoInvito.PENDING);
    }

    @Test
    @DisplayName("trovaAccettatiPerEmail: delega al repo con stato ACCEPTED")
    void trovaAccettatiPerEmail_delegaConStatoAccepted() {
        InvitoCollaborazione accettato = new InvitoCollaborazione();
        accettato.setStato(StatoInvito.ACCEPTED);
        when(invitoRepo.findByEmailDestinatarioAndStato("luca@studio.it", StatoInvito.ACCEPTED))
                .thenReturn(List.of(accettato));

        List<InvitoCollaborazione> lista = invitoService.trovaAccettatiPerEmail("luca@studio.it");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getStato()).isEqualTo(StatoInvito.ACCEPTED);
    }

    // ─── scadenzaAutomatica ───────────────────────────────────────────────────

    @Test
    @DisplayName("scadenzaAutomatica: marca come EXPIRED gli inviti PENDING scaduti")
    void scadenzaAutomatica_marcaExpired() {
        InvitoCollaborazione scaduto = new InvitoCollaborazione();
        scaduto.setStato(StatoInvito.PENDING);
        scaduto.setScadeIl(LocalDateTime.now().minusDays(1));

        when(invitoRepo.findByStatoAndScadeIlBefore(eq(StatoInvito.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(scaduto));

        invitoService.scadenzaAutomatica();

        assertThat(scaduto.getStato()).isEqualTo(StatoInvito.EXPIRED);
        verify(invitoRepo).saveAll(anyList());
    }

    @Test
    @DisplayName("scadenzaAutomatica: nessun invito scaduto → nessun saveAll")
    void scadenzaAutomatica_nessunScaduto_nessunSave() {
        when(invitoRepo.findByStatoAndScadeIlBefore(eq(StatoInvito.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        invitoService.scadenzaAutomatica();

        verify(invitoRepo, never()).saveAll(any());
    }
}
