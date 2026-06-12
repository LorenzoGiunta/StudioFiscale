package com.tesi.gestionalec.dto.response;

import com.tesi.gestionalec.model.StatoDocumento;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO di risposta con i metadati di un documento e i nomi di cliente e revisore.
 */
@Data
public class DocumentoResponse {
    private Long id;
    private String nome;
    private String tipoFile;
    private Long dimensione;
    private StatoDocumento stato;
    private String motivazioneRifiuto;
    private Integer versione;
    private LocalDateTime dataCaricamento;
    private String nomeCliente;
    private String nomeRevisore;    // assente finché non viene assegnato un revisore
}
