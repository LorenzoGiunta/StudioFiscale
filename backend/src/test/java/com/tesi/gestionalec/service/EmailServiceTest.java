package com.tesi.gestionalec.service;

import com.tesi.gestionalec.service.impl.EmailService;
import com.tesi.gestionalec.service.impl.SpedizioneEmail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.*;

/**
 * Test unitari per EmailService: la spedizione è allineata alla transazione del
 * chiamante (parte solo al commit, mai su rollback) ed è immediata fuori da una
 * transazione. L'invio SMTP vero è delegato a SpedizioneEmail, qui mockata.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService – Unit Tests")
class EmailServiceTest {

    @Mock SpedizioneEmail spedizione;

    @InjectMocks
    EmailService emailService;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("fuori da una transazione: spedisce subito")
    void fuoriTransazione_spedisceSubito() {
        emailService.inviaEmail("a@b.it", "Oggetto", "<p>corpo</p>");

        verify(spedizione).spedisci("a@b.it", "Oggetto", "<p>corpo</p>");
    }

    @Test
    @DisplayName("dentro una transazione: non spedisce finché non c'è il commit")
    void inTransazione_spedisceAlCommit() {
        TransactionSynchronizationManager.initSynchronization();

        emailService.inviaEmail("a@b.it", "Oggetto", "corpo");
        verify(spedizione, never()).spedisci(any(), any(), any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(spedizione).spedisci("a@b.it", "Oggetto", "corpo");
    }

    @Test
    @DisplayName("dentro una transazione: con rollback l'email non parte")
    void inTransazione_rollback_nonSpedisce() {
        TransactionSynchronizationManager.initSynchronization();

        emailService.inviaEmail("a@b.it", "Oggetto", "corpo");

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verify(spedizione, never()).spedisci(any(), any(), any());
    }
}
