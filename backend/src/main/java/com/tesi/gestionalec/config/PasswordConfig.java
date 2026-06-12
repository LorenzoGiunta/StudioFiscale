package com.tesi.gestionalec.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configurazione dell'algoritmo di cifratura delle password.
 *
 * Espone come bean l'encoder basato su BCrypt, utilizzato per memorizzare le
 * password in forma di hash e per verificarle in fase di autenticazione.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}