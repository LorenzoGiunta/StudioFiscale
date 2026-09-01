package com.tesi.gestionalec.state;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * Traduce lo stato di una pratica tra oggetto e colonna del database.
 *
 * È l'unico punto del progetto che conosce l'elenco degli stati concreti: il
 * dominio lavora esclusivamente con gli oggetti {@link StatoPraticaState},
 * mentre la corrispondenza con il codice testuale salvato a database resta
 * confinata qui, nello strato di persistenza. La ricostruzione avviene per
 * ricerca sull'elenco degli stati, senza alcun costrutto condizionale da
 * aggiornare all'aggiunta di un nuovo stato.
 */
@Converter
public class StatoPraticaConverter implements AttributeConverter<StatoPraticaState, String> {

    private static final List<StatoPraticaState> STATI = List.of(
            BozzaState.INSTANCE,
            InLavorazioneState.INSTANCE,
            InAttesaDocumentiState.INSTANCE,
            CompletataState.INSTANCE);

    @Override
    public String convertToDatabaseColumn(StatoPraticaState stato) {
        return stato == null ? null : stato.getCodice();
    }

    @Override
    public StatoPraticaState convertToEntityAttribute(String codice) {
        if (codice == null) {
            return null;
        }
        return daCodice(codice);
    }

    /** Risolve lo stato corrispondente al codice indicato. */
    public static StatoPraticaState daCodice(String codice) {
        return STATI.stream()
                .filter(stato -> stato.getCodice().equals(codice))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Stato pratica sconosciuto: " + codice));
    }
}
