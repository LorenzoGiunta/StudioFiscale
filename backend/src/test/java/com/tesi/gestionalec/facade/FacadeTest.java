package com.tesi.gestionalec.facade;

import com.tesi.gestionalec.dto.request.DocumentoRequest;
import com.tesi.gestionalec.dto.request.PraticaRequest;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.dto.response.PraticaResponse;
import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.service.interfaces.ClienteService;
import com.tesi.gestionalec.service.interfaces.DocumentoService;
import com.tesi.gestionalec.service.interfaces.PraticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per PraticaFacade e DocumentoFacade: verificano l'orchestrazione
 * dei servizi e la propagazione del richiedente per i controlli di ownership.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Facade – Unit Tests")
class FacadeTest {

    @Mock PraticaService praticaService;
    @Mock ClienteService clienteService;
    @Mock DocumentoService documentoService;

    @InjectMocks
    PraticaFacade praticaFacade;

    DocumentoFacade documentoFacade;

    private Cliente cliente;
    private Pratica pratica;
    private Commercialista commercialista;

    @BeforeEach
    void setUp() {
        documentoFacade = new DocumentoFacade(documentoService, praticaService, clienteService);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Mario");
        cliente.setCognome("Rossi");

        commercialista = new Commercialista();
        commercialista.setId(99L);

        pratica = new Pratica();
        pratica.setId(10L);
        pratica.setCliente(cliente);
        pratica.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        pratica.setStato(StatoPratica.BOZZA);
        pratica.setStatoCorrente(new com.tesi.gestionalec.state.BozzaState());
    }

    private PraticaRequest praticaRequest() {
        PraticaRequest request = new PraticaRequest();
        request.setClienteId(1L);
        request.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        request.setScadenza(LocalDate.of(2025, 12, 31));
        return request;
    }

    // ─── PraticaFacade.creaEAssegna ───────────────────────────────────────────

    @Test
    @DisplayName("creaEAssegna: senza collaboratore — crea pratica e restituisce DTO")
    void creaEAssegna_senzaCollaboratore_creaPratica() {
        when(clienteService.trovaClientePerId(1L)).thenReturn(cliente);
        when(praticaService.creaPratica(any(), eq(commercialista))).thenReturn(pratica);

        PraticaResponse result = praticaFacade.creaEAssegna(praticaRequest(), null, commercialista);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        verify(praticaService).creaPratica(any(Pratica.class), eq(commercialista));
        verify(praticaService, never()).assegnaCollaboratore(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("creaEAssegna: con collaboratoreId — crea, assegna (con ownership) e rilegge")
    void creaEAssegna_conCollaboratore_assegnaERestituisce() {
        when(clienteService.trovaClientePerId(1L)).thenReturn(cliente);
        when(praticaService.creaPratica(any(), eq(commercialista))).thenReturn(pratica);
        when(praticaService.trovaPerId(10L, commercialista)).thenReturn(pratica);

        PraticaResponse result = praticaFacade.creaEAssegna(praticaRequest(), 5L, commercialista);

        verify(praticaService).assegnaCollaboratore(10L, 5L, commercialista);
        verify(praticaService).trovaPerId(10L, commercialista);
        assertThat(result.getId()).isEqualTo(10L);
    }

    // ─── PraticaFacade.avanzaERecupera ───────────────────────────────────────

    @Test
    @DisplayName("avanzaERecupera: avanza lo stato (con ownership) e restituisce il DTO aggiornato")
    void avanzaERecupera_avanzaERestituisce() {
        pratica.setStato(StatoPratica.IN_LAVORAZIONE);
        when(praticaService.trovaPerId(10L, commercialista)).thenReturn(pratica);

        PraticaResponse result = praticaFacade.avanzaERecupera(10L, commercialista);

        verify(praticaService).avanzaStato(10L, commercialista);
        verify(praticaService).trovaPerId(10L, commercialista);
        assertThat(result.getStato()).isEqualTo(StatoPratica.IN_LAVORAZIONE);
    }

    // ─── DocumentoFacade.caricaEAssegna ───────────────────────────────────────

    private DocumentoRequest documentoRequest() {
        DocumentoRequest request = new DocumentoRequest();
        request.setPraticaId(10L);
        request.setCaricatoDaId(1L);
        request.setNome("Fattura.pdf");
        request.setTipoFile("PDF");
        request.setPercorsoFile("uploads/fattura.pdf");
        return request;
    }

    @Test
    @DisplayName("caricaEAssegna: senza collaboratore — verifica ownership, carica e non assegna")
    void documentoFacade_caricaEAssegna_senzaCollaboratore() {
        Documento salvato = new Documento();
        salvato.setId(20L);
        salvato.setNome("Fattura.pdf");
        salvato.setStato(StatoDocumento.IN_REVISIONE);
        salvato.setVersione(1);
        salvato.setPratica(pratica);
        salvato.setCaricatoDa(cliente);

        when(praticaService.trovaPerId(10L, cliente)).thenReturn(pratica);
        when(clienteService.trovaClientePerId(1L)).thenReturn(cliente);
        when(documentoService.caricaDocumento(any())).thenReturn(salvato);

        DocumentoResponse result = documentoFacade.caricaEAssegna(documentoRequest(), null, cliente);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20L);
        verify(praticaService).trovaPerId(10L, cliente);
        verify(documentoService, never()).assegnaRevisore(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("caricaEAssegna: con collaboratoreId — carica, assegna il revisore (con ownership) e rilegge")
    void documentoFacade_caricaEAssegna_conCollaboratore() {
        Documento salvato = new Documento();
        salvato.setId(20L);
        salvato.setNome("CUD.pdf");
        salvato.setStato(StatoDocumento.IN_REVISIONE);
        salvato.setVersione(1);
        salvato.setPratica(pratica);
        salvato.setCaricatoDa(cliente);

        when(praticaService.trovaPerId(10L, cliente)).thenReturn(pratica);
        when(clienteService.trovaClientePerId(1L)).thenReturn(cliente);
        when(documentoService.caricaDocumento(any())).thenReturn(salvato);
        when(documentoService.trovaPerId(20L, cliente)).thenReturn(salvato);

        DocumentoResponse result = documentoFacade.caricaEAssegna(documentoRequest(), 5L, cliente);

        assertThat(result.getId()).isEqualTo(20L);
        verify(documentoService).assegnaRevisore(20L, 5L, cliente);
        verify(documentoService).trovaPerId(20L, cliente);
    }
}
