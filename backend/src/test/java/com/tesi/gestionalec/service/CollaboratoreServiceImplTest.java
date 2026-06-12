package com.tesi.gestionalec.service;

import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.CollaboratoreRepo;
import com.tesi.gestionalec.repository.DocumentoRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.impl.CollaboratoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari per CollaboratoreServiceImpl.
 * Copre: approvazione e rifiuto documenti (con motivazione).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CollaboratoreService – Unit Tests")
class CollaboratoreServiceImplTest {

    @Mock UtenteRepo utenteRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CollaboratoreRepo collaboratoreRepo;
    @Mock DocumentoRepo documentoRepo;
    @Mock GestoreNotifiche gestoreNotifiche;

    @InjectMocks
    CollaboratoreServiceImpl collaboratoreService;

    private Documento documento;
    private Cliente autore;

    @BeforeEach
    void setUp() {
        documento = new Documento();
        documento.setId(30L);
        documento.setNome("Fattura_2024.pdf");
        documento.setStato(StatoDocumento.IN_REVISIONE);

        // Cliente che ha caricato il documento: destinatario dell'esito revisione
        autore = new Cliente();
        autore.setId(1L);
        autore.setEmail("anna@studio.it");
        documento.setCaricatoDa(autore);

        // Collaboratore assegnato al documento come revisore
        Collaboratore revisore = new Collaboratore();
        revisore.setId(5L);
        documento.setRevisore(revisore);
    }

    // ─── approvaDocumento ─────────────────────────────────────────────────────

    @Test
    @DisplayName("approvaDocumento imposta stato APPROVATO e salva")
    void approvaDocumento_impostaStatoApprovatoESalva() {
        when(documentoRepo.findById(30L)).thenReturn(Optional.of(documento));
        when(documentoRepo.save(any())).thenReturn(documento);

        // collaboratoreId 5L corrisponde al revisore assegnato
        collaboratoreService.approvaDocumento(30L, 5L);

        assertThat(documento.getStato()).isEqualTo(StatoDocumento.APPROVATO);
        verify(documentoRepo).save(documento);

        // Notifica di esito approvazione al cliente autore
        org.mockito.ArgumentCaptor<Notifica> captor = org.mockito.ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(autore);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.DOCUMENTO_APPROVATO);
        assertThat(captor.getValue().getMessaggio()).contains("approvato", "Fattura_2024.pdf");
    }

    @Test
    @DisplayName("approvaDocumento: revisore non assegnato → ForbiddenOperationException")
    void approvaDocumento_nonAssegnato_lanciaForbidden() {
        when(documentoRepo.findById(30L)).thenReturn(Optional.of(documento));

        // collaboratoreId 99L è diverso dal revisore assegnato (5L)
        assertThatThrownBy(() -> collaboratoreService.approvaDocumento(30L, 99L))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
    }

    @Test
    @DisplayName("approvaDocumento lancia eccezione se documento non trovato")
    void approvaDocumento_nonTrovato_lanciaEccezione() {
        when(documentoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collaboratoreService.approvaDocumento(99L, 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── rifiutaDocumento ─────────────────────────────────────────────────────

    @Test
    @DisplayName("rifiutaDocumento imposta stato RIFIUTATO con motivazione")
    void rifiutaDocumento_impostaStatoERifiuto() {
        when(documentoRepo.findById(30L)).thenReturn(Optional.of(documento));
        when(documentoRepo.save(any())).thenReturn(documento);

        collaboratoreService.rifiutaDocumento(30L, "Documento illeggibile", 5L);

        assertThat(documento.getStato()).isEqualTo(StatoDocumento.RIFIUTATO);
        assertThat(documento.getMotivazioneRifiuto()).isEqualTo("Documento illeggibile");
        verify(documentoRepo).save(documento);

        // Notifica di esito rifiuto al cliente autore, con la motivazione
        org.mockito.ArgumentCaptor<Notifica> captor = org.mockito.ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(autore);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotifica.DOCUMENTO_RIFIUTATO);
        assertThat(captor.getValue().getMessaggio()).contains("rifiutato", "Documento illeggibile");
    }

    @Test
    @DisplayName("rifiutaDocumento: revisore non assegnato → ForbiddenOperationException")
    void rifiutaDocumento_nonAssegnato_lanciaForbidden() {
        when(documentoRepo.findById(30L)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> collaboratoreService.rifiutaDocumento(30L, "motivo", 99L))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(documentoRepo, never()).save(any());
    }

    @Test
    @DisplayName("rifiutaDocumento lancia eccezione se documento non trovato")
    void rifiutaDocumento_nonTrovato_lanciaEccezione() {
        when(documentoRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collaboratoreService.rifiutaDocumento(99L, "motivo", 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("rifiutaDocumento con motivazione vuota salva comunque lo stato RIFIUTATO")
    void rifiutaDocumento_motivazioneVuota_salvaComunque() {
        when(documentoRepo.findById(30L)).thenReturn(Optional.of(documento));
        when(documentoRepo.save(any())).thenReturn(documento);

        collaboratoreService.rifiutaDocumento(30L, "", 5L);

        assertThat(documento.getStato()).isEqualTo(StatoDocumento.RIFIUTATO);
        assertThat(documento.getMotivazioneRifiuto()).isEmpty();
    }

    // ─── trovaPraticheAssegnate / trovaDocumentiInRevisione ───────────────────

    @Test
    @DisplayName("trovaPraticheAssegnate: restituisce pratiche del collaboratore")
    void trovaPraticheAssegnate_restituiscePratiche() {
        Collaboratore collab = new Collaboratore();
        collab.setId(5L);
        Pratica p = new Pratica();
        p.setId(10L);
        collab.setPraticheAssegnate(java.util.List.of(p));

        when(collaboratoreRepo.findById(5L)).thenReturn(Optional.of(collab));

        var risultato = collaboratoreService.trovaPraticheAssegnate(5L);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("trovaDocumentiInRevisione: restituisce documenti del collaboratore")
    void trovaDocumentiInRevisione_restituisceDocumenti() {
        Collaboratore collab = new Collaboratore();
        collab.setId(5L);
        Documento d = new Documento();
        d.setId(20L);
        collab.setDocumentiInRevisione(java.util.List.of(d));

        when(collaboratoreRepo.findById(5L)).thenReturn(Optional.of(collab));

        var risultato = collaboratoreService.trovaDocumentiInRevisione(5L);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getId()).isEqualTo(20L);
    }
}
