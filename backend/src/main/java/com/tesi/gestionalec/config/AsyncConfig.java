package com.tesi.gestionalec.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configurazione dell'esecuzione asincrona.
 *
 * Definisce due pool di thread distinti, uno per l'invio delle email e uno per
 * le notifiche, così da elaborare questi compiti fuori dal thread della
 * richiesta e impedire che un picco di carico su un canale rallenti l'altro.
 *
 * Entrambi i pool sono "induriti" per non far mai fallire l'operazione di
 * business che li invoca: se la coda è piena il task viene eseguito dal
 * chiamante anziché rifiutato (freno naturale, ma nessuna eccezione), e allo
 * spegnimento si attende che i task in coda finiscano invece di scartarli.
 *
 * Mette inoltre a disposizione un executor di default con il nome che Spring
 * Boot si aspetta ({@code applicationTaskExecutor}): la sola presenza di pool
 * personalizzati fa infatti rinunciare Boot a crearlo, e le parti del framework
 * che ne hanno bisogno (MVC asincrono, WebSocket) ripiegherebbero su thread
 * creati senza limite.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final int ATTESA_SPEGNIMENTO_SECONDI = 30;

    // Pool dedicato alle email: isola le chiamate SMTP, tipicamente lente
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        return creaPool("email-thread-", 2, 5, 50);
    }

    // Pool dedicato alle notifiche, separato da quello delle email
    @Bean(name = "notificaExecutor")
    public Executor notificaExecutor() {
        return creaPool("notifica-thread-", 2, 10, 100);
    }

    /**
     * Executor generale, costruito con il builder di Boot così da rispettare le
     * proprietà {@code spring.task.execution.*}. È quello usato dai metodi
     * {@code @Async} senza qualificatore e dal resto del framework.
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        ThreadPoolTaskExecutor executor = builder.build();
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(ATTESA_SPEGNIMENTO_SECONDI);
        executor.initialize();
        return executor;
    }

    private ThreadPoolTaskExecutor creaPool(String prefisso, int core, int max, int coda) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(coda);
        executor.setThreadNamePrefix(prefisso);

        // Coda piena: il task lo esegue il thread chiamante. La richiesta si
        // rallenta, ma non fallisce per colpa di un arretrato di email.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Spegnimento: i task già accettati vengono completati, non persi
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(ATTESA_SPEGNIMENTO_SECONDI);

        executor.initialize();
        return executor;
    }

    // ─── AsyncConfigurer ─────────────────────────────────────────────────────
    // getAsyncExecutor() non è sovrascritto: restituendo null, Spring usa il
    // TaskExecutor marcato @Primary (applicationTaskExecutor) per i metodi
    // @Async senza qualificatore.

    /**
     * Le eccezioni sollevate dai metodi {@code @Async void} non hanno un
     * chiamante a cui risalire: qui vengono registrate in modo esplicito, con
     * il metodo che le ha prodotte, invece di affidarsi al gestore silenzioso
     * di default. È l'unico punto da toccare per aggiungere metriche o retry.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error(
                "Task asincrono fallito in {}.{}: {}",
                method.getDeclaringClass().getSimpleName(), method.getName(), ex.getMessage(), ex);
    }
}
