package com.tesi.gestionalec.security;


import com.tesi.gestionalec.service.impl.UtenteServiceImpl;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro di autenticazione eseguito una sola volta per richiesta HTTP.
 *
 * Estrae il token JWT dall'header {@code Authorization}, ne verifica la validità
 * e, se corretto, popola il contesto di sicurezza con l'utente autenticato. In
 * assenza di token o in caso di token non valido la richiesta prosegue senza
 * autenticazione, lasciando a Spring Security la decisione finale sull'accesso.
 */
@Component
@RequiredArgsConstructor
public class FilterAutenticazione extends OncePerRequestFilter {


    private final GestoreTokenService gestoreTokenService;
    private final UtenteServiceImpl utenteServiceImpl;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Un token malformato o con firma non valida solleva una JwtException.
        // Trovandosi in un filtro servlet e non in un controller, l'eccezione
        // sfuggirebbe al gestore globale, producendo un errore non strutturato.
        // Intercettandola qui la catena prosegue senza autenticare, lasciando
        // che Spring Security risponda con un 401 nel formato previsto.
        String email;
        try {
            email = gestoreTokenService.estraiEmail(token);
        } catch (JwtException e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = utenteServiceImpl.loadUserByUsername(email);

            if (gestoreTokenService.isTokenValido(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
