package com.tesi.gestionalec.dto.request;

import com.tesi.gestionalec.model.TipoPratica;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Dati necessari alla creazione di una pratica: cliente, tipo e scadenza. */
@Data
public class PraticaRequest {
    @NotNull(message = "L'ID del cliente è obbligatorio")
    private Long clienteId;

    @NotNull(message = "Il tipo di pratica è obbligatorio")
    private TipoPratica tipoPratica;

    @NotNull(message = "La scadenza è obbligatoria")
    @FutureOrPresent(message = "La scadenza non può essere nel passato")
    private LocalDate scadenza;
}
