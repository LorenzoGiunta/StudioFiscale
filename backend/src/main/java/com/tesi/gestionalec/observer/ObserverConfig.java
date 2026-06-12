package com.tesi.gestionalec.observer;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione che compone il pattern Observer all'avvio dell'applicazione.
 *
 * Una volta che il contesto ha istanziato il soggetto e i singoli observer,
 * questa classe li collega tra loro: è l'unico punto in cui si decide quali
 * canali sono attivi, per cui abilitarne o disabilitarne uno richiede di
 * intervenire solo qui, senza toccare la logica che emette le notifiche.
 */
@Configuration
@RequiredArgsConstructor
public class ObserverConfig {

    private final GestoreNotifiche gestN;
    private final DatabaseNotificaObserver DBN;
    private final EmailNotificaObserver email;

    @PostConstruct
    public void registraObserver (){
        gestN.aggiungiObeserver(DBN);
        gestN.aggiungiObeserver(email);
    }
}