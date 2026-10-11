package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Amministratore;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.StatoRegistrazione;
import com.tesi.gestionalec.model.TipoNotifica;
import org.mockito.ArgumentCaptor;
import com.tesi.gestionalec.repository.AmministratoreRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.DocumentoRepo;
import com.tesi.gestionalec.repository.PraticaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.impl.AmministratoreServiceImpl;
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
 * Test unitari per AmministratoreServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AmministratoreService – Unit Tests")
class AmministratoreServiceImplTest {

    @Mock UtenteRepo utenteRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AmministratoreRepo amministratoreRepo;
    @Mock PraticaRepo praticaRepo;
    @Mock DocumentoRepo documentoRepo;
    @Mock CommercialistaRepo commercialistaRepo;
    @Mock com.tesi.gestionalec.observer.GestoreNotifiche gestoreNotifiche;

    AmministratoreServiceImpl amministratoreService;

    private Amministratore admin;

    @BeforeEach
    void setUp() {
        amministratoreService = new AmministratoreServiceImpl(
                utenteRepo, passwordEncoder, amministratoreRepo, praticaRepo, documentoRepo,
                commercialistaRepo, gestoreNotifiche);

        admin = new Amministratore();
        admin.setId(1L);
        admin.setNome("Admin");
        admin.setCognome("System");
        admin.setEmail("admin@studio.it");
    }

    @Test
    @DisplayName("aggiornaUltimaAzione: imposta il timestamp e salva")
    void aggiornaUltimaAzione_impostaTimestampESalva() {
        when(amministratoreRepo.findById(1L)).thenReturn(Optional.of(admin));
        when(amministratoreRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        amministratoreService.aggiornaUltimaAzione(1L);

        assertThat(admin.getUltimaAzioneAmministrativa()).isNotNull();
        verify(amministratoreRepo).save(admin);
    }

    @Test
    @DisplayName("aggiornaUltimaAzione: admin non trovato → ResourceNotFoundException")
    void aggiornaUltimaAzione_nonTrovato_lanciaEccezione() {
        when(amministratoreRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> amministratoreService.aggiornaUltimaAzione(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── Verifica delle iscrizioni ────────────────────────────────────────────

    private Commercialista commercialistaInAttesa() {
        Commercialista c = new Commercialista();
        c.setId(7L);
        c.setNome("Giulia");
        c.setCognome("Bianchi");
        c.setEmail("giulia@studio.it");
        c.setNumeroAlbo("12345/A");
        c.setEnabled(false);
        c.setStatoRegistrazione(StatoRegistrazione.IN_ATTESA);
        return c;
    }

    @Test
    @DisplayName("trovaRichiesteIscrizione: restituisce gli account IN_ATTESA")
    void trovaRichiesteIscrizione_filtraInAttesa() {
        Commercialista c = commercialistaInAttesa();
        when(utenteRepo.findByStatoRegistrazioneOrderByDataRegistrazioneAsc(StatoRegistrazione.IN_ATTESA))
                .thenReturn(List.of(c));

        assertThat(amministratoreService.trovaRichiesteIscrizione()).containsExactly(c);
    }

    @Test
    @DisplayName("approvaIscrizione: approva, abilita, notifica l'utente e traccia l'azione")
    void approvaIscrizione_abilitaENotifica() {
        Commercialista c = commercialistaInAttesa();
        when(utenteRepo.findById(7L)).thenReturn(Optional.of(c));
        when(amministratoreRepo.findById(1L)).thenReturn(Optional.of(admin));

        amministratoreService.approvaIscrizione(7L, admin);

        assertThat(c.getStatoRegistrazione()).isEqualTo(StatoRegistrazione.APPROVATA);
        assertThat(c.isEnabled()).isTrue();
        verify(utenteRepo).save(c);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.ACCOUNT_APPROVATO);
        assertThat(captor.getValue().getDestinatario()).isSameAs(c);

        assertThat(admin.getUltimaAzioneAmministrativa()).isNotNull();
    }

    @Test
    @DisplayName("approvaIscrizione: già approvata → InvalidStateException")
    void approvaIscrizione_giaApprovata_lanciaEccezione() {
        Commercialista c = commercialistaInAttesa();
        c.setStatoRegistrazione(StatoRegistrazione.APPROVATA);
        when(utenteRepo.findById(7L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> amministratoreService.approvaIscrizione(7L, admin))
                .isInstanceOf(InvalidStateException.class);
        verify(utenteRepo, never()).save(any());
        verify(gestoreNotifiche, never()).notificaTutti(any());
    }

    @Test
    @DisplayName("rifiutaIscrizione: rifiuta, lascia disabilitato e notifica con la motivazione")
    void rifiutaIscrizione_conMotivazione() {
        Commercialista c = commercialistaInAttesa();
        when(utenteRepo.findById(7L)).thenReturn(Optional.of(c));
        when(amministratoreRepo.findById(1L)).thenReturn(Optional.of(admin));

        amministratoreService.rifiutaIscrizione(7L, "Numero di albo non risultante", admin);

        assertThat(c.getStatoRegistrazione()).isEqualTo(StatoRegistrazione.RIFIUTATA);
        assertThat(c.isEnabled()).isFalse();
        verify(utenteRepo).save(c);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.ACCOUNT_RIFIUTATO);
        assertThat(captor.getValue().getMessaggio()).contains("Numero di albo non risultante");
    }

    @Test
    @DisplayName("rifiutaIscrizione: non in attesa → InvalidStateException")
    void rifiutaIscrizione_nonInAttesa_lanciaEccezione() {
        Commercialista c = commercialistaInAttesa();
        c.setStatoRegistrazione(StatoRegistrazione.RIFIUTATA);
        when(utenteRepo.findById(7L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> amministratoreService.rifiutaIscrizione(7L, null, admin))
                .isInstanceOf(InvalidStateException.class);
        verify(utenteRepo, never()).save(any());
    }

    @Test
    @DisplayName("approvaIscrizione: una richiesta rifiutata può essere approvata in un secondo momento")
    void approvaIscrizione_dopoRifiuto_ok() {
        Commercialista c = commercialistaInAttesa();
        c.setStatoRegistrazione(StatoRegistrazione.RIFIUTATA);
        when(utenteRepo.findById(7L)).thenReturn(Optional.of(c));
        when(amministratoreRepo.findById(1L)).thenReturn(Optional.of(admin));

        amministratoreService.approvaIscrizione(7L, admin);

        assertThat(c.getStatoRegistrazione()).isEqualTo(StatoRegistrazione.APPROVATA);
        assertThat(c.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("calcolaStatistiche: aggrega utenti, pratiche e documenti")
    void calcolaStatistiche_aggrega() {
        com.tesi.gestionalec.model.Cliente cliente = new com.tesi.gestionalec.model.Cliente();
        cliente.setEnabled(true);
        com.tesi.gestionalec.model.Commercialista comm = new com.tesi.gestionalec.model.Commercialista();
        comm.setEnabled(false);

        com.tesi.gestionalec.model.Pratica pratica = new com.tesi.gestionalec.model.Pratica();
        pratica.setStatoCorrente(com.tesi.gestionalec.state.BozzaState.INSTANCE);

        com.tesi.gestionalec.model.Documento doc = new com.tesi.gestionalec.model.Documento();
        doc.setStato(com.tesi.gestionalec.model.StatoDocumento.IN_REVISIONE);

        when(utenteRepo.findAll()).thenReturn(java.util.List.of(cliente, comm));
        when(utenteRepo.findAllDeleted()).thenReturn(java.util.List.of());
        when(praticaRepo.findAll()).thenReturn(java.util.List.of(pratica));
        when(documentoRepo.findAll()).thenReturn(java.util.List.of(doc));

        var stat = amministratoreService.calcolaStatistiche();

        assertThat(stat.getUtentiTotali()).isEqualTo(2);
        assertThat(stat.getUtentiAbilitati()).isEqualTo(1);
        assertThat(stat.getUtentiDisabilitati()).isEqualTo(1);
        assertThat(stat.getPraticheTotali()).isEqualTo(1);
        assertThat(stat.getPratichePerStato()).containsEntry("BOZZA", 1L);
        assertThat(stat.getDocumentiTotali()).isEqualTo(1);
        assertThat(stat.getDocumentiPerStato()).containsEntry("IN_REVISIONE", 1L);
    }

    @Test
    @DisplayName("calcolaStatistiche: espone il numero di iscrizioni in attesa")
    void calcolaStatistiche_richiesteInAttesa() {
        when(utenteRepo.findAll()).thenReturn(List.of());
        when(utenteRepo.findAllDeleted()).thenReturn(List.of());
        when(praticaRepo.findAll()).thenReturn(List.of());
        when(documentoRepo.findAll()).thenReturn(List.of());
        when(utenteRepo.countByStatoRegistrazione(StatoRegistrazione.IN_ATTESA)).thenReturn(3L);

        assertThat(amministratoreService.calcolaStatistiche().getRichiesteInAttesa()).isEqualTo(3);
    }

    // ─── Operazioni amministrative tracciate e protette ──────────────────────

    @Test
    @DisplayName("disabilitaUtente: utente normale → disabilita e traccia l'azione")
    void disabilitaUtente_utenteNormale_disabilitaETraccia() {
        Cliente bersaglio = new Cliente();
        bersaglio.setId(2L);
        bersaglio.setEnabled(true);

        when(utenteRepo.findById(2L)).thenReturn(Optional.of(bersaglio));
        when(utenteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(amministratoreRepo.findById(1L)).thenReturn(Optional.of(admin));
        when(amministratoreRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        amministratoreService.disabilitaUtente(2L, admin);

        assertThat(bersaglio.isEnabled()).isFalse();
        assertThat(admin.getUltimaAzioneAmministrativa()).isNotNull();
        verify(amministratoreRepo).save(admin);

        // L'utente disabilitato viene avvisato con una notifica ACCOUNT_DISABILITATO
        org.mockito.ArgumentCaptor<com.tesi.gestionalec.model.Notifica> captor =
                org.mockito.ArgumentCaptor.forClass(com.tesi.gestionalec.model.Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getTipo())
                .isEqualTo(com.tesi.gestionalec.model.TipoNotifica.ACCOUNT_DISABILITATO);
        assertThat(captor.getValue().getDestinatario()).isEqualTo(bersaglio);
    }

    @Test
    @DisplayName("disabilitaUtente: sé stesso → ForbiddenOperationException")
    void disabilitaUtente_seStesso_lanciaForbidden() {
        assertThatThrownBy(() -> amministratoreService.disabilitaUtente(1L, admin))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("stesso account");

        verify(utenteRepo, never()).save(any());
        verify(amministratoreRepo, never()).save(any());
    }

    @Test
    @DisplayName("eliminaUtente: altro amministratore → ForbiddenOperationException")
    void eliminaUtente_altroAdmin_lanciaForbidden() {
        Amministratore altroAdmin = new Amministratore();
        altroAdmin.setId(2L);
        when(utenteRepo.findById(2L)).thenReturn(Optional.of(altroAdmin));

        assertThatThrownBy(() -> amministratoreService.eliminaUtente(2L, admin))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("altro amministratore");

        verify(utenteRepo, never()).save(any());
        verify(amministratoreRepo, never()).save(any());
    }

    @Test
    @DisplayName("abilitaUtente: nessuna guardia, traccia comunque l'azione")
    void abilitaUtente_tracciaAzione() {
        Cliente bersaglio = new Cliente();
        bersaglio.setId(2L);
        bersaglio.setEnabled(false);

        when(utenteRepo.findById(2L)).thenReturn(Optional.of(bersaglio));
        when(utenteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(amministratoreRepo.findById(1L)).thenReturn(Optional.of(admin));
        when(amministratoreRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        amministratoreService.abilitaUtente(2L, admin);

        assertThat(bersaglio.isEnabled()).isTrue();
        assertThat(admin.getUltimaAzioneAmministrativa()).isNotNull();
    }
}
