package com.tesi.gestionalec.strategy;

import org.springframework.stereotype.Component;


/**
 * Strategia di calcolo per il regime forfettario (pattern Strategy).
 *
 * L'imponibile si ottiene applicando al reddito un coefficiente di redditività
 * e su di esso grava un'aliquota sostitutiva unica, senza scaglioni.
 */
@Component
public class RegimeForfettarioStrategy implements TaxStrategy{

    /** Coefficiente di redditività applicato al reddito lordo. */
    private static final double COEFFICIENTE = 0.67;
    /** Aliquota sostitutiva unica prevista dal regime. */
    private static final double ALIQUOTA = 0.15;

    @Override
    public double calcola(double reddito) {
        double baseImponibile = reddito * COEFFICIENTE;
        return baseImponibile * ALIQUOTA;
    }
}
