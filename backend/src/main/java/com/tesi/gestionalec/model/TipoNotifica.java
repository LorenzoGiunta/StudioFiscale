package com.tesi.gestionalec.model;

/** Categorie di evento che possono dare luogo a una notifica. */
public enum TipoNotifica {
    CAMBIO_STATO,
    DOCUMENTO_CARICATO,
    DOCUMENTO_APPROVATO,
    DOCUMENTO_RIFIUTATO,
    SCADENZA_IMMINENTE,
    INVITO_COLLABORAZIONE,
    ACCOUNT_DISABILITATO,
    ACCOUNT_CREATO,
    RICHIESTA_ISCRIZIONE,   // all'amministratore: un commercialista attende la verifica dell'albo
    ACCOUNT_APPROVATO,      // al richiedente: iscrizione approvata, può accedere
    ACCOUNT_RIFIUTATO,      // al richiedente: iscrizione rifiutata
    PASSWORD_MODIFICATA     // conferma di reimpostazione della password
}
