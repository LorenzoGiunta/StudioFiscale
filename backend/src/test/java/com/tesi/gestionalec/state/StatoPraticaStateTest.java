package com.tesi.gestionalec.state;

import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;
import com.tesi.gestionalec.model.StatoPratica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per il Pattern State delle Pratiche.
 * Nessuna dipendenza Spring/DB: testano la logica pura di avanzamento stato.
 *
 * Flusso atteso:
 *   BOZZA → IN_LAVORAZIONE → IN_ATTESA_DOCUMENTI → COMPLETATA (terminale)
 *
 * Da IN_ATTESA_DOCUMENTI la transizione a COMPLETATA richiede almeno un
 * documento associato alla pratica e che tutti risultino approvati.
 */
@DisplayName("State Pattern – Pratica")
class StatoPraticaStateTest {

    private Pratica pratica;

    @BeforeEach
    void setUp() {
        pratica = new Pratica();
    }

    /** Crea un documento con lo stato indicato e lo associa alla pratica. */
    private Documento creaDocumento(StatoDocumento stato) {
        Documento doc = new Documento();
        doc.setStato(stato);
        doc.setPratica(pratica);
        return doc;
    }

    /** Imposta sulla pratica una lista di documenti tutti APPROVATO. */
    private void impostaDocumentiApprovati(int quanti) {
        List<Documento> docs = new ArrayList<>();
        for (int i = 0; i < quanti; i++) {
            docs.add(creaDocumento(StatoDocumento.APPROVATO));
        }
        pratica.setListaDocumenti(docs);
    }

    // ─── BOZZA ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BozzaState.avanza() → IN_LAVORAZIONE")
    void bozza_avanza_aInLavorazione() {
        pratica.setStato(StatoPratica.BOZZA);
        pratica.setStatoCorrente(new BozzaState());

        pratica.getStatoCorrente().avanza(pratica);

        assertThat(pratica.getStato()).isEqualTo(StatoPratica.IN_LAVORAZIONE);
        assertThat(pratica.getStatoCorrente()).isInstanceOf(InLavorazioneState.class);
    }

    @Test
    @DisplayName("BozzaState.getStato() restituisce BOZZA")
    void bozza_getStato() {
        assertThat(new BozzaState().getStato()).isEqualTo(StatoPratica.BOZZA);
    }

    // ─── IN_LAVORAZIONE ───────────────────────────────────────────────────────

    @Test
    @DisplayName("InLavorazioneState.avanza() → IN_ATTESA_DOCUMENTI")
    void inLavorazione_avanza_aInAttesaDocumenti() {
        pratica.setStato(StatoPratica.IN_LAVORAZIONE);
        pratica.setStatoCorrente(new InLavorazioneState());

        pratica.getStatoCorrente().avanza(pratica);

        assertThat(pratica.getStato()).isEqualTo(StatoPratica.IN_ATTESA_DOCUMENTI);
        assertThat(pratica.getStatoCorrente()).isInstanceOf(InAttesaDocumentiState.class);
    }

    @Test
    @DisplayName("InLavorazioneState.getStato() restituisce IN_LAVORAZIONE")
    void inLavorazione_getStato() {
        assertThat(new InLavorazioneState().getStato()).isEqualTo(StatoPratica.IN_LAVORAZIONE);
    }

    // ─── IN_ATTESA_DOCUMENTI ──────────────────────────────────────────────────

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() → COMPLETATA (tutti i documenti approvati)")
    void inAttesaDocumenti_avanza_aCompletata() {
        pratica.setStato(StatoPratica.IN_ATTESA_DOCUMENTI);
        pratica.setStatoCorrente(new InAttesaDocumentiState());
        impostaDocumentiApprovati(2);

        pratica.getStatoCorrente().avanza(pratica);

        assertThat(pratica.getStato()).isEqualTo(StatoPratica.COMPLETATA);
        assertThat(pratica.getStatoCorrente()).isInstanceOf(CompletataState.class);
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se non ci sono documenti")
    void inAttesaDocumenti_avanza_senzaDocumenti_lanciaEccezione() {
        pratica.setStato(StatoPratica.IN_ATTESA_DOCUMENTI);
        pratica.setStatoCorrente(new InAttesaDocumentiState());
        pratica.setListaDocumenti(new ArrayList<>());

        assertThatThrownBy(() -> pratica.getStatoCorrente().avanza(pratica))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("almeno un documento");
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se lista documenti è null")
    void inAttesaDocumenti_avanza_listaNull_lanciaEccezione() {
        pratica.setStato(StatoPratica.IN_ATTESA_DOCUMENTI);
        pratica.setStatoCorrente(new InAttesaDocumentiState());
        pratica.setListaDocumenti(null);

        assertThatThrownBy(() -> pratica.getStatoCorrente().avanza(pratica))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("almeno un documento");
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se un documento è IN_REVISIONE")
    void inAttesaDocumenti_avanza_conDocumentoInRevisione_lanciaEccezione() {
        pratica.setStato(StatoPratica.IN_ATTESA_DOCUMENTI);
        pratica.setStatoCorrente(new InAttesaDocumentiState());
        pratica.setListaDocumenti(List.of(
                creaDocumento(StatoDocumento.APPROVATO),
                creaDocumento(StatoDocumento.IN_REVISIONE)
        ));

        assertThatThrownBy(() -> pratica.getStatoCorrente().avanza(pratica))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("tutti i documenti devono essere approvati");
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se un documento è RIFIUTATO")
    void inAttesaDocumenti_avanza_conDocumentoRifiutato_lanciaEccezione() {
        pratica.setStato(StatoPratica.IN_ATTESA_DOCUMENTI);
        pratica.setStatoCorrente(new InAttesaDocumentiState());
        pratica.setListaDocumenti(List.of(
                creaDocumento(StatoDocumento.APPROVATO),
                creaDocumento(StatoDocumento.RIFIUTATO)
        ));

        assertThatThrownBy(() -> pratica.getStatoCorrente().avanza(pratica))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("tutti i documenti devono essere approvati");
    }

    @Test
    @DisplayName("InAttesaDocumentiState.getStato() restituisce IN_ATTESA_DOCUMENTI")
    void inAttesaDocumenti_getStato() {
        assertThat(new InAttesaDocumentiState().getStato()).isEqualTo(StatoPratica.IN_ATTESA_DOCUMENTI);
    }

    // ─── COMPLETATA (terminale) ────────────────────────────────────────────────

    @Test
    @DisplayName("CompletataState.avanza() lancia InvalidStateException")
    void completata_avanza_lanciaEccezione() {
        pratica.setStato(StatoPratica.COMPLETATA);
        pratica.setStatoCorrente(new CompletataState());

        assertThatThrownBy(() -> pratica.getStatoCorrente().avanza(pratica))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("COMPLETATA");
    }

    @Test
    @DisplayName("CompletataState.getStato() restituisce COMPLETATA")
    void completata_getStato() {
        assertThat(new CompletataState().getStato()).isEqualTo(StatoPratica.COMPLETATA);
    }

    // ─── Flusso completo end-to-end ────────────────────────────────────────────

    @Test
    @DisplayName("Flusso completo: BOZZA → IN_LAVORAZIONE → IN_ATTESA_DOCUMENTI → COMPLETATA")
    void flussoDiAvanzamentoCompleto() {
        pratica.setStato(StatoPratica.BOZZA);
        pratica.setStatoCorrente(new BozzaState());

        pratica.getStatoCorrente().avanza(pratica);
        assertThat(pratica.getStato()).isEqualTo(StatoPratica.IN_LAVORAZIONE);

        pratica.getStatoCorrente().avanza(pratica);
        assertThat(pratica.getStato()).isEqualTo(StatoPratica.IN_ATTESA_DOCUMENTI);

        // Per avanzare a COMPLETATA servono documenti tutti approvati
        impostaDocumentiApprovati(1);

        pratica.getStatoCorrente().avanza(pratica);
        assertThat(pratica.getStato()).isEqualTo(StatoPratica.COMPLETATA);

        // Lo stato terminale non si può più avanzare
        assertThatThrownBy(() -> pratica.getStatoCorrente().avanza(pratica))
                .isInstanceOf(InvalidStateException.class);
    }
}

