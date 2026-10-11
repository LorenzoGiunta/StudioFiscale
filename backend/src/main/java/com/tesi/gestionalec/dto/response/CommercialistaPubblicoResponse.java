package com.tesi.gestionalec.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Vista pubblica di un commercialista, usata dal form di registrazione del
 * cliente per scegliere il professionista. Espone solo ciò che serve a
 * riconoscerlo: l'endpoint che la restituisce non richiede autenticazione.
 */
@Data
@AllArgsConstructor
public class CommercialistaPubblicoResponse {
    private Long id;
    private String nome;
    private String cognome;
}
