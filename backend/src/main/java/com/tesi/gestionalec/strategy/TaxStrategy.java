package com.tesi.gestionalec.strategy;

/**
 * Astrazione dell'algoritmo di calcolo delle imposte nel pattern Strategy.
 *
 * Ogni regime fiscale fornisce una propria implementazione del calcolo,
 * intercambiabile a runtime in funzione del regime del contribuente. Il
 * chiamante resta indipendente dalle formule applicate: introdurre un nuovo
 * regime significa aggiungere una strategia, non modificare il codice esistente.
 */
public interface TaxStrategy {

    /** Calcola l'imposta dovuta a partire dal reddito imponibile indicato. */
    double calcola(double reddito);
}