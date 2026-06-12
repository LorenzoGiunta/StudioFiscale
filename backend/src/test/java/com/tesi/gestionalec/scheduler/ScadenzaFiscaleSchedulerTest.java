package com.tesi.gestionalec.scheduler;

import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.NotificaRepo;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per ScadenzaFiscaleScheduler.
 * Invoca direttamente controllaScadenze() senza attendere il trigger cron.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScadenzaFiscaleScheduler – Unit Tests")
class ScadenzaFiscaleSchedulerTest {

    @Mock PraticaService praticaService;
    @Mock GestoreNotifiche gestoreNotifiche;
    @Mock NotificaRepo notificaRepo;

    @InjectMocks
    ScadenzaFiscaleScheduler scheduler;

    private Cliente cliente;
    private Pratica praticaInScadenza;
    private Pratica praticaFutura;
    private Pratica praticaSenzaScadenza;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Mario");
        cliente.setCognome("Rossi");

        praticaInScadenza = new Pratica();
        praticaInScadenza.setId(10L);
        praticaInScadenza.setCliente(cliente);
        praticaInScadenza.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        praticaInScadenza.setScadenza(LocalDate.now().plusDays(3)); // tra 3 giorni

        praticaFutura = new Pratica();
        praticaFutura.setId(20L);
        praticaFutura.setCliente(cliente);
        praticaFutura.setTipoPratica(TipoPratica.IVA);
        praticaFutura.setScadenza(LocalDate.now().plusDays(30)); // tra 30 giorni, fuori range

        praticaSenzaScadenza = new Pratica();
        praticaSenzaScadenza.setId(30L);
        praticaSenzaScadenza.setCliente(cliente);
        praticaSenzaScadenza.setScadenza(null); // nessuna scadenza impostata
    }

    @Test
    @DisplayName("controllaScadenze: pratica in scadenza entro 7gg → notifica inviata")
    void controllaScadenze_praticaInScadenza_notificaInviata() {
        when(praticaService.trovaTutte()).thenReturn(List.of(praticaInScadenza));
        when(notificaRepo.existsByDestinatarioAndTipoAndDataCreazioneBetween(
                eq(cliente), eq(TipoNotifica.SCADENZA_IMMINENTE), any(), any()))
                .thenReturn(false);

        scheduler.controllaScadenze();

        ArgumentCaptor<Notifica> captor = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(captor.capture());
        Notifica notifica = captor.getValue();
        assertThat(notifica.getDestinatario()).isEqualTo(cliente);
        assertThat(notifica.getTipo()).isEqualTo(TipoNotifica.SCADENZA_IMMINENTE);
        assertThat(notifica.isLetta()).isFalse();
        assertThat(notifica.getMessaggio()).contains("3 giorni");
    }

    @Test
    @DisplayName("controllaScadenze: notifica già inviata oggi → nessuna notifica duplicata")
    void controllaScadenze_notificaGiaInviata_nessunDuplico() {
        when(praticaService.trovaTutte()).thenReturn(List.of(praticaInScadenza));
        when(notificaRepo.existsByDestinatarioAndTipoAndDataCreazioneBetween(
                any(), any(), any(), any()))
                .thenReturn(true); // già notificato oggi

        scheduler.controllaScadenze();

        verify(gestoreNotifiche, never()).notificaTutti(any());
    }

    @Test
    @DisplayName("controllaScadenze: pratica futura (>7gg) → nessuna notifica")
    void controllaScadenze_praticaFutura_nessunNotifica() {
        when(praticaService.trovaTutte()).thenReturn(List.of(praticaFutura));

        scheduler.controllaScadenze();

        verify(gestoreNotifiche, never()).notificaTutti(any());
        verify(notificaRepo, never()).existsByDestinatarioAndTipoAndDataCreazioneBetween(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("controllaScadenze: pratica senza scadenza → ignorata")
    void controllaScadenze_senzaScadenza_ignorata() {
        when(praticaService.trovaTutte()).thenReturn(List.of(praticaSenzaScadenza));

        scheduler.controllaScadenze();

        verify(gestoreNotifiche, never()).notificaTutti(any());
    }

    @Test
    @DisplayName("controllaScadenze: nessuna pratica → nessuna notifica")
    void controllaScadenze_nessunaPratica_nessunNotifica() {
        when(praticaService.trovaTutte()).thenReturn(List.of());

        scheduler.controllaScadenze();

        verify(gestoreNotifiche, never()).notificaTutti(any());
    }
}
