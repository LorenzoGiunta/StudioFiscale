package com.tesi.gestionalec.service;

import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.NotificaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.impl.NotificaServiceImpl;
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
 * Test unitari per NotificaServiceImpl.
 * Copre: recupero, segna-letta singola, segna-tutte-lette.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificaService – Unit Tests")
class NotificaServiceImplTest {

    @Mock NotificaRepo notificaRepo;
    @Mock UtenteRepo utenteRepo;

    @InjectMocks
    NotificaServiceImpl notificaService;

    private Utente utente;
    private Notifica notifica1;
    private Notifica notifica2;

    @BeforeEach
    void setUp() {
        utente = new Cliente();
        utente.setId(1L);

        notifica1 = new Notifica();
        notifica1.setId(100L);
        notifica1.setDestinatario(utente);
        notifica1.setMessaggio("Pratica creata");
        notifica1.setLetta(false);

        notifica2 = new Notifica();
        notifica2.setId(101L);
        notifica2.setDestinatario(utente);
        notifica2.setMessaggio("Documento approvato");
        notifica2.setLetta(false);
    }

    // ─── trovaPerUtente ───────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerUtente restituisce tutte le notifiche dell'utente")
    void trovaPerUtente_restituisceTutte() {
        when(notificaRepo.findByDestinatario(utente)).thenReturn(List.of(notifica1, notifica2));

        List<Notifica> result = notificaService.trovaPerUtente(utente);

        assertThat(result).hasSize(2);
        verify(notificaRepo).findByDestinatario(utente);
    }

    // ─── trovaNonLette ────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaNonLette restituisce solo le notifiche non lette")
    void trovaNonLette_soloNonLette() {
        notifica2.setLetta(true); // questa è già letta
        when(notificaRepo.findByDestinatarioAndLettaFalse(utente)).thenReturn(List.of(notifica1));

        List<Notifica> result = notificaService.trovaNonLette(utente);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLetta()).isFalse();
    }

    // ─── segnaComeLetta ───────────────────────────────────────────────────────

    @Test
    @DisplayName("segnaComeLetta imposta letta=true e salva")
    void segnaComeLetta_impostaLettaTrue() {
        when(notificaRepo.findById(100L)).thenReturn(Optional.of(notifica1));

        notificaService.segnaComeLetta(100L);

        assertThat(notifica1.isLetta()).isTrue();
        verify(notificaRepo).save(notifica1);
    }

    @Test
    @DisplayName("segnaComeLetta lancia eccezione se notifica non trovata")
    void segnaComeLetta_nonTrovata_lanciaEccezione() {
        when(notificaRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificaService.segnaComeLetta(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    // ─── segnaComeLetta con ownership check ──────────────────────────────────

    @Test
    @DisplayName("segnaComeLetta(id, utente): proprietario → imposta letta=true")
    void segnaComeLettaConUtente_proprietario_impostaLetta() {
        when(notificaRepo.findById(100L)).thenReturn(Optional.of(notifica1));

        notificaService.segnaComeLetta(100L, utente);

        assertThat(notifica1.isLetta()).isTrue();
        verify(notificaRepo).save(notifica1);
    }

    @Test
    @DisplayName("segnaComeLetta(id, utente): utente diverso → ForbiddenOperationException")
    void segnaComeLettaConUtente_altrui_lanciaForbidden() {
        when(notificaRepo.findById(100L)).thenReturn(Optional.of(notifica1));

        Utente altroUtente = new Cliente();
        altroUtente.setId(99L); // diverso dal destinatario (id=1L)

        assertThatThrownBy(() -> notificaService.segnaComeLetta(100L, altroUtente))
                .isInstanceOf(com.tesi.gestionalec.exception.ForbiddenOperationException.class);
        verify(notificaRepo, never()).save(any());
    }

    // ─── segnaComeLetteTutte ──────────────────────────────────────────────────

    @Test
    @DisplayName("segnaComeLetteTutte imposta letta=true su tutte le non lette")
    void segnaComeLetteTutte_tutteImpostateALetta() {
        when(utenteRepo.findById(1L)).thenReturn(Optional.of(utente));
        when(notificaRepo.findByDestinatarioAndLettaFalse(utente))
                .thenReturn(List.of(notifica1, notifica2));

        notificaService.segnaComeLetteTutte(1L);

        assertThat(notifica1.isLetta()).isTrue();
        assertThat(notifica2.isLetta()).isTrue();

        // Deve salvare TUTTE in un'unica chiamata saveAll
        ArgumentCaptor<List<Notifica>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificaRepo).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("segnaComeLetteTutte: utente senza notifiche non chiama saveAll con elementi")
    void segnaComeLetteTutte_nessunaNotifica_saveAllListaVuota() {
        when(utenteRepo.findById(1L)).thenReturn(Optional.of(utente));
        when(notificaRepo.findByDestinatarioAndLettaFalse(utente)).thenReturn(List.of());

        notificaService.segnaComeLetteTutte(1L);

        ArgumentCaptor<List<Notifica>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificaRepo).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("segnaComeLetteTutte lancia eccezione se utente non trovato")
    void segnaComeLetteTutte_utenteNonTrovato_lanciaEccezione() {
        when(utenteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificaService.segnaComeLetteTutte(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
