package com.tesi.gestionalec.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita per indirizzo IP i tentativi sugli endpoint pubblici più delicati:
 * il login (forza bruta sulle password) e la richiesta di recupero password
 * (bombardamento di email verso una vittima). Oltre la soglia risponde
 * 429 Too Many Requests con l'intestazione {@code Retry-After}.
 *
 * Implementazione a finestra scorrevole in memoria, sufficiente per un
 * singolo nodo: con più istanze occorrerebbe uno store condiviso (es. Redis).
 * Dietro il reverse proxy l'IP reale è letto da {@code X-Forwarded-For}.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Regola: quante richieste in quale finestra, per una data rotta. */
    record Regola(String percorso, int massimo, Duration finestra) {}

    static final Map<String, Regola> REGOLE = Map.of(
            "/api/auth/login",                   new Regola("login", 10, Duration.ofMinutes(15)),
            "/api/auth/password/richiedi-reset", new Regola("reset",  5, Duration.ofHours(1)));

    /** Oltre questo numero di chiavi tracciate si scartano quelle senza tentativi recenti. */
    static final int SOGLIA_PULIZIA = 1000;

    private static final Duration FINESTRA_MASSIMA = REGOLE.values().stream()
            .map(Regola::finestra).max(Duration::compareTo).orElse(Duration.ofHours(1));

    private final Clock clock;
    private final Map<String, Deque<Long>> tentativi = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        this(Clock.systemUTC());
    }

    /** Costruttore con orologio iniettabile, per i test. */
    RateLimitFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !REGOLE.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Regola regola = REGOLE.get(request.getRequestURI());
        String chiave = regola.percorso() + ":" + ipClient(request);

        if (tentativi.size() > SOGLIA_PULIZIA) {
            pulisciChiaviStantie();
        }

        long attesaSecondi = registraTentativo(chiave, regola);
        if (attesaSecondi > 0) {
            log.warn("[RATE-LIMIT] Troppi tentativi su {} da {}", request.getRequestURI(), ipClient(request));
            rispondi429(response, request.getRequestURI(), attesaSecondi);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Registra il tentativo e restituisce 0 se ammesso, altrimenti i secondi da
     * attendere prima che il più vecchio tentativo esca dalla finestra.
     */
    private long registraTentativo(String chiave, Regola regola) {
        long adesso = clock.millis();
        long inizioFinestra = adesso - regola.finestra().toMillis();

        Deque<Long> coda = tentativi.computeIfAbsent(chiave, k -> new ArrayDeque<>());
        synchronized (coda) {
            while (!coda.isEmpty() && coda.peekFirst() < inizioFinestra) {
                coda.pollFirst();   // scarta i tentativi usciti dalla finestra
            }
            if (coda.size() >= regola.massimo()) {
                return Math.max(1, (coda.peekFirst() + regola.finestra().toMillis() - adesso) / 1000);
            }
            coda.addLast(adesso);
            return 0;
        }
    }

    // Dietro nginx/proxy l'IP del client è il primo di X-Forwarded-For
    private static String ipClient(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Stesso formato JSON di ApiError, scritto a mano: il filtro sta fuori da MVC
    private static void rispondi429(HttpServletResponse response, String path, long attesaSecondi) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(attesaSecondi));
        response.getWriter().write("""
                {"timestamp":"%s","status":429,"error":"Troppi tentativi",\
                "message":"Hai fatto troppe richieste in poco tempo. Riprova tra %d secondi.","path":"%s"}"""
                .formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                        attesaSecondi, path));
    }

    /**
     * Rimuove le chiavi il cui ultimo tentativo è più vecchio della finestra più
     * lunga: non possono più influenzare nessuna decisione, occupano solo memoria.
     */
    void pulisciChiaviStantie() {
        long limite = clock.millis() - FINESTRA_MASSIMA.toMillis();
        tentativi.entrySet().removeIf(e -> {
            Deque<Long> coda = e.getValue();
            synchronized (coda) {
                return coda.isEmpty() || coda.peekLast() < limite;
            }
        });
    }

    /** Numero di chiavi (IP × rotta) attualmente tracciate; per test e diagnostica. */
    int chiaviTracciate() {
        return tentativi.size();
    }
}
