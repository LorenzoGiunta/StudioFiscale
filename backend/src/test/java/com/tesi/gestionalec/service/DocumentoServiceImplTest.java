package com.tesi.gestionalec.service;

import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.ClienteRepo;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.DocumentoRepo;
import com.tesi.gestionalec.repository.InvitoCollaborazioneRepo;
import com.tesi.gestionalec.service.impl.DocumentoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari per DocumentoServiceImpl.
 * Copre: caricamento iniziale, versionamento, ownership check in base al ruolo,
 * assegnazione revisore, eliminazione logica e l'invio delle notifiche agli
 * interessati (autore, commercialista e collaboratore assegnato) sia al primo
 * caricamento sia al caricamento di una nuova versione.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoService – Unit Tests")
class DocumentoServiceImplTest {

    @Mock DocumentoRepo documentoRepo;
    @Mock CollaboratoreRepo collaboratoreRepo;
    @Mock ClienteRepo clienteRepo;
    @Mock InvitoCollaborazioneRepo invitoRepo;
    @Mock GestoreNotifiche gestoreNotifiche;

    @InjectMocks
    DocumentoServiceImpl documentoService;

    private Documento documentoBase;
    private Collaboratore revisore;
    private Cliente cliente;
    private Commercialista commercialista;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Anna");
        cliente.setCognome("Verdi");

        commercialista = new Commercialista();
        commercialista.setId(7L);
        cliente.setCommercialista(commercialista);

        Pratica pratica = new Pratica();
        pratica.setId(10L);
        pratica.setCliente(cliente);

        documentoBase = new Documento();
        documentoBase.setId(20L);
        documentoBase.setNome("CUD_2024.pdf");
        documentoBase.setTipoFile("CUD");
        documentoBase.setPercorsoFile("uploads/documenti/uuid_CUD_2024.pdf");
        documentoBase.setDimensione(102400L);
        documentoBase.setPratica(pratica);
        documentoBase.setCaricatoDa(cliente);
        documentoBase.setVersione(1);
        documentoBase.setStato(StatoDocumento.IN_REVISIONE);

        revisore = new Collaboratore();
        revisore.setId(5L);
        revisore.setNome("Luca");
        revisore.setCognome("Bianchi");
        documentoBase.setRevisore(revisore);
    }

    // ─── caricaDocumento ──────────────────────────────────────────────────────

    @Test
    @DisplayName("caricaDocumento imposta versione 1 e stato IN_REVISIONE")
    void caricaDocumento_impostaVersioneUnoEInRevisione() {
        Documento nuovo = new Documento();
        nuovo.setNome("Fattura.pdf");
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Documento salvato = documentoService.caricaDocumento(nuovo);

        assertThat(salvato.getVersione()).isEqualTo(1);
        assertThat(salvato.getStato()).isEqualTo(StatoDocumento.IN_REVISIONE);
        verify(documentoRepo).save(nuovo);
    }

    @Test
    @DisplayName("caricaDocumento: notifica autore, commercialista e collaboratore assegnato")
    void caricaDocumento_inviaNotificheAiTreDestinatari() {
        Collaboratore assegnatario = new Collaboratore();
        assegnatario.setId(8L);
        assegnatario.setNome("Paolo");
        assegnatario.setCognome("Neri");

        Pratica pratica = new Pratica();
        pratica.setId(10L);
        pratica.setCliente(cliente);
        pratica.setAssegnataA(assegnatario);

        Documento nuovo = new Documento();
        nuovo.setNome("Fattura.pdf");
        nuovo.setCaricatoDa(cliente);
        nuovo.setPratica(pratica);

        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // cliente.commercialista è già valorizzato in setUp (commercialista id=7)
        when(clienteRepo.findByIdConCommercialista(1L)).thenReturn(Optional.of(cliente));

        documentoService.caricaDocumento(nuovo);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche, times(3)).notificaTutti(captor.capture());

        List<Notifica> notifiche = captor.getAllValues();
        // tutte di tipo DOCUMENTO_CARICATO e non lette
        assertThat(notifiche).allMatch(n -> n.getTipo() == TipoNotifica.DOCUMENTO_CARICATO);
        assertThat(notifiche).allMatch(n -> !n.isLetta());
        // i tre destinatari corretti: autore, commercialista, collaboratore assegnato
        assertThat(notifiche).extracting(Notifica::getDestinatario)
                .containsExactlyInAnyOrder(cliente, commercialista, assegnatario);
        // il messaggio per l'autore è una conferma, quello per gli altri cita il cliente
        assertThat(notifiche)
                .filteredOn(n -> n.getDestinatario() == cliente)
                .singleElement()
                .satisfies(n -> assertThat(n.getMessaggio()).contains("Il tuo documento", "Fattura.pdf"));
        assertThat(notifiche)
                .filteredOn(n -> n.getDestinatario() == commercialista)
                .singleElement()
                .satisfies(n -> assertThat(n.getMessaggio()).contains("Anna Verdi", "Fattura.pdf"));
    }

    @Test
    @DisplayName("caricaDocumento: senza collaboratore assegnato notifica solo autore e commercialista")
    void caricaDocumento_senzaAssegnatario_notificaDue() {
        Pratica pratica = new Pratica();
        pratica.setId(10L);
        pratica.setCliente(cliente);
        // nessun collaboratore assegnato

        Documento nuovo = new Documento();
        nuovo.setNome("F24.pdf");
        nuovo.setCaricatoDa(cliente);
        nuovo.setPratica(pratica);

        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepo.findByIdConCommercialista(1L)).thenReturn(Optional.of(cliente));

        documentoService.caricaDocumento(nuovo);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche, times(2)).notificaTutti(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notifica::getDestinatario)
                .containsExactlyInAnyOrder(cliente, commercialista);
    }

    @Test
    @DisplayName("caricaDocumento: contesto incompleto (senza autore) non emette notifiche")
    void caricaDocumento_senzaAutore_nessunaNotifica() {
        Documento nuovo = new Documento();
        nuovo.setNome("Senza.pdf");
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentoService.caricaDocumento(nuovo);

        verifyNoInteractions(gestoreNotifiche);
        verifyNoInteractions(clienteRepo);
    }

    // ─── trovaPerId ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerId restituisce il documento se esiste")
    void trovaPerId_esistente() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));

        Documento trovato = documentoService.trovaPerId(20L);

        assertThat(trovato.getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("trovaPerId lancia RuntimeException se non esiste")
    void trovaPerId_nonEsistente_lanciaEccezione() {
        when(documentoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.trovaPerId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── trovaPerId con ownership check ──────────────────────────────────

    @Test
    @DisplayName("trovaPerId(id, utente): CLIENTE autore → restituisce documento")
    void trovaPerId_clienteAutore_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));

        Documento trovato = documentoService.trovaPerId(20L, cliente);

        assertThat(trovato.getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("trovaPerId(id, utente): CLIENTE non autore → ForbiddenOperationException")
    void trovaPerId_clienteAltro_lanciaForbidden() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));

        Cliente altroCliente = new Cliente();
        altroCliente.setId(99L);

        assertThatThrownBy(() -> documentoService.trovaPerId(20L, altroCliente))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("trovaPerId(id, utente): COLLABORATORE revisore assegnato → restituisce documento")
    void trovaPerId_collaboratoreRevisore_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));

        Documento trovato = documentoService.trovaPerId(20L, revisore);

        assertThat(trovato.getId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("trovaPerId(id, utente): COMMERCIALISTA corretto → restituisce documento")
    void trovaPerId_commercialistaCorretto_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));

        Documento trovato = documentoService.trovaPerId(20L, commercialista);

        assertThat(trovato.getId()).isEqualTo(20L);
    }

    // ─── nuovaVersione ────────────────────────────────────────────────────────

    @Test
    @DisplayName("nuovaVersione incrementa il numero di versione del documento")
    void nuovaVersione_incrementaVersione() {
        documentoBase.setVersione(2);
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Documento nuovoDoc = new Documento();
        nuovoDoc.setNome("CUD_2024_v2.pdf");

        Documento salvato = documentoService.nuovaVersione(20L, nuovoDoc);

        assertThat(salvato.getVersione()).isEqualTo(3); // 2 + 1
        assertThat(salvato.getStato()).isEqualTo(StatoDocumento.IN_REVISIONE);
    }

    @Test
    @DisplayName("nuovaVersione eredita pratica e cliente dal documento originale")
    void nuovaVersione_ereditaPraticaECliente() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Documento nuovoDoc = new Documento();
        Documento salvato = documentoService.nuovaVersione(20L, nuovoDoc);

        assertThat(salvato.getPratica()).isEqualTo(documentoBase.getPratica());
        assertThat(salvato.getCaricatoDa()).isEqualTo(documentoBase.getCaricatoDa());
    }

    @Test
    @DisplayName("nuovaVersione(id, doc, utente): autore originale → salva nuova versione")
    void nuovaVersione_autoreOriginale_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Documento nuovoDoc = new Documento();
        Documento salvato = documentoService.nuovaVersione(20L, nuovoDoc, cliente);

        assertThat(salvato.getVersione()).isEqualTo(2); // 1 + 1
    }

    @Test
    @DisplayName("nuovaVersione(id, doc, utente): cliente diverso → ForbiddenOperationException")
    void nuovaVersione_altrocliente_lanciaForbidden() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));

        Cliente altroCliente = new Cliente();
        altroCliente.setId(99L);

        assertThatThrownBy(() -> documentoService.nuovaVersione(20L, new Documento(), altroCliente))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
    }

    @Test
    @DisplayName("nuovaVersione: notifica autore, commercialista e collaboratore con testo 'nuova versione'")
    void nuovaVersione_inviaNotifiche() {
        Collaboratore assegnatario = new Collaboratore();
        assegnatario.setId(8L);
        assegnatario.setNome("Paolo");
        assegnatario.setCognome("Neri");
        documentoBase.getPratica().setAssegnataA(assegnatario);

        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepo.findByIdConCommercialista(1L)).thenReturn(Optional.of(cliente));

        Documento nuovaVer = new Documento();
        nuovaVer.setNome("CUD_2024_v2.pdf");
        documentoService.nuovaVersione(20L, nuovaVer);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche, times(3)).notificaTutti(captor.capture());

        List<Notifica> notifiche = captor.getAllValues();
        assertThat(notifiche).extracting(Notifica::getDestinatario)
                .containsExactlyInAnyOrder(cliente, commercialista, assegnatario);
        assertThat(notifiche).allMatch(n -> n.getTipo() == TipoNotifica.DOCUMENTO_CARICATO);
        // tutti i messaggi devono indicare che si tratta di una nuova versione
        assertThat(notifiche).allMatch(n -> n.getMessaggio().toLowerCase().contains("nuova versione"));
        // conferma all'autore vs avviso che cita il cliente
        assertThat(notifiche)
                .filteredOn(n -> n.getDestinatario() == cliente)
                .singleElement()
                .satisfies(n -> assertThat(n.getMessaggio()).contains("La nuova versione", "CUD_2024_v2.pdf"));
        assertThat(notifiche)
                .filteredOn(n -> n.getDestinatario() == assegnatario)
                .singleElement()
                .satisfies(n -> assertThat(n.getMessaggio()).contains("Anna Verdi", "CUD_2024_v2.pdf"));
    }

    // ─── assegnaRevisore ──────────────────────────────────────────────────────

    @Test
    @DisplayName("assegnaRevisore setta il collaboratore sul documento")
    void assegnaRevisore_settaIlCollaboratore() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(collaboratoreRepo.findById(5L)).thenReturn(Optional.of(revisore));
        when(documentoRepo.save(any())).thenReturn(documentoBase);

        documentoService.assegnaRevisore(20L, 5L);

        assertThat(documentoBase.getRevisore()).isEqualTo(revisore);
        verify(documentoRepo).save(documentoBase);
    }

    @Test
    @DisplayName("assegnaRevisore lancia eccezione se collaboratore non trovato")
    void assegnaRevisore_collaboratoreNonTrovato_lanciaEccezione() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(collaboratoreRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.assegnaRevisore(20L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── varianti sicure assegnaRevisore (anti-IDOR) ──────────────────────────

    @Test
    @DisplayName("assegnaRevisore(richiedente): documento di altro studio → Forbidden")
    void assegnaRevisore_conRichiedente_documentoAltroStudio_forbidden() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        Commercialista altroComm = new Commercialista();
        altroComm.setId(99L);

        assertThatThrownBy(() -> documentoService.assegnaRevisore(20L, 5L, altroComm))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
    }

    @Test
    @DisplayName("assegnaRevisore(richiedente): collaboratore non dello studio → Forbidden")
    void assegnaRevisore_conRichiedente_collabFuoriStudio_forbidden() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(invitoRepo.existsByCommercialista_IdAndCollaboratore_IdAndStato(
                7L, 5L, StatoInvito.ACCEPTED)).thenReturn(false);

        assertThatThrownBy(() -> documentoService.assegnaRevisore(20L, 5L, commercialista))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
    }

    @Test
    @DisplayName("assegnaRevisore(richiedente): documento e collaboratore dello studio → assegna")
    void assegnaRevisore_conRichiedente_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(invitoRepo.existsByCommercialista_IdAndCollaboratore_IdAndStato(
                7L, 5L, StatoInvito.ACCEPTED)).thenReturn(true);
        when(collaboratoreRepo.findById(5L)).thenReturn(Optional.of(revisore));
        when(documentoRepo.save(any())).thenReturn(documentoBase);

        documentoService.assegnaRevisore(20L, 5L, commercialista);

        assertThat(documentoBase.getRevisore()).isEqualTo(revisore);
        verify(documentoRepo).save(documentoBase);
    }

    // ─── approvaDocumento / rifiutaDocumento (commercialista o revisore) ──────

    @Test
    @DisplayName("approvaDocumento: commercialista dello studio → APPROVATO e notifica all'autore")
    void approvaDocumento_commercialista_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentoService.approvaDocumento(20L, commercialista);

        assertThat(documentoBase.getStato()).isEqualTo(StatoDocumento.APPROVATO);

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(cliente);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.DOCUMENTO_APPROVATO);
        assertThat(captor.getValue().getMessaggio()).contains("approvato", "CUD_2024.pdf");
    }

    @Test
    @DisplayName("approvaDocumento: revisore assegnato → APPROVATO (ownership per ruolo)")
    void approvaDocumento_revisore_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentoService.approvaDocumento(20L, revisore);

        assertThat(documentoBase.getStato()).isEqualTo(StatoDocumento.APPROVATO);
        verify(gestoreNotifiche).notificaTutti(any(Notifica.class));
    }

    @Test
    @DisplayName("approvaDocumento: commercialista di altro studio → Forbidden, nessun salvataggio")
    void approvaDocumento_altroStudio_forbidden() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        Commercialista altroComm = new Commercialista();
        altroComm.setId(99L);

        assertThatThrownBy(() -> documentoService.approvaDocumento(20L, altroComm))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
        verifyNoInteractions(gestoreNotifiche);
    }

    @Test
    @DisplayName("rifiutaDocumento: commercialista → RIFIUTATO con motivazione e notifica all'autore")
    void rifiutaDocumento_commercialista_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentoService.rifiutaDocumento(20L, "Dati mancanti", commercialista);

        assertThat(documentoBase.getStato()).isEqualTo(StatoDocumento.RIFIUTATO);
        assertThat(documentoBase.getMotivazioneRifiuto()).isEqualTo("Dati mancanti");

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(cliente);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.DOCUMENTO_RIFIUTATO);
        assertThat(captor.getValue().getMessaggio()).contains("rifiutato", "Dati mancanti");
    }

    // ─── trovaPerPratica ──────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerPratica: delega al repo e restituisce lista")
    void trovaPerPratica_delegaAlRepo() {
        Pratica pratica = new Pratica();
        pratica.setId(10L);
        when(documentoRepo.findByPratica(pratica)).thenReturn(List.of(documentoBase));

        var lista = documentoService.trovaPerPratica(pratica);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getId()).isEqualTo(20L);
        verify(documentoRepo).findByPratica(pratica);
    }

    // ─── eliminaDocumento ─────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminaDocumento: imposta deleted=true e salva")
    void eliminaDocumento_impostaDeletedESalva() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentoService.eliminaDocumento(20L);

        assertThat(documentoBase.isDeleted()).isTrue();
        assertThat(documentoBase.getDeletedAt()).isNotNull();
        verify(documentoRepo).save(documentoBase);
    }

    @Test
    @DisplayName("eliminaDocumento: documento non trovato → ResourceNotFoundException")
    void eliminaDocumento_nonTrovato_lanciaEccezione() {
        when(documentoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.eliminaDocumento(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── variante sicura eliminaDocumento (anti-IDOR) ─────────────────────────

    @Test
    @DisplayName("eliminaDocumento(richiedente): documento di altro studio → Forbidden")
    void eliminaDocumento_conRichiedente_altroStudio_forbidden() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        Commercialista altroComm = new Commercialista();
        altroComm.setId(99L);

        assertThatThrownBy(() -> documentoService.eliminaDocumento(20L, altroComm))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
    }

    @Test
    @DisplayName("eliminaDocumento(richiedente): commercialista dello studio → cancellazione logica")
    void eliminaDocumento_conRichiedente_proprietario_ok() {
        when(documentoRepo.findById(20L)).thenReturn(Optional.of(documentoBase));
        when(documentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentoService.eliminaDocumento(20L, commercialista);

        assertThat(documentoBase.isDeleted()).isTrue();
        assertThat(documentoBase.getDeletedAt()).isNotNull();
        verify(documentoRepo).save(documentoBase);
    }
}
