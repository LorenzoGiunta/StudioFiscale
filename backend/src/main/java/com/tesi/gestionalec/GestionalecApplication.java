package com.tesi.gestionalec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto di ingresso dell'applicazione.
 *
 * Avvia il contesto Spring Boot e abilita l'esecuzione dei processi schedulati
 * (controllo delle scadenze e scadenza automatica degli inviti).
 */
@SpringBootApplication
@EnableScheduling
public class GestionalecApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionalecApplication.class, args);
    }

}
