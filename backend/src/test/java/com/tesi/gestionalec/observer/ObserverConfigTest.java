package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.repository.NotificaRepo;
import com.tesi.gestionalec.service.impl.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Composizione del pattern Observer: in fase di avvio il soggetto
 * (GestoreNotifiche) viene popolato con i suoi observer concreti.
 *
 * Il test verifica che la registrazione iniziale colleghi al soggetto
 * sia il canale di persistenza sia quello di notifica via email.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Observer - ObserverConfig Unit Tests")
class ObserverConfigTest {

    @Mock
    GestoreNotifiche gestoreNotifiche;
    @Mock
    NotificaRepo notificaRepo;
    @Mock
    EmailService emailService;

    @Test
    @DisplayName("registraObserver → registra DB ed Email observer su GestoreNotifiche")
    void registraObserver_registraEntrambi() {
        DatabaseNotificaObserver dbObserver = new DatabaseNotificaObserver(notificaRepo);
        EmailNotificaObserver emailObserver = new EmailNotificaObserver(emailService);
        ObserverConfig config = new ObserverConfig(gestoreNotifiche, dbObserver, emailObserver);

        config.registraObserver();

        verify(gestoreNotifiche).aggiungiObeserver(dbObserver);
        verify(gestoreNotifiche).aggiungiObeserver(emailObserver);
    }
}
