package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.observer.interfaces.NotificaObserver;
import com.tesi.gestionalec.service.impl.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Observer concreto che inoltra le notifiche sul canale email.
 *
 * Determina l'oggetto del messaggio in base al tipo di evento e ne compone
 * il corpo a partire dai dati del destinatario, delegando l'invio effettivo
 * all'{@link EmailService}. Non è necessario un pool separato: l'invio SMTP
 * asincrono è già gestito da {@code EmailService} su {@code emailExecutor},
 * evitando il doppio salto di thread che occupava {@code notificaExecutor}
 * per poi passare immediatamente a {@code emailExecutor}.
 */
@Component
@RequiredArgsConstructor
public class EmailNotificaObserver implements NotificaObserver {

    private final EmailService emailService;

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
        };

        emailService.inviaEmail(email, oggetto, costruisciHtml(notifica));
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