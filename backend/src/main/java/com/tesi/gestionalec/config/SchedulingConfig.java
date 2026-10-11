package com.tesi.gestionalec.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Scheduler dedicato ai job {@code @Scheduled} (scadenze fiscali, inviti,
 * token di reset).
 *
 * Senza questa classe i job finirebbero sul {@code messageBrokerTaskScheduler}
 * del WebSocket: la configurazione della chat registra infatti un bean
 * {@code TaskScheduler}, il che fa rinunciare Spring Boot a crearne uno
 * proprio, e Spring assegna i job all'unico scheduler che trova. I job
 * condividerebbero così i thread con gli heartbeat STOMP, e le proprietà
 * {@code spring.task.scheduling.*} resterebbero senza effetto.
 *
 * Il pool è costruito con il builder di Boot, quindi dimensione, prefisso dei
 * thread e attesa allo spegnimento si regolano da {@code application.properties}.
 */
@Configuration
@RequiredArgsConstructor
public class SchedulingConfig implements SchedulingConfigurer {

    private final ThreadPoolTaskSchedulerBuilder builder;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        ThreadPoolTaskScheduler scheduler = builder.build();
        scheduler.initialize();
        registrar.setTaskScheduler(scheduler);
    }
}
