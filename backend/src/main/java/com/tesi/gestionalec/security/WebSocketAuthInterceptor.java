package com.tesi.gestionalec.security;

import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.impl.UtenteServiceImpl;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Autentica le connessioni STOMP/WebSocket.
 * <p>
 * Sul frame CONNECT legge l'header nativo {@code Authorization: Bearer <jwt>}
 * (inviato dal client negli connectHeaders), valida il token con la stessa logica
 * del filtro HTTP e, se valido, associa alla sessione un {@link UtentePrincipal}.
 * Tutti i frame successivi (SUBSCRIBE/SEND) della stessa sessione erediteranno
 * automaticamente questo principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final GestoreTokenService gestoreTokenService;
    private final UtenteServiceImpl utenteServiceImpl;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // L'autenticazione avviene solo sul frame CONNECT: il principal così
        // stabilito viene poi propagato a tutti i frame della stessa sessione.
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingException("WebSocket: token di autenticazione mancante");
        }

        String token = authHeader.substring(7);
        String email;
        try {
            email = gestoreTokenService.estraiEmail(token);
        } catch (JwtException e) {
            throw new MessagingException("WebSocket: token non valido");
        }

        final UserDetails userDetails;
        try {
            userDetails = utenteServiceImpl.loadUserByUsername(email);
        } catch (UsernameNotFoundException e) {
            throw new MessagingException("WebSocket: utente non trovato");
        }

        if (!gestoreTokenService.isTokenValido(token, userDetails)) {
            throw new MessagingException("WebSocket: token non valido o scaduto");
        }

        accessor.setUser(new UtentePrincipal((Utente) userDetails));
        log.debug("WebSocket: sessione autenticata per utente {}", email);

        return message;
    }
}
