package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.request.PraticaRequest;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.Cliente;


/**
 * Conversione bidirezionale tra l'entità {@link Pratica} e i relativi DTO: dal
 * modello al DTO di risposta (esponendo i soli nomi di cliente e collaboratore) e
 * dalla richiesta al modello, demandando al servizio i campi gestiti internamente.
 */
public class PraticaMapper {
    // Da entità a DTO di risposta
    public static PraticaResponse toResponse(Pratica pratica) {
        PraticaResponse dto = new PraticaResponse();
        dto.setId(pratica.getId());
        dto.setTipoPratica(pratica.getTipoPratica());
        dto.setStato(pratica.getStato());
        dto.setDataCreazione(pratica.getDataCreazione());
        dto.setScadenza(pratica.getScadenza());

        // Si espone il solo nome del cliente, non l'intera entità
        dto.setNomeCliente(
                pratica.getCliente().getNome() + " " + pratica.getCliente().getCognome()
        );

        // Il collaboratore può non essere ancora assegnato
        if (pratica.getAssegnataA() != null) {
            dto.setNomeCollaboratore(
                    pratica.getAssegnataA().getNome() + " " + pratica.getAssegnataA().getCognome()
            );
        }

        return dto;
    }

    // Da DTO di richiesta a entità
    public static Pratica toModel(PraticaRequest request, Cliente cliente) {
        Pratica pratica = new Pratica();
        pratica.setCliente(cliente);
        pratica.setTipoPratica(request.getTipoPratica());
        pratica.setScadenza(request.getScadenza());
        // Stato e data di creazione sono impostati dal servizio o dalla persistenza
        return pratica;
    }
}
