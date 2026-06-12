package com.tesi.gestionalec.scheduler;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.NotificaRepo;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Processo schedulato per la segnalazione delle scadenze fiscali imminenti.
 *
 * Una volta al giorno esamina le pratiche con scadenza entro una settimana e
 * notifica i clienti interessati, evitando invii ripetuti nella stessa giornata.
 *
 * Il {@code ReentrantLock} è stato rimosso in quanto ridondante: Spring utilizza
 * di default un pool schedulato a singolo thread, quindi due esecuzioni dello
 * stesso job non si sovrappongono mai. In un deployment multi-nodo occorrerebbe
 * un meccanismo di lock distribuito (es. ShedLock con {@code @SchedulerLock}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScadenzaFiscaleScheduler {

    private final PraticaService praticaService;
    private final GestoreNotifiche gestoreNotifiche;
    private final NotificaRepo notificaRepo;

    @Scheduled(cron = "0 0 0 * * *")   // ogni giorno a mezzanotte
    public void controllaScadenze() {
        log.info("Controllo scadenze fiscali avviato...");

        List<Pratica> pratiche = praticaService.trovaTutte()
                .stream()
                .filter(p -> p.getScadenza() != null && p.getCliente() != null)
                .toList();

        LocalDateTime inizioOggi = LocalDate.now().atStartOfDay();
        LocalDateTime fineOggi   = inizioOggi.plusDays(1);

        for (Pratica pratica : pratiche) {
            LocalDate scadenza = pratica.getScadenza();
            LocalDate oggi = LocalDate.now();
            long giorniMancanti = ChronoUnit.DAYS.between(oggi, scadenza);

            // Si notifica solo entro i sette giorni dalla scadenza
            if (giorniMancanti >= 0 && giorniMancanti <= 7) {
                // Al più una notifica di scadenza per cliente nella giornata
                boolean giàNotificato = notificaRepo
                        .existsByDestinatarioAndTipoAndDataCreazioneBetween(
                                pratica.getCliente(),
                                TipoNotifica.SCADENZA_IMMINENTE,
                                inizioOggi,
                                fineOggi);

                if (!giàNotificato) {
                    Notifica notifica = new Notifica();
                    notifica.setDestinatario(pratica.getCliente());
                    notifica.setMessaggio("La pratica \"" + pratica.getTipoPratica().name().replace("_", " ")
                            + "\" scade tra " + giorniMancanti + " giorni!");
                    notifica.setTipo(TipoNotifica.SCADENZA_IMMINENTE);
                    notifica.setLetta(false);

                    gestoreNotifiche.notificaTutti(notifica);
                    log.info("Notifica scadenza inviata per pratica: {}", pratica.getId());
                } else {
                    log.debug("Notifica scadenza già inviata oggi per pratica: {}", pratica.getId());
                }
            }
        }

        log.info("Controllo scadenze fiscali completato.");
    }
}