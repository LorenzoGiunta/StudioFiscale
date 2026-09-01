package com.tesi.gestionalec.state;

import com.tesi.gestionalec.exception.InvalidStateException;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.StatoDocumento;
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

    // ─── Stato iniziale ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Una pratica nuova nasce già in BOZZA, senza interventi esterni")
    void praticaNuova_nasceInBozza() {
        assertThat(pratica.getStatoCorrente()).isSameAs(BozzaState.INSTANCE);
        assertThat(pratica.getStatoCorrente().getCodice()).isEqualTo("BOZZA");
    }

    // ─── BOZZA ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BozzaState.avanza() → IN_LAVORAZIONE")
    void bozza_avanza_aInLavorazione() {
        pratica.setStatoCorrente(BozzaState.INSTANCE);

        pratica.avanza();

        assertThat(pratica.getStatoCorrente()).isSameAs(InLavorazioneState.INSTANCE);
    }

    @Test
    @DisplayName("BozzaState espone codice ed etichetta")
    void bozza_codiceEdEtichetta() {
        assertThat(BozzaState.INSTANCE.getCodice()).isEqualTo("BOZZA");
        assertThat(BozzaState.INSTANCE.getEtichetta()).isEqualTo("Bozza");
        assertThat(BozzaState.INSTANCE.isFinale()).isFalse();
    }

    // ─── IN_LAVORAZIONE ───────────────────────────────────────────────────────

    @Test
    @DisplayName("InLavorazioneState.avanza() → IN_ATTESA_DOCUMENTI")
    void inLavorazione_avanza_aInAttesaDocumenti() {
        pratica.setStatoCorrente(InLavorazioneState.INSTANCE);

        pratica.avanza();

        assertThat(pratica.getStatoCorrente()).isSameAs(InAttesaDocumentiState.INSTANCE);
    }

    @Test
    @DisplayName("InLavorazioneState espone codice ed etichetta")
    void inLavorazione_codiceEdEtichetta() {
        assertThat(InLavorazioneState.INSTANCE.getCodice()).isEqualTo("IN_LAVORAZIONE");
        assertThat(InLavorazioneState.INSTANCE.getEtichetta()).isEqualTo("In lavorazione");
    }

    // ─── IN_ATTESA_DOCUMENTI ──────────────────────────────────────────────────

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() → COMPLETATA (tutti i documenti approvati)")
    void inAttesaDocumenti_avanza_aCompletata() {
        pratica.setStatoCorrente(InAttesaDocumentiState.INSTANCE);
        impostaDocumentiApprovati(2);

        pratica.avanza();

        assertThat(pratica.getStatoCorrente()).isSameAs(CompletataState.INSTANCE);
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se non ci sono documenti")
    void inAttesaDocumenti_avanza_senzaDocumenti_lanciaEccezione() {
        pratica.setStatoCorrente(InAttesaDocumentiState.INSTANCE);
        pratica.setListaDocumenti(new ArrayList<>());

        assertThatThrownBy(() -> pratica.avanza())
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("almeno un documento");

        // Il fallimento non deve lasciare la pratica in uno stato avanzato
        assertThat(pratica.getStatoCorrente()).isSameAs(InAttesaDocumentiState.INSTANCE);
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se lista documenti è null")
    void inAttesaDocumenti_avanza_listaNull_lanciaEccezione() {
        pratica.setStatoCorrente(InAttesaDocumentiState.INSTANCE);
        pratica.setListaDocumenti(null);

        assertThatThrownBy(() -> pratica.avanza())
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("almeno un documento");
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se un documento è IN_REVISIONE")
    void inAttesaDocumenti_avanza_conDocumentoInRevisione_lanciaEccezione() {
        pratica.setStatoCorrente(InAttesaDocumentiState.INSTANCE);
        pratica.setListaDocumenti(List.of(
                creaDocumento(StatoDocumento.APPROVATO),
                creaDocumento(StatoDocumento.IN_REVISIONE)
        ));

        assertThatThrownBy(() -> pratica.avanza())
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("tutti i documenti devono essere approvati");
    }

    @Test
    @DisplayName("InAttesaDocumentiState.avanza() lancia eccezione se un documento è RIFIUTATO")
    void inAttesaDocumenti_avanza_conDocumentoRifiutato_lanciaEccezione() {
        pratica.setStatoCorrente(InAttesaDocumentiState.INSTANCE);
        pratica.setListaDocumenti(List.of(
                creaDocumento(StatoDocumento.APPROVATO),
                creaDocumento(StatoDocumento.RIFIUTATO)
        ));

        assertThatThrownBy(() -> pratica.avanza())
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("tutti i documenti devono essere approvati");
    }

    @Test
    @DisplayName("InAttesaDocumentiState espone codice ed etichetta")
    void inAttesaDocumenti_codiceEdEtichetta() {
        assertThat(InAttesaDocumentiState.INSTANCE.getCodice()).isEqualTo("IN_ATTESA_DOCUMENTI");
        assertThat(InAttesaDocumentiState.INSTANCE.getEtichetta()).isEqualTo("In attesa documenti");
    }

    // ─── COMPLETATA (terminale) ───────────────────────────────────────────────

    @Test
    @DisplayName("CompletataState.avanza() lancia InvalidStateException")
    void completata_avanza_lanciaEccezione() {
        pratica.setStatoCorrente(CompletataState.INSTANCE);

        assertThatThrownBy(() -> pratica.avanza())
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("COMPLETATA");
    }

    @Test
    @DisplayName("CompletataState è terminale ed espone codice ed etichetta")
    void completata_eTerminale() {
        assertThat(CompletataState.INSTANCE.isFinale()).isTrue();
        assertThat(CompletataState.INSTANCE.getCodice()).isEqualTo("COMPLETATA");
        assertThat(CompletataState.INSTANCE.getEtichetta()).isEqualTo("Completata");
    }

    // ─── Flusso completo end-to-end ───────────────────────────────────────────

    @Test
    @DisplayName("Flusso completo: BOZZA → IN_LAVORAZIONE → IN_ATTESA_DOCUMENTI → COMPLETATA")
    void flussoDiAvanzamentoCompleto() {
        assertThat(pratica.getStatoCorrente()).isSameAs(BozzaState.INSTANCE);

        pratica.avanza();
        assertThat(pratica.getStatoCorrente()).isSameAs(InLavorazioneState.INSTANCE);

        pratica.avanza();
        assertThat(pratica.getStatoCorrente()).isSameAs(InAttesaDocumentiState.INSTANCE);

        impostaDocumentiApprovati(1);
        pratica.avanza();
        assertThat(pratica.getStatoCorrente()).isSameAs(CompletataState.INSTANCE);

        // Stato terminale: non si avanza oltre
        assertThatThrownBy(() -> pratica.avanza())
                .isInstanceOf(InvalidStateException.class);
    }

    // ─── Converter di persistenza ─────────────────────────────────────────────

    @Test
    @DisplayName("Il converter fa andata e ritorno tra oggetto di stato e codice")
    void converter_andataERitorno() {
        StatoPraticaConverter converter = new StatoPraticaConverter();

        for (StatoPraticaState stato : List.of(
                BozzaState.INSTANCE, InLavorazioneState.INSTANCE,
                InAttesaDocumentiState.INSTANCE, CompletataState.INSTANCE)) {

            String codice = converter.convertToDatabaseColumn(stato);
            assertThat(codice).isEqualTo(stato.getCodice());
            assertThat(converter.convertToEntityAttribute(codice)).isSameAs(stato);
        }
    }

    @Test
    @DisplayName("Il converter gestisce il valore null in entrambe le direzioni")
    void converter_null() {
        StatoPraticaConverter converter = new StatoPraticaConverter();

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("Il converter rifiuta un codice sconosciuto letto dal database")
    void converter_codiceSconosciuto() {
        StatoPraticaConverter converter = new StatoPraticaConverter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute("ARCHIVIATA"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ARCHIVIATA");
    }
}
