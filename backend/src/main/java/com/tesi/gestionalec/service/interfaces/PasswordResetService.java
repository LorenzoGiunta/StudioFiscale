package com.tesi.gestionalec.service.interfaces;

/**
 * Recupero della password tramite email.
 *
 * Flusso in due passi: l'utente chiede il reset indicando la propria email e
 * riceve un link con un token monouso a scadenza; seguendo il link sceglie la
 * nuova password, che viene salvata solo se il token è ancora valido.
 */
public interface PasswordResetService {

    /**
     * Genera un token e invia il link di reimpostazione all'indirizzo indicato.
     * Non segnala in alcun modo se l'email è registrata o no: l'esito per il
     * chiamante è identico nei due casi, così da non permettere di scoprire
     * quali indirizzi hanno un account.
     */
    void richiediReset(String email);

    /**
     * Imposta la nuova password dell'utente associato al token e consuma il
     * token. Fallisce se il token non esiste, è già stato usato o è scaduto.
     */
    void reimpostaPassword(String token, String nuovaPassword);
}
