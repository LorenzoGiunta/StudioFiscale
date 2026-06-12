package com.tesi.gestionalec.observer;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.service.impl.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Concrete observer del pattern Observer: alla ricezione di una notifica
 * traduce il tipo di evento in un'email verso il destinatario.
 *
 * I test isolano l'observer dal resto del sistema sostituendo l'EmailService
 * con un mock, così da verificarne la sola responsabilità: scegliere l'oggetto
 * coerente con il tipo di notifica e comporre il corpo del messaggio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Observer - EmailNotificaObserver Unit Tests")
class EmailNotificaObserverTest {

    @Mock
    EmailService emailService;

    private Notifica notificaConTipo(TipoNotifica tipo) {
        Cliente destinatario = new Cliente();
        destinatario.setNome("Mario");
        destinatario.setEmail("mario@test.it");

        Notifica n = new Notifica();
        n.setTipo(tipo);
        n.setMessaggio("Messaggio di prova");
        n.setDestinatario(destinatario);
        return n;
    }

    @Test
    @DisplayName("aggiorna CAMBIO_STATO → oggetto 'Aggiornamento stato pratica'")
    void aggiorna_cambioStato_oggettoCorretto() {
        EmailNotificaObserver observer = new EmailNotificaObserver(emailService);

        observer.aggiorna(notificaConTipo(TipoNotifica.CAMBIO_STATO));

        verify(emailService).inviaEmail(eq("mario@test.it"),
                eq("Aggiornamento stato pratica"), anyString());
    }

    @Test
    @DisplayName("aggiorna DOCUMENTO_CARICATO → oggetto 'Nuovo documento caricato'")
    void aggiorna_documentoCaricato_oggettoCorretto() {
        EmailNotificaObserver observer = new EmailNotificaObserver(emailService);

        observer.aggiorna(notificaConTipo(TipoNotifica.DOCUMENTO_CARICATO));

        verify(emailService).inviaEmail(eq("mario@test.it"),
                eq("Nuovo documento caricato"), anyString());
    }

    @Test
    @DisplayName("aggiorna DOCUMENTO_APPROVATO → oggetto 'Documento approvato'")
    void aggiorna_documentoApprovato_oggettoCorretto() {
        EmailNotificaObserver observer = new EmailNotificaObserver(emailService);

        observer.aggiorna(notificaConTipo(TipoNotifica.DOCUMENTO_APPROVATO));

        verify(emailService).inviaEmail(eq("mario@test.it"),
                eq("Documento approvato"), anyString());
    }

    @Test
    @DisplayName("aggiorna DOCUMENTO_RIFIUTATO → oggetto 'Documento rifiutato'")
    void aggiorna_documentoRifiutato_oggettoCorretto() {
        EmailNotificaObserver observer = new EmailNotificaObserver(emailService);

        observer.aggiorna(notificaConTipo(TipoNotifica.DOCUMENTO_RIFIUTATO));

        verify(emailService).inviaEmail(eq("mario@test.it"),
                eq("Documento rifiutato"), anyString());
    }

    @Test
    @DisplayName("aggiorna SCADENZA_IMMINENTE → oggetto 'Scadenza imminente'")
    void aggiorna_scadenzaImminente_oggettoCorretto() {
        EmailNotificaObserver observer = new EmailNotificaObserver(emailService);

        observer.aggiorna(notificaConTipo(TipoNotifica.SCADENZA_IMMINENTE));

        verify(emailService).inviaEmail(eq("mario@test.it"),
                eq("Scadenza imminente"), anyString());
    }

    @Test
    @DisplayName("aggiorna → il corpo HTML contiene nome destinatario e messaggio")
    void aggiorna_corpoHtml_contieneNomeEMessaggio() {
        EmailNotificaObserver observer = new EmailNotificaObserver(emailService);

        observer.aggiorna(notificaConTipo(TipoNotifica.CAMBIO_STATO));

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).inviaEmail(eq("mario@test.it"), eq("Aggiornamento stato pratica"),
                htmlCaptor.capture());
        assertThat(htmlCaptor.getValue())
                .contains("Mario")
                .contains("Messaggio di prova");
    }
}
