package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per il Pattern Observer (GestoreNotifiche).
 * Verifica registrazione, rimozione e notifica degli observer, e
 * l'allineamento della consegna alla transazione del chiamante.
 */
@DisplayName("Observer – GestoreNotifiche Unit Tests")
class GestoreNotificheTest {

    private GestoreNotifiche gestore;

    @BeforeEach
    void setUp() {
        gestore = new GestoreNotifiche();
    }

    @AfterEach
    void tearDown() {
        // Ripulisce l'eventuale "transazione" simulata sul thread di test
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** Observer fake che raccoglie le notifiche ricevute */
    static class ObserverFake implements NotificaObserver {
        final List<Notifica> ricevute = new ArrayList<>();
        @Override public void aggiorna(Notifica n) { ricevute.add(n); }
    }

    /** Observer fake che fallisce sempre */
    static class ObserverRotto implements NotificaObserver {
        @Override public void aggiorna(Notifica n) { throw new IllegalStateException("boom"); }
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

    // ─── consegna allineata alla transazione ─────────────────────────────────

    @Test
    @DisplayName("dentro una transazione: la notifica NON parte finché non c'è il commit")
    void inTransazione_consegnaSoloDopoCommit() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);
        TransactionSynchronizationManager.initSynchronization();   // simula una transazione aperta

        gestore.notificaTutti(new Notifica());

        assertThat(obs.ricevute).as("prima del commit").isEmpty();

        // Commit: il transaction manager invoca afterCommit sulle sincronizzazioni registrate
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        assertThat(obs.ricevute).as("dopo il commit").hasSize(1);
    }

    @Test
    @DisplayName("dentro una transazione: con rollback non parte nulla")
    void inTransazione_rollback_nessunaConsegna() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);
        TransactionSynchronizationManager.initSynchronization();

        gestore.notificaTutti(new Notifica());

        // Rollback: afterCommit non viene mai chiamato, solo afterCompletion
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        assertThat(obs.ricevute).isEmpty();
    }

    @Test
    @DisplayName("dentro una transazione: più notifiche vengono consegnate tutte al commit, in ordine")
    void inTransazione_piuNotifiche_tutteAlCommit() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);
        TransactionSynchronizationManager.initSynchronization();

        Notifica prima = new Notifica();  prima.setMessaggio("1");
        Notifica seconda = new Notifica(); seconda.setMessaggio("2");
        gestore.notificaTutti(prima);
        gestore.notificaTutti(seconda);
        assertThat(obs.ricevute).isEmpty();

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        assertThat(obs.ricevute).extracting(Notifica::getMessaggio).containsExactly("1", "2");
    }

    @Test
    @DisplayName("fuori da una transazione: la consegna è immediata")
    void fuoriTransazione_consegnaImmediata() {
        ObserverFake obs = new ObserverFake();
        gestore.aggiungiObeserver(obs);
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

        gestore.notificaTutti(new Notifica());

        assertThat(obs.ricevute).hasSize(1);
    }

    // ─── isolamento degli observer ───────────────────────────────────────────

    @Test
    @DisplayName("un observer che fallisce non impedisce agli altri di ricevere la notifica")
    void observerRotto_nonBloccaGliAltri() {
        ObserverFake ok = new ObserverFake();
        gestore.aggiungiObeserver(new ObserverRotto());
        gestore.aggiungiObeserver(ok);

        assertThatCode(() -> gestore.notificaTutti(new Notifica())).doesNotThrowAnyException();
        assertThat(ok.ricevute).hasSize(1);
    }
}
