package com.tesi.gestionalec.exception;

import com.tesi.gestionalec.model.StatoRegistrazione;

/**
 * Segnala un tentativo di accesso con un account la cui iscrizione non è stata
 * (ancora) approvata da un amministratore. Distingue il caso in attesa da
 * quello rifiutato, così che il client possa mostrare un messaggio adeguato
 * anziché un generico "account disabilitato". Tradotta in HTTP 401.
 */
public class RegistrazioneNonApprovataException extends RuntimeException {

    private final StatoRegistrazione stato;

    public RegistrazioneNonApprovataException(StatoRegistrazione stato) {
        super(switch (stato) {
            case IN_ATTESA -> "La tua richiesta di iscrizione è in attesa di verifica da parte di un "
                    + "amministratore. Riceverai un'email non appena l'account sarà attivato.";
            case RIFIUTATA -> "La tua richiesta di iscrizione è stata rifiutata. "
                    + "Per chiarimenti contatta l'amministratore.";
            case APPROVATA -> "L'iscrizione risulta approvata.";
        });
        this.stato = stato;
    }

    public StatoRegistrazione getStato() {
        return stato;
    }

    /** Titolo breve dell'errore, usato dal client per scegliere lo stile dell'avviso. */
    public String getTitolo() {
        return stato == StatoRegistrazione.RIFIUTATA
                ? "Registrazione rifiutata"
                : "Registrazione in attesa";
    }
}
