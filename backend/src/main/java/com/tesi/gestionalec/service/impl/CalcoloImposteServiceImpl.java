package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.service.interfaces.CalcoloImposteService;
import com.tesi.gestionalec.strategy.RegimeForfettarioStrategy;
import com.tesi.gestionalec.strategy.RegimeOrdinarioStrategy;
import com.tesi.gestionalec.strategy.TaxStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Contesto del pattern Strategy per il calcolo delle imposte.
 *
 * Seleziona la strategia di calcolo appropriata in base al regime fiscale del
 * cliente e le delega la determinazione dell'importo dovuto. Prima di procedere
 * verifica che il profilo contenga i dati indispensabili (regime e reddito),
 * così da fornire un errore esplicito invece di un risultato privo di senso.
 */
@Service
@RequiredArgsConstructor
public class CalcoloImposteServiceImpl implements CalcoloImposteService {

    private final RegimeOrdinarioStrategy ordinario;
    private final RegimeForfettarioStrategy forfettario;

    @Override
    public double CalcolaPerCliente(Cliente cliente) {
        if (cliente.getRegime() == null) {
            throw new IllegalArgumentException(
                    "Il cliente '" + cliente.getNome() + " " + cliente.getCognome()
                            + "' non ha un regime fiscale impostato. Aggiornare il profilo prima di calcolare le imposte.");
        }
        if (cliente.getRedditoAnnuo() == null) {
            throw new IllegalArgumentException(
                    "Il cliente '" + cliente.getNome() + " " + cliente.getCognome()
                            + "' non ha un reddito annuo impostato. Aggiornare il profilo prima di calcolare le imposte.");
        }

        TaxStrategy strategia = switch (cliente.getRegime()) {
            case FORFETTARIO -> forfettario;
            case ORDINARIO -> ordinario;
        };
        return strategia.calcola(cliente.getRedditoAnnuo());
    }
}
