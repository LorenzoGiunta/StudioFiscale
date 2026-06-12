package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.repository.NotificaRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Concrete observer del pattern Observer incaricato della persistenza:
 * ogni notifica emessa dal soggetto osservato viene salvata sul database.
 *
 * Il repository è sostituito da un mock per verificare la delega senza
 * dipendere da un'istanza reale di persistenza.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Observer - DatabaseNotificaObserver Unit Tests")
class DatabaseNotificaObserverTest {

    @Mock
    NotificaRepo repo;

    @Test
    @DisplayName("aggiorna → salva la notifica nel repository")
    void aggiorna_salvaNotifica() {
        DatabaseNotificaObserver observer = new DatabaseNotificaObserver(repo);
        Notifica notifica = new Notifica();
        notifica.setMessaggio("Persistimi");

        observer.aggiorna(notifica);

        verify(repo).save(notifica);
    }
}
