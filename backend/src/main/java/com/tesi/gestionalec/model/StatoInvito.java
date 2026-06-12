package com.tesi.gestionalec.model;

/** Stati possibili di un invito di collaborazione lungo il suo ciclo di vita. */
public enum StatoInvito {
    PENDING,   // inviato, in attesa di risposta del collaboratore
    ACCEPTED,  // accettato: la collaborazione è attiva
    DECLINED,  // rifiutato dal collaboratore
    EXPIRED    // scaduto automaticamente per superamento del termine
}
