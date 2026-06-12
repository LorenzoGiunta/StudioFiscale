package com.tesi.gestionalec.observer.interfaces;

import com.tesi.gestionalec.model.Notifica;

/**
 * Ruolo Observer del pattern omonimo: viene avvisato dal soggetto al verificarsi
 * di una notifica e reagisce secondo la propria responsabilità (persistenza,
 * invio email, ecc.).
 */
public interface NotificaObserver {

    /** Notifica all'observer il verificarsi di un nuovo evento. */
    void aggiorna(Notifica notifica);
}
