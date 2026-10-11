package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObservable;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Soggetto (subject) del pattern Observer applicato alle notifiche di dominio.
 *
 * Mantiene l'elenco degli observer interessati agli eventi del sistema e li
 * informa quando si verifica una nuova notifica, senza conoscerne la natura:
 * questo disaccoppia la logica che genera l'evento dai canali che lo gestiscono
 * (persistenza, email, ...), rendendo possibile aggiungerne di nuovi senza
 * modificare i punti in cui le notifiche vengono emesse.
 *
 * La consegna è allineata alla transazione del chiamante: se la notifica viene
 * emessa dentro una transazione, gli observer vengono avvisati solo dopo il
 * commit. In caso di rollback non parte nulla, così l'utente non riceve mai
 * email o notifiche in-app per operazioni che in realtà non sono avvenute.
 * Fuori da una transazione (job schedulati, controller) la consegna è immediata.
 */
@Slf4j
@Component
public class GestoreNotifiche implements NotificaObservable {

    private final List<NotificaObserver> observers = new CopyOnWriteArrayList<>();

    @Override
    public void aggiungiObeserver(NotificaObserver observer) {
        observers.add(observer);
    }

    @Override
    public void rimuoviObeserver(NotificaObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notificaTutti(Notifica notifica) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Dentro una transazione: la consegna viene rimandata al commit.
            // Se la transazione fallisce, afterCommit non viene mai invocato.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    consegna(notifica);
                }
            });
            return;
        }
        consegna(notifica);
    }

    // La consegna a ciascun observer è asincrona (@Async lato observer): il
    // ciclo si limita a inoltrare l'evento e non attende il loro esito. Un
    // observer che fallisce in modo sincrono non impedisce agli altri di
    // ricevere la notifica.
    private void consegna(Notifica notifica) {
        for (NotificaObserver observer : observers) {
            try {
                observer.aggiorna(notifica);
            } catch (RuntimeException ex) {
                log.error("Observer {} ha fallito la consegna della notifica di tipo {}: {}",
                        observer.getClass().getSimpleName(), notifica.getTipo(), ex.getMessage(), ex);
            }
        }
    }
}
