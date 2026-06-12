package com.tesi.gestionalec.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Servizio per la generazione e la verifica dei token JWT.
 *
 * Emette token firmati che identificano l'utente tramite la propria email e ne
 * controlla la validità confrontando il soggetto con l'utente atteso e
 * verificandone la scadenza. Chiave segreta e durata sono parametri esterni di
 * configurazione, così da non figurare nel codice sorgente.
 */
@Component
public class GestoreTokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generaToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getChiave(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String estraiEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getChiave())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValido(String token, UserDetails userDetails) {
        try {
            String email = estraiEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenScaduto(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenScaduto(String token) {
        Date scadenza = Jwts.parserBuilder()
                .setSigningKey(getChiave())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
        return scadenza.before(new Date());
    }

    private Key getChiave() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}