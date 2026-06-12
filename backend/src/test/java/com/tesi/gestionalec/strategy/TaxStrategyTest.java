package com.tesi.gestionalec.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per il Pattern Strategy del calcolo delle imposte.
 * Zero dipendenze: testano la matematica fiscale pura.
 */
@DisplayName("Strategy – Calcolo Imposte")
class TaxStrategyTest {

    private RegimeForfettarioStrategy forfettario;
    private RegimeOrdinarioStrategy ordinario;

    @BeforeEach
    void setUp() {
        forfettario = new RegimeForfettarioStrategy();
        ordinario   = new RegimeOrdinarioStrategy();
    }

    // ─── RegimeForfettario ────────────────────────────────────────────────────
    // Formula: reddito * 0.67 (coefficiente) * 0.15 (aliquota flat)

    @Test
    @DisplayName("Forfettario: reddito zero → imposta zero")
    void forfettario_redditoZero() {
        assertThat(forfettario.calcola(0)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Forfettario: reddito 10.000 → 1.005,00")
    void forfettario_reddito10000() {
        double attesa = 10_000 * 0.67 * 0.15;   // 1005.0
        assertThat(forfettario.calcola(10_000)).isCloseTo(attesa, within(0.01));
    }

    @Test
    @DisplayName("Forfettario: reddito 30.000 → 3.015,00")
    void forfettario_reddito30000() {
        double attesa = 30_000 * 0.67 * 0.15;   // 3015.0
        assertThat(forfettario.calcola(30_000)).isCloseTo(attesa, within(0.01));
    }

    @Test
    @DisplayName("Forfettario: risultato è sempre inferiore al reddito lordo")
    void forfettario_risultatoSempreMenoreDelReddito() {
        assertThat(forfettario.calcola(50_000)).isLessThan(50_000);
    }

    // ─── RegimeOrdinario ──────────────────────────────────────────────────────
    // Scaglioni: ≤28k→23%, 28-50k→35%, >50k→43%

    @Test
    @DisplayName("Ordinario: reddito zero → imposta zero")
    void ordinario_redditoZero() {
        assertThat(ordinario.calcola(0)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Ordinario: primo scaglione (≤28.000) → 23%")
    void ordinario_primoScaglione() {
        double attesa = 20_000 * 0.23;   // 4600.0
        assertThat(ordinario.calcola(20_000)).isCloseTo(attesa, within(0.01));
    }

    @Test
    @DisplayName("Ordinario: al limite del primo scaglione (28.000) → 6.440,00")
    void ordinario_limitePrimoScaglione() {
        double attesa = 28_000 * 0.23;   // 6440.0
        assertThat(ordinario.calcola(28_000)).isCloseTo(attesa, within(0.01));
    }

    @Test
    @DisplayName("Ordinario: secondo scaglione (28k-50k) applica 35% sulla parte eccedente")
    void ordinario_secondoScaglione() {
        // reddito 40.000: 28k*0.23 + 12k*0.35 = 6440 + 4200 = 10640
        double attesa = 28_000 * 0.23 + (40_000 - 28_000) * 0.35;
        assertThat(ordinario.calcola(40_000)).isCloseTo(attesa, within(0.01));
    }

    @Test
    @DisplayName("Ordinario: al limite del secondo scaglione (50.000)")
    void ordinario_limiteSecondoScaglione() {
        double attesa = 28_000 * 0.23 + 22_000 * 0.35;  // 6440 + 7700 = 14140
        assertThat(ordinario.calcola(50_000)).isCloseTo(attesa, within(0.01));
    }

    @Test
    @DisplayName("Ordinario: terzo scaglione (>50k) applica 43% sulla parte eccedente")
    void ordinario_terzoScaglione() {
        // reddito 80.000: 28k*0.23 + 22k*0.35 + 30k*0.43
        double attesa = 28_000 * 0.23 + 22_000 * 0.35 + (80_000 - 50_000) * 0.43;
        assertThat(ordinario.calcola(80_000)).isCloseTo(attesa, within(0.01));
    }

    @ParameterizedTest(name = "Ordinario: reddito {0} → imposta positiva")
    @CsvSource({"1000", "28000", "28001", "50000", "50001", "100000"})
    @DisplayName("Ordinario: qualsiasi reddito positivo produce imposta positiva")
    void ordinario_redditoPositivo_impostaPositiva(double reddito) {
        assertThat(ordinario.calcola(reddito)).isPositive();
    }

    @Test
    @DisplayName("Ordinario è sempre più alto del forfettario oltre i 28.000 di reddito")
    void ordinario_sempreMaggioreDiForfettario_oltre28k() {
        // Il regime ordinario è tipicamente più oneroso per redditi alti
        double reddito = 60_000;
        assertThat(ordinario.calcola(reddito)).isGreaterThan(forfettario.calcola(reddito));
    }
}
