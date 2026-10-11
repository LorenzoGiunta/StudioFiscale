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
 * Le pratiche candidate sono selezionate con una sola query sull'intervallo di
 * date, senza caricare l'intero archivio.
 *
 * Non servono lock: lo stesso job non viene mai eseguito in parallelo dallo
 * scheduler di Spring, anche con più thread nel pool. In un deployment
 * multi-nodo occorrerebbe un lock distribuito (es. ShedLock con
 * {@code @SchedulerLock}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScadenzaFiscaleScheduler {

    private final PraticaService praticaService;
    private final GestoreNotifiche gestoreNotifiche;
    private final NotificaRepo notificaRepo;

    /** Finestra di preavviso: si notifica da 7 giorni prima fino al giorno stesso. */
    static final int GIORNI_PREAVVISO = 7;

    @Scheduled(cron = "0 0 0 * * *")   // ogni giorno a mezzanotte
    public void controllaScadenze() {
        log.info("Controllo scadenze fiscali avviato...");

        LocalDate oggi = LocalDate.now();

        // Una sola query sull'intervallo utile, invece dell'intero archivio
        List<Pratica> pratiche = praticaService.trovaConScadenzaTra(oggi, oggi.plusDays(GIORNI_PREAVVISO))
                .stream()
                .filter(p -> p.getScadenza() != null && p.getCliente() != null)
                .toList();

        LocalDateTime inizioOggi = oggi.atStartOfDay();
        LocalDateTime fineOggi   = inizioOggi.plusDays(1);

        for (Pratica pratica : pratiche) {
            long giorniMancanti = ChronoUnit.DAYS.between(oggi, pratica.getScadenza());

            // Difensivo: la query filtra già, ma la finestra resta esplicita qui
            if (giorniMancanti >= 0 && giorniMancanti <= GIORNI_PREAVVISO) {
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