package com.tesi.gestionalec.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Servizio per l'invio delle email transazionali.
 *
 * È il punto d'ingresso per i servizi di dominio: se viene chiamato dentro una
 * transazione, la spedizione viene rimandata al commit, così che un rollback
 * successivo non lasci in giro email per operazioni mai avvenute (un invito o
 * un link di reset che sul database non esistono). Fuori da una transazione
 * spedisce subito. In entrambi i casi l'invio effettivo è asincrono, delegato
 * a {@link SpedizioneEmail}, e non incide sui tempi di risposta.
 *
 * Nota: la delega passa per un bean distinto, e non per un metodo di questa
 * stessa classe, perché {@code @Async} agisce solo attraverso il proxy Spring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SpedizioneEmail spedizione;

    public void inviaEmail(String destinatario, String oggetto, String testo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Dentro una transazione: si spedisce solo se e quando va a buon fine
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    spedizione.spedisci(destinatario, oggetto, testo);
                }
            });
            return;
        }
        spedizione.spedisci(destinatario, oggetto, testo);
    }
}
