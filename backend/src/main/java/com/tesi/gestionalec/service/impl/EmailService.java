package com.tesi.gestionalec.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servizio per l'invio delle email transazionali.
 *
 * Compone e spedisce messaggi in formato HTML in modo asincrono, così da non
 * incidere sui tempi di risposta delle operazioni che li generano. L'invio è
 * disattivabile via configurazione (utile in dimostrazione o in ambienti privi
 * di server SMTP) e gli errori di spedizione vengono registrati senza
 * propagarsi, lasciando intatto il resto del flusso, comprese le notifiche
 * applicative.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Consente di disabilitare l'invio mantenendo operativo il resto del flusso
    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Async("emailExecutor")
    public void inviaEmail(String destinatario, String oggetto, String testo) {
        if (!emailEnabled) {
            log.info("Invio email disabilitato — destinatario {}, oggetto '{}'", destinatario, oggetto);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(oggetto);
            helper.setText(testo, true); // corpo in formato HTML

            mailSender.send(message);
            log.info("Email inviata a {}", destinatario);
        } catch (Exception e) {
            // Errore non bloccante: viene registrato ma non propagato, così la
            // notifica applicativa resta comunque persistita.
            log.warn("Invio email a {} fallito: {}", destinatario, e.getMessage());
        }
    }
}
