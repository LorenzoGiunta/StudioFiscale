package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.ClienteRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari per ClienteServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService – Unit Tests")
class ClienteServiceImplTest {

    @Mock UtenteRepo utenteRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ClienteRepo clienteRepo;
    @Mock CommercialistaRepo commercialistaRepo;

    ClienteServiceImpl clienteService;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteServiceImpl(utenteRepo, passwordEncoder, clienteRepo, commercialistaRepo);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Mario");
        cliente.setCognome("Rossi");
        cliente.setEmail("mario@test.it");
        cliente.setCodFiscale("RSSMRO80A01H501Z");
        cliente.setRegime(RegimeFiscale.ORDINARIO);
        cliente.setRedditoAnnuo(50000.0);

        Pratica p = new Pratica();
        p.setId(10L);
        cliente.setPratiche(new java.util.ArrayList<>(List.of(p)));

        Documento d = new Documento();
        d.setId(20L);
        cliente.setDocumenti(new java.util.ArrayList<>(List.of(d)));
    }

    // ─── trovaPerCodFiscale ───────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerCodFiscale: restituisce cliente se esiste")
    void trovaPerCodFiscale_esistente_restituisceCliente() {
        when(clienteRepo.findByCodFiscale("RSSMRO80A01H501Z")).thenReturn(Optional.of(cliente));

        Cliente trovato = clienteService.trovaPerCodFiscale("RSSMRO80A01H501Z");

        assertThat(trovato.getCodFiscale()).isEqualTo("RSSMRO80A01H501Z");
    }

    @Test
    @DisplayName("trovaPerCodFiscale: cod. fiscale non trovato → ResourceNotFoundException")
    void trovaPerCodFiscale_nonEsistente_lanciaEccezione() {
        when(clienteRepo.findByCodFiscale("INESISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.trovaPerCodFiscale("INESISTENTE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INESISTENTE");
    }

    // ─── trovaPratiche ────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPratiche: restituisce le pratiche del cliente")
    void trovaPratiche_restituisceLista() {
        when(clienteRepo.findByIdConCommercialista(1L)).thenReturn(Optional.of(cliente));

        List<Pratica> pratiche = clienteService.trovaPratiche(1L);

        assertThat(pratiche).hasSize(1);
        assertThat(pratiche.get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("trovaPratiche: cliente non trovato → ResourceNotFoundException")
    void trovaPratiche_clienteNonTrovato_lanciaEccezione() {
        when(clienteRepo.findByIdConCommercialista(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.trovaPratiche(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── trovaDocumenti ───────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaDocumenti: restituisce i documenti del cliente")
    void trovaDocumenti_restituisceLista() {
        when(clienteRepo.findByIdConCommercialista(1L)).thenReturn(Optional.of(cliente));

        List<Documento> documenti = clienteService.trovaDocumenti(1L);

        assertThat(documenti).hasSize(1);
        assertThat(documenti.get(0).getId()).isEqualTo(20L);
    }

    // ─── aggiorna ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("aggiorna: aggiorna tutti i campi e salva")
    void aggiorna_aggiornaEсалва() {
        Cliente dati = new Cliente();
        dati.setNome("Giuseppe");
        dati.setCognome("Verdi");
        dati.setEmail("giuseppe@test.it");
        dati.setCodFiscale("VRDGPP75M01H501K");
        dati.setPIVA("12345678901");
        dati.setRegime(RegimeFiscale.FORFETTARIO);
        dati.setRedditoAnnuo(30000.0);

        when(clienteRepo.findByIdConCommercialista(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Cliente aggiornato = clienteService.aggiorna(1L, dati);

        assertThat(aggiornato.getNome()).isEqualTo("Giuseppe");
        assertThat(aggiornato.getRegime()).isEqualTo(RegimeFiscale.FORFETTARIO);
        verify(clienteRepo).save(cliente);
    }

    @Test
    @DisplayName("aggiorna: cliente non trovato → ResourceNotFoundException")
    void aggiorna_nonTrovato_lanciaEccezione() {
        when(clienteRepo.findByIdConCommercialista(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.aggiorna(99L, new Cliente()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
