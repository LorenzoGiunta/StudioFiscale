package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import com.tesi.gestionalec.service.impl.SpedizioneEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Observer concreto che inoltra le notifiche sul canale email.
 *
 * Determina l'oggetto del messaggio in base al tipo di evento e ne compone
 * il corpo a partire dai dati del destinatario, delegando l'invio effettivo a
 * {@link SpedizioneEmail}. Legge nome ed email sul thread chiamante e passa
 * all'altro thread solo stringhe, così nessuna entità JPA attraversa il
 * confine. Chiama la spedizione asincrona direttamente, e non il servizio
 * transazionale, perché il {@code GestoreNotifiche} lo invoca già dopo il
 * commit: un ulteriore rinvio "al commit" in quella fase non verrebbe mai
 * eseguito.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificaObserver implements NotificaObserver {

    private final SpedizioneEmail spedizione;

    @Override
    public void aggiorna(Notifica notifica) {
        String email = notifica.getDestinatario().getEmail();

        String oggetto = switch (notifica.getTipo()) {
            case CAMBIO_STATO          -> "Aggiornamento stato pratica";
            case DOCUMENTO_CARICATO    -> "Nuovo documento caricato";
            case DOCUMENTO_APPROVATO   -> "Documento approvato";
            case DOCUMENTO_RIFIUTATO   -> "Documento rifiutato";
            case SCADENZA_IMMINENTE    -> "Scadenza imminente";
            case INVITO_COLLABORAZIONE -> "Invito di collaborazione";
            case ACCOUNT_DISABILITATO  -> "Account disabilitato";
            case ACCOUNT_CREATO        -> "Benvenuto su Gestionale Commercialista";
            case RICHIESTA_ISCRIZIONE  -> "Nuova richiesta di iscrizione da verificare";
            case ACCOUNT_APPROVATO     -> "Iscrizione approvata";
            case ACCOUNT_RIFIUTATO     -> "Iscrizione rifiutata";
            case PASSWORD_MODIFICATA   -> "La tua password è stata reimpostata";
        };

        spedizione.spedisci(email, oggetto, costruisciHtml(notifica));
    }

    private String costruisciHtml(Notifica notifica) {
        return """
                <html><body>
                <h2>Ciao %s,</h2>
                <p>%s</p>
                <br><small>Gestionale Commercialista</small>
                </body></html>
                """.formatted(
                notifica.getDestinatario().getNome(),
                notifica.getMessaggio()
        );
    }
}
