package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.request.DocumentoRequest;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Pratica;

/**
 * Conversione bidirezionale tra l'entità {@link Documento} e i relativi DTO:
 * dal modello al DTO di risposta (con i nomi di cliente e revisore appiattiti) e
 * dalla richiesta al modello, lasciando al servizio i campi gestiti internamente.
 */
public class DocumentoMapper {

    // Da entità a DTO di risposta
    public static DocumentoResponse toResponse(Documento documento) {
        DocumentoResponse dto = new DocumentoResponse();
        dto.setId(documento.getId());
        dto.setNome(documento.getNome());
        dto.setTipoFile(documento.getTipoFile());
        dto.setDimensione(documento.getDimensione());
        dto.setStato(documento.getStato());
        dto.setMotivazioneRifiuto(documento.getMotivazioneRifiuto());
        dto.setVersione(documento.getVersione());
        dto.setDataCaricamento(documento.getDataCaricamento());

        dto.setNomeCliente(
                documento.getCaricatoDa().getNome() + " " + documento.getCaricatoDa().getCognome()
        );

        // Il revisore può non essere ancora assegnato
        if (documento.getRevisore() != null) {
            dto.setNomeRevisore(
                    documento.getRevisore().getNome() + " " + documento.getRevisore().getCognome()
            );
        }

        return dto;
    }

    // Da DTO di richiesta a entità
    public static Documento toModel(DocumentoRequest request, Pratica pratica, Cliente cliente) {
        Documento documento = new Documento();
        documento.setNome(request.getNome());
        documento.setTipoFile(request.getTipoFile());
        documento.setPercorsoFile(request.getPercorsoFile());
        documento.setDimensione(request.getDimensione());
        documento.setPratica(pratica);
        documento.setCaricatoDa(cliente);
        // Stato, versione e data di caricamento sono impostati dal servizio
        return documento;
    }
}