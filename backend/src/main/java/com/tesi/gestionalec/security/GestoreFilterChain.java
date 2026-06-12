package com.tesi.gestionalec.security;

import com.tesi.gestionalec.config.CorsConfig;
import com.tesi.gestionalec.service.impl.UtenteServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configurazione centrale della sicurezza web.
 *
 * Definisce la catena dei filtri di sicurezza: disabilita la sessione lato
 * server in favore di un'autenticazione stateless basata su JWT, configura il
 * CORS e stabilisce le regole di accesso agli endpoint per ruolo. Inserisce
 * inoltre il filtro JWT prima di quello di autenticazione standard e registra
 * il provider che convalida le credenziali in fase di login.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class GestoreFilterChain {

    private final UtenteServiceImpl utenteServiceImpl;
    private final FilterAutenticazione filterDiAutenticazione;
    private final PasswordEncoder passwordEncoder;
    private final CorsConfig corsConfig;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(utenteServiceImpl);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // rifiuto invito accessibile senza login (link diretto da email)
                        .requestMatchers("/api/inviti/*/rifiuta").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("AMMINISTRATORE")
                        .requestMatchers("/api/commercialista/**").hasRole("COMMERCIALISTA")
                        .requestMatchers("/api/collaboratore/**").hasRole("COLLABORATORE")
                        .requestMatchers("/api/cliente/**").hasRole("CLIENTE")
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(filterDiAutenticazione,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}