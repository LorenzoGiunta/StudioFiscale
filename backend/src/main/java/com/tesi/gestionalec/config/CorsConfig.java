package com.tesi.gestionalec.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configurazione della politica CORS.
 *
 * Abilita le richieste provenienti dalle origini del frontend, specificando
 * metodi, header e modalità di gestione delle credenziali ammessi, così da
 * consentire il dialogo tra client e API serviti su porte diverse. Le origini
 * ammesse arrivano dalla configurazione ({@code app.cors.allowed-origins}),
 * così cambiano tra sviluppo, container e produzione senza toccare il codice.
 */
@Configuration
public class CorsConfig {

    // Origini del frontend ammesse, separate da virgola nella property
    @Value("${app.cors.allowed-origins}")
    private List<String> originiAmmesse;

    /** Origini ammesse, condivise con l'handshake WebSocket. */
    public List<String> getOriginiAmmesse() {
        return originiAmmesse;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(originiAmmesse);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
