package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.dto.response.StatisticheResponse;
import com.tesi.gestionalec.model.Utente;

/**
 * Contratto specifico per l'amministratore: tracciamento dell'attività
 * amministrativa e produzione delle statistiche di sistema, in aggiunta alle
 * funzionalità comuni ereditate dal contratto utente.
 */
public interface AmministratoreService extends UtenteService{
    void aggiornaUltimaAzione(Long amministratoreId);
    StatisticheResponse calcolaStatistiche();

    /**
     * Operazioni amministrative sul ciclo di vita degli account svolte da un
     * amministratore identificato ({@code richiedente}). Rispetto alle varianti
     * ereditate, applicano i controlli di sicurezza propri dell'area di
     * amministrazione (per le operazioni distruttive) e tracciano l'istante
     * dell'ultima azione di chi le compie.
     */
    void abilitaUtente(Long id, Utente richiedente);
    void disabilitaUtente(Long id, Utente richiedente);
    void eliminaUtente(Long id, Utente richiedente);
    void ripristinaUtente(Long id, Utente richiedente);
}
