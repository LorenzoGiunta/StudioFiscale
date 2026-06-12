package com.tesi.gestionalec.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tesi.gestionalec.model.RegimeFiscale;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO di risposta con anagrafica e dati fiscali di un cliente, impiegato dal
 * commercialista per la visualizzazione dei clienti e il calcolo delle imposte.
 */
@Data
@NoArgsConstructor
public class ClienteResponse {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private boolean enabled;
    private String codFiscale;
    // La chiave JSON resta "pIVA" (attesa dal frontend); il campo Java è
    // partitaIva perché, lasciandolo pIVA, Jackson deriverebbe dal getter
    // getPIVA() la chiave "piva", disallineandola dal client.
    @JsonProperty("pIVA")
    private String partitaIva;
    private RegimeFiscale regime;
    private Double redditoAnnuo;
    private Long commercialistaId;
    private String nomeCommercialista;
}
