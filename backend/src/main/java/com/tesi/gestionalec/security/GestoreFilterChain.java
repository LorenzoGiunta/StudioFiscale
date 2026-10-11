package com.tesi.gestionalec.security;

import com.tesi.gestionalec.config.CorsConfig;
import com.tesi.gestionalec.service.impl.UtenteServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
    private final RateLimitFilter rateLimitFilter;
    private final PasswordEncoder passwordEncoder;
    private final CorsConfig corsConfig;

    /*
     * I filtri JWT e rate-limit sono @Component: Spring Boot li registrerebbe
     * anche come filtri servlet generici, facendoli girare una volta FUORI dalla
     * security chain e una dentro. Oltre allo spreco, il contesto di sicurezza
     * impostato "fuori" interferisce con la gestione degli errori di
     * autorizzazione dentro la chain. Qui si disattiva la registrazione
     * automatica: i filtri vivono solo dove sono aggiunti sotto, nell'ordine
     * voluto.
     */
    @Bean
    public FilterRegistrationBean<FilterAutenticazione> registrazioneFiltroJwt(FilterAutenticazione filtro) {
        FilterRegistrationBean<FilterAutenticazione> registrazione = new FilterRegistrationBean<>(filtro);
        registrazione.setEnabled(false);
        return registrazione;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> registrazioneRateLimit(RateLimitFilter filtro) {
        FilterRegistrationBean<RateLimitFilter> registrazione = new FilterRegistrationBean<>(filtro);
        registrazione.setEnabled(false);
        return registrazione;
    }

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
                // Richiesta senza autenticazione valida (token assente, scaduto,
                // di un account disabilitato o eliminato) → 401, non il 403 di
                // default: è ciò che il frontend intercetta per tornare al login.
                // Il 403 resta per chi è autenticato ma non autorizzato.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // Autenticato ma senza il ruolo richiesto dalla regola URL → 403
                        // scritto direttamente. Il gestore di default usa sendError(),
                        // che dirotta su /error: senza permesso su quella rotta il 403
                        // verrebbe rimpiazzato da un 401 fuorviante.
                        .accessDeniedHandler((request, response, denied) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Accesso negato\","
                                    + "\"message\":\"Non hai i permessi per accedere a questa risorsa.\","
                                    + "\"path\":\"" + request.getRequestURI() + "\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // Pagina d'errore di Spring Boot: raggiunta via dispatch interno
                        // (es. eccezione non gestita); senza permesso risponderebbe 401
                        .requestMatchers("/error").permitAll()
                        // rifiuto invito accessibile senza login (link diretto da email)
                        .requestMatchers("/api/inviti/*/rifiuta").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("AMMINISTRATORE")
                        .requestMatchers("/api/commercialista/**").hasRole("COMMERCIALISTA")
                        .requestMatchers("/api/collaboratore/**").hasRole("COLLABORATORE")
                        .requestMatchers("/api/cliente/**").hasRole("CLIENTE")
                        .anyRequest().authenticated()
                )

                .authenticationProvider(authenticationProvider())
                // Prima il limite sui tentativi (login, reset password), poi il JWT
                .addFilterBefore(rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filterDiAutenticazione,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}