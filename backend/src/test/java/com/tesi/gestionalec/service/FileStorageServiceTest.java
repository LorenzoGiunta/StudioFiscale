package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.FileNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per FileStorageService.
 * Usa @TempDir di JUnit per creare una cartella temporanea, evitando
 * side-effect sul filesystem reale.
 */
@DisplayName("FileStorageService – Unit Tests")
class FileStorageServiceTest {

    @TempDir Path tempDir;
    private FileStorageService service;

    @BeforeEach
    void setUp() throws IOException {
        service = new FileStorageService(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // @TempDir si auto-pulisce, ma rimuoviamo eventuali residui
        if (Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try { if (!p.equals(tempDir)) Files.deleteIfExists(p); }
                    catch (IOException ignored) {}
                });
            }
        }
    }

    @Test
    @DisplayName("salva: scrive il file e restituisce un percorso relativo non vuoto")
    void salva_scriveFileERestituiscePercorso() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "ciao".getBytes());

        String percorso = service.salva(file);

        assertThat(percorso).isNotBlank();
        assertThat(percorso).endsWith("doc.pdf");
        assertThat(Files.exists(tempDir.resolve(percorso))).isTrue();
    }

    @Test
    @DisplayName("salva: filename con path traversal → IllegalArgumentException")
    void salva_pathTraversal_lanciaEccezione() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.salva(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non valido");
    }

    @Test
    @DisplayName("salva: filename vuoto → salva comunque senza eccezioni")
    void salva_filenameVuoto_nonLancia() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "", "application/octet-stream", "x".getBytes());

        String percorso = service.salva(file);

        assertThat(percorso).isNotBlank();
        assertThat(Files.exists(tempDir.resolve(percorso))).isTrue();
    }

    @Test
    @DisplayName("carica: ritorna Resource leggibile per file esistente")
    void carica_fileEsistente_ritornaResource() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes());
        String percorso = service.salva(file);

        Resource res = service.carica(percorso);

        assertThat(res.exists()).isTrue();
        assertThat(res.isReadable()).isTrue();
    }

    @Test
    @DisplayName("carica: file inesistente → FileNotFoundException")
    void carica_fileInesistente_lanciaEccezione() {
        assertThatThrownBy(() -> service.carica("inesistente.pdf"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("elimina: rimuove file esistente senza lanciare")
    void elimina_fileEsistente_rimuove() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "del.txt", "text/plain", "x".getBytes());
        String percorso = service.salva(file);

        service.elimina(percorso);

        assertThat(Files.exists(tempDir.resolve(percorso))).isFalse();
    }

    @Test
    @DisplayName("elimina: file inesistente → no-op (nessuna eccezione)")
    void elimina_fileInesistente_noOp() {
        assertThatCode(() -> service.elimina("nope.pdf")).doesNotThrowAnyException();
    }
}
