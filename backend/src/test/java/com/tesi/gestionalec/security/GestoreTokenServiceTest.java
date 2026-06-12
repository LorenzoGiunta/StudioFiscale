package com.tesi.gestionalec.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per GestoreTokenService (JWT).
 * Usa ReflectionTestUtils per iniettare i valori @Value senza Spring context.
 */
@DisplayName("GestoreTokenService – JWT Unit Tests")
class GestoreTokenServiceTest {

    private GestoreTokenService tokenService;

    // Il secret deve essere sufficientemente lungo per HMAC-SHA256 (min 32 char)
    private static final String TEST_SECRET =
            "siVisPacemParaBellumErrareHumanumEstPerseverareAutemDiabolicumAleaIactaEst";
    private static final long TEST_EXPIRATION = 86_400_000L; // 24h

    @BeforeEach
    void setUp() {
        tokenService = new GestoreTokenService();
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenService, "expiration", TEST_EXPIRATION);
    }

    // ─── generaToken ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("generaToken produce un JWT non nullo e non vuoto")
    void generaToken_produceJwtNonVuoto() {
        String token = tokenService.generaToken("test@example.com");

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generaToken produce un JWT con tre parti separate da punto")
    void generaToken_formaCorrettaJwt() {
        String token = tokenService.generaToken("test@example.com");

        // Un JWT è sempre formato da 3 parti: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ─── estraiEmail ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("estraiEmail restituisce l'email originale dal token")
    void estraiEmail_emailCorretta() {
        String email = "mario.rossi@studio.it";
        String token = tokenService.generaToken(email);

        String estratta = tokenService.estraiEmail(token);

        assertThat(estratta).isEqualTo(email);
    }

    // ─── isTokenValido ────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValido: token valido con email corretta → true")
    void isTokenValido_tokenValidoEmailCorretta_returnsTrue() {
        String email = "mario.rossi@studio.it";
        String token = tokenService.generaToken(email);
        UserDetails userDetails = userDetailsOf(email);

        assertThat(tokenService.isTokenValido(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValido: email diversa nello UserDetails → false")
    void isTokenValido_emailDiversa_returnsFalse() {
        String token = tokenService.generaToken("alpha@studio.it");
        UserDetails altroUtente = userDetailsOf("beta@studio.it");

        assertThat(tokenService.isTokenValido(token, altroUtente)).isFalse();
    }

    @Test
    @DisplayName("isTokenValido: token scaduto → false")
    void isTokenValido_tokenScaduto_returnsFalse() throws Exception {
        // Imposta expiration negativa → token già scaduto in fase di generazione
        ReflectionTestUtils.setField(tokenService, "expiration", -1L);
        String email = "test@studio.it";
        String tokenScaduto = tokenService.generaToken(email);
        UserDetails userDetails = userDetailsOf(email);

        assertThat(tokenService.isTokenValido(tokenScaduto, userDetails)).isFalse();
    }

    @Test
    @DisplayName("isTokenValido: token con firma alterata → false")
    void isTokenValido_firmaAlterata_returnsFalse() {
        String token = tokenService.generaToken("test@studio.it");
        UserDetails userDetails = userDetailsOf("test@studio.it");

        // Altera l'ultimo carattere della firma (terza parte del JWT)
        String[] parti = token.split("\\.");
        String tokenManomesso = parti[0] + "." + parti[1] + "." + parti[2] + "X";

        assertThat(tokenService.isTokenValido(tokenManomesso, userDetails)).isFalse();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private UserDetails userDetailsOf(String email) {
        return User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }
}
