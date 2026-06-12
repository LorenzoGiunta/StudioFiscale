package com.tesi.gestionalec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Dati per la registrazione di un documento e il collegamento alla pratica e all'autore del caricamento. */
@Data
public class DocumentoRequest {
    @NotBlank(message = "Il nome del documento è obbligatorio")
    private String nome;

    @NotBlank(message = "Il tipo di file è obbligatorio")
    private String tipoFile;

    @NotBlank(message = "Il percorso del file è obbligatorio")
    private String percorsoFile;

    @NotNull(message = "La dimensione del file è obbligatoria")
    @Positive(message = "La dimensione deve essere positiva")
    private Long dimensione;

    @NotNull(message = "L'ID della pratica è obbligatorio")
    private Long praticaId;

    @NotNull(message = "L'ID di chi carica il documento è obbligatorio")
    private Long caricatoDaId;
}