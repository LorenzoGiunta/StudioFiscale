package com.tesi.gestionalec.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitari per RateLimitFilter: soglie per rotta, isolamento per IP,
 * finestra scorrevole, rotte non coperte lasciate passare.
 */
@DisplayName("RateLimitFilter – Unit Tests")
class RateLimitFilterTest {

    /** Orologio manuale: i test spostano il tempo senza attendere davvero. */
    static class OrologioManuale extends Clock {
        Instant adesso = Instant.parse("2026-01-01T10:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return adesso; }
        void avanza(Duration d) { adesso = adesso.plus(d); }
    }

    private OrologioManuale orologio;
    private RateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        orologio = new OrologioManuale();
        filtro = new RateLimitFilter(orologio);
    }

    private MockHttpServletResponse invia(String uri, String ip, String xff) throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRequestURI(uri);
        req.setRemoteAddr(ip);
        if (xff != null) req.addHeader("X-Forwarded-For", xff);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    @DisplayName("login: 10 tentativi passano, l'11° dallo stesso IP riceve 429 con Retry-After")
    void login_oltreSoglia_429() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(invia("/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse bloccata = invia("/api/auth/login", "10.0.0.1", null);

        assertThat(bloccata.getStatus()).isEqualTo(429);
        assertThat(bloccata.getHeader("Retry-After")).isNotBlank();
        assertThat(bloccata.getContentAsString()).contains("\"status\":429").contains("Troppi tentativi");
    }

    @Test
    @DisplayName("gli IP sono indipendenti: il blocco di uno non tocca l'altro")
    void ipDiversi_indipendenti() throws Exception {
        for (int i = 0; i < 10; i++) invia("/api/auth/login", "10.0.0.1", null);

        assertThat(invia("/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(429);
        assertThat(invia("/api/auth/login", "10.0.0.2", null).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("finestra scorrevole: passati 15 minuti i tentativi tornano disponibili")
    void finestraScorrevole_sblocca() throws Exception {
        for (int i = 0; i < 10; i++) invia("/api/auth/login", "10.0.0.1", null);
        assertThat(invia("/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(429);

        orologio.avanza(Duration.ofMinutes(15).plusSeconds(1));

        assertThat(invia("/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("reset password: soglia più stretta (5 in un'ora), separata da quella del login")
    void resetPassword_sogliaSeparata() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(invia("/api/auth/password/richiedi-reset", "10.0.0.1", null).getStatus()).isEqualTo(200);
        }
        assertThat(invia("/api/auth/password/richiedi-reset", "10.0.0.1", null).getStatus()).isEqualTo(429);
        // il login dello stesso IP non è influenzato
        assertThat(invia("/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("dietro un proxy conta il primo IP di X-Forwarded-For, non quello del proxy")
    void xForwardedFor_usatoComeIp() throws Exception {
        for (int i = 0; i < 10; i++) invia("/api/auth/login", "172.18.0.2", "203.0.113.7, 172.18.0.2");

        assertThat(invia("/api/auth/login", "172.18.0.2", "203.0.113.7, 172.18.0.2").getStatus()).isEqualTo(429);
        // altro client reale, stesso proxy: passa
        assertThat(invia("/api/auth/login", "172.18.0.2", "203.0.113.8, 172.18.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("rotte non coperte e metodi diversi da POST non sono limitati")
    void altreRotte_nonLimitate() throws Exception {
        for (int i = 0; i < 30; i++) {
            assertThat(invia("/api/auth/registra", "10.0.0.1", null).getStatus()).isEqualTo(200);
        }
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/auth/login");
        get.setRequestURI("/api/auth/login");
        get.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        for (int i = 0; i < 30; i++) filtro.doFilter(get, res, new MockFilterChain());
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("pulizia: le chiavi senza tentativi nell'ultima ora vengono scartate, le altre restano")
    void pulizia_chiaviStantie() throws Exception {
        invia("/api/auth/login", "10.0.0.1", null);           // vecchio
        orologio.avanza(Duration.ofMinutes(61));
        invia("/api/auth/login", "10.0.0.2", null);           // recente
        assertThat(filtro.chiaviTracciate()).isEqualTo(2);

        filtro.pulisciChiaviStantie();

        assertThat(filtro.chiaviTracciate()).isEqualTo(1);
    }
}
