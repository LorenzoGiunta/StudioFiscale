package com.tesi.gestionalec.observer.interfaces;

import com.tesi.gestionalec.model.Notifica;

/**
 * Ruolo Subject del pattern Observer: gestisce la registrazione e la rimozione
 * degli observer e ne coordina l'avviso quando si verifica una notifica.
 */
public interface NotificaObservable {
    void aggiungiObeserver(NotificaObserver observer);
    void rimuoviObeserver(NotificaObserver observer);
    void notificaTutti(Notifica notifica);
}
