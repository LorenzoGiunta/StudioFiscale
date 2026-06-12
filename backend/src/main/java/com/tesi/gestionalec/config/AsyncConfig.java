package com.tesi.gestionalec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configurazione dell'esecuzione asincrona.
 *
 * Definisce due pool di thread distinti, uno per l'invio delle email e uno per
 * le notifiche, così da elaborare questi compiti fuori dal thread della
 * richiesta e impedire che un picco di carico su un canale rallenti l'altro.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    // Pool dedicato alle email: isola le chiamate SMTP, tipicamente lente
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-thread-");
        executor.initialize();
        return executor;
    }

    // Pool dedicato alle notifiche, separato da quello delle email
    @Bean(name = "notificaExecutor")
    public Executor notificaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notifica-thread-");
        executor.initialize();
        return executor;
    }
}
