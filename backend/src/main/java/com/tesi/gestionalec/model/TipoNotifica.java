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
    ACCOUNT_CREATO
}
