package com.tesi.gestionalec.service;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.RegimeFiscale;
import com.tesi.gestionalec.service.impl.CalcoloImposteServiceImpl;
import com.tesi.gestionalec.strategy.RegimeForfettarioStrategy;
import com.tesi.gestionalec.strategy.RegimeOrdinarioStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per CalcoloImposteServiceImpl.
 * Verifica che selezioni la strategia corretta in base al regime fiscale.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CalcoloImposteService – Unit Tests")
class CalcoloImposteServiceImplTest {

    @Mock RegimeOrdinarioStrategy ordinario;
    @Mock RegimeForfettarioStrategy forfettario;

    @InjectMocks
    CalcoloImposteServiceImpl calcoloService;

    @Test
    @DisplayName("regime ORDINARIO → usa RegimeOrdinarioStrategy")
    void calcolaPerCliente_ordinario_usaStrategiaOrdinaria() {
        Cliente cliente = new Cliente();
        cliente.setRegime(RegimeFiscale.ORDINARIO);
        cliente.setRedditoAnnuo(50000.0);

        when(ordinario.calcola(50000.0)).thenReturn(12500.0);

        double risultato = calcoloService.CalcolaPerCliente(cliente);

        assertThat(risultato).isEqualTo(12500.0);
        verify(ordinario).calcola(50000.0);
        verify(forfettario, never()).calcola(anyDouble());
    }

    @Test
    @DisplayName("regime FORFETTARIO → usa RegimeForfettarioStrategy")
    void calcolaPerCliente_forfettario_usaStrategiaForfettaria() {
        Cliente cliente = new Cliente();
        cliente.setRegime(RegimeFiscale.FORFETTARIO);
        cliente.setRedditoAnnuo(30000.0);

        when(forfettario.calcola(30000.0)).thenReturn(4500.0);

        double risultato = calcoloService.CalcolaPerCliente(cliente);

        assertThat(risultato).isEqualTo(4500.0);
        verify(forfettario).calcola(30000.0);
        verify(ordinario, never()).calcola(anyDouble());
    }
}
