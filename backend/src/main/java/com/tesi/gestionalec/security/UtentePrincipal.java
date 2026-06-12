package com.tesi.gestionalec.security;

import com.tesi.gestionalec.model.Utente;

import java.security.Principal;

/**
 * Principal associato alle sessioni WebSocket/STOMP.
 * <p>
 * Il {@link #getName()} espone l'identificativo dell'utente come stringa, che
 * costituisce la chiave con cui il broker instrada i messaggi diretti a un
 * singolo destinatario: il nome del principal coincide quindi con l'id e non
 * con l'email, coerentemente con l'invio effettuato dal controller di chat.
 * <p>
 * Conserva inoltre il riferimento all'entità {@link Utente}, così da rendere
 * disponibile il mittente completo a partire dal {@link Principal} iniettato.
 */
public record UtentePrincipal(Utente utente) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(utente.getId());
    }
}
