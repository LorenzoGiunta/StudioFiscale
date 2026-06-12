package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import com.tesi.gestionalec.repository.NotificaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Observer concreto responsabile della persistenza delle notifiche.
 *
 * Ogni evento ricevuto viene reso permanente sul database, così da poter
 * essere riproposto all'utente anche in un momento successivo. Il salvataggio
 * è eseguito in modo asincrono su un pool dedicato ({@code notificaExecutor}):
 * l'invocazione passa per il proxy AOP di Spring, condizione necessaria
 * affinché l'annotazione {@code @Async} abbia effetto.
 */
@Component
@RequiredArgsConstructor
public class DatabaseNotificaObserver implements NotificaObserver {

    private final NotificaRepo repo;

    @Override
    @Async("notificaExecutor")
    public void aggiorna(Notifica notifica) {
        repo.save(notifica);
    }
}
