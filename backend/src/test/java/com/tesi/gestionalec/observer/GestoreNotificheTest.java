package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per il Pattern Observer (GestoreNotifiche).
 * Verifica registrazione, rimozione e notifica degli observer.
 */
@DisplayName("Observer – GestoreNotifiche Unit Tests")
class GestoreNotificheTest {

    private GestoreNotifiche gestore;

    @BeforeEach
    void setUp() {
        gestore = new GestoreNotifiche();
    }

    /** Observer fake che raccoglie le notifiche ricevute */
    static class ObserverFake implements NotificaObserver {
        final List<Notifica> ricevute = new ArrayList<>();
        @Override public void aggiorna(Notifica n) { ricevute.add(n); }
    }

    // ─── aggiungiObserver ─────────────────────────────────────────────────────

    @Test
    @DisplayName("notificaTutti: un observer riceve la notifica")
    void notificaTutti_unObserver_riceveNotifica() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);

        Notifica n = new Notifica();
        n.setMessaggio("Test");
        gestore.notificaTutti(n);

        assertThat(obs.ricevute).hasSize(1);
        assertThat(obs.ricevute.get(0).getMessaggio()).isEqualTo("Test");
    }

    @Test
    @DisplayName("notificaTutti: più observer ricevono tutti la notifica")
    void notificaTutti_piuObserver_tuttiRicevono() {
        ObserverFake obs1 = new ObserverFake();
        ObserverFake obs2 = new ObserverFake();
        ObserverFake obs3 = new ObserverFake();
        gestore.aggiungiObeserver(obs1);
        gestore.aggiungiObeserver(obs2);
        gestore.aggiungiObeserver(obs3);

        gestore.notificaTutti(new Notifica());

        assertThat(obs1.ricevute).hasSize(1);
        assertThat(obs2.ricevute).hasSize(1);
        assertThat(obs3.ricevute).hasSize(1);
    }

    @Test
    @DisplayName("notificaTutti: senza observer non lancia eccezione")
    void notificaTutti_nessunoObserver_nessunErrore() {
        assertThatCode(() -> gestore.notificaTutti(new Notifica()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notificaTutti: più notifiche → observer accumula tutte")
    void notificaTutti_piuNotifiche_tutteAccumulate() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);

        gestore.notificaTutti(new Notifica());
        gestore.notificaTutti(new Notifica());
        gestore.notificaTutti(new Notifica());

        assertThat(obs.ricevute).hasSize(3);
    }

    // ─── rimuoviObserver ──────────────────────────────────────────────────────

    @Test
    @DisplayName("rimuoviObserver: l'observer rimosso non riceve più notifiche")
    void rimuoviObserver_nonRicevePiu() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);
        gestore.rimuoviObeserver(obs);

        gestore.notificaTutti(new Notifica());

        assertThat(obs.ricevute).isEmpty();
    }

    @Test
    @DisplayName("rimuoviObserver: solo l'observer rimosso non riceve, gli altri sì")
    void rimuoviObserver_altriContinuanoARicevere() {
        ObserverFake obs1 = new ObserverFake();
        ObserverFake obs2 = new ObserverFake();
        gestore.aggiungiObeserver(obs1);
        gestore.aggiungiObeserver(obs2);
        gestore.rimuoviObeserver(obs1);

        gestore.notificaTutti(new Notifica());

        assertThat(obs1.ricevute).isEmpty();
        assertThat(obs2.ricevute).hasSize(1);
    }
}
