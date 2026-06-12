package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObservable;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import org.springframework.stereotype.Component;

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
 */
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
        // La consegna a ciascun observer è asincrona (@Async lato observer):
        // il ciclo si limita a inoltrare l'evento e non attende il loro esito.
        for (NotificaObserver observer : observers) {
            observer.aggiorna(notifica);
        }
    }
}
