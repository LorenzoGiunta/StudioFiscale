package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.FileNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Servizio di archiviazione dei file su filesystem.
 *
 * Gestisce salvataggio, lettura ed eliminazione dei documenti caricati,
 * memorizzandoli in una directory configurabile. Per evitare collisioni i file
 * vengono rinominati con un identificativo univoco e, in fase di salvataggio,
 * si verifica che il nome non consenta di uscire dalla cartella consentita
 * (protezione contro il path traversal). Sono ammessi solo i formati tipici
 * dei documenti fiscali: eseguibili, script e pagine HTML vengono respinti sia
 * per estensione sia per content-type dichiarato.
 */
@Service
public class FileStorageService {

    /** Estensioni ammesse per i documenti caricati (minuscole, senza punto). */
    static final Set<String> ESTENSIONI_AMMESSE = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

    /** Content-type ammessi; il browser li dichiara, il server li accetta solo se coerenti. */
    static final Set<String> CONTENT_TYPE_AMMESSI = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "application/octet-stream");   // alcuni client non specificano il tipo

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDirPath) throws IOException {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    /**
     * Salva il file caricato e restituisce il percorso relativo da conservare in
     * banca dati. Il nome fisico antepone un identificativo univoco a quello
     * originale per scongiurare le collisioni tra documenti omonimi.
     */
    public String salva(MultipartFile file) throws IOException {
        String nomeOriginale = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        // Rifiuta i nomi che tentano di risalire l'albero delle directory
        if (nomeOriginale.contains("..")) {
            throw new IllegalArgumentException("Nome file non valido: " + nomeOriginale);
        }
        verificaFormatoAmmesso(nomeOriginale, file.getContentType());

        String nomeFisico = UUID.randomUUID() + "_" + nomeOriginale;
        Path destinazione = this.uploadDir.resolve(nomeFisico);
        Files.copy(file.getInputStream(), destinazione, StandardCopyOption.REPLACE_EXISTING);
        return uploadDir.relativize(destinazione.toAbsolutePath()).toString();
    }

    /**
     * Ammette solo i formati previsti per i documenti fiscali. Il controllo è
     * doppio: sull'estensione (quella che poi decide il content-type in download)
     * e sul content-type dichiarato dal client, se presente.
     */
    static void verificaFormatoAmmesso(String nomeFile, String contentType) {
        int punto = nomeFile.lastIndexOf('.');
        String estensione = punto >= 0 ? nomeFile.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        if (!ESTENSIONI_AMMESSE.contains(estensione)) {
            throw new IllegalArgumentException(
                    "Formato non ammesso: sono accettati solo " + String.join(", ", ESTENSIONI_AMMESSE.stream().sorted().toList()));
        }
        if (contentType != null && !CONTENT_TYPE_AMMESSI.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Tipo di contenuto non ammesso: " + contentType);
        }
    }

    /**
     * Recupera un file archiviato come {@link Resource}, pronto per il download.
     */
    public Resource carica(String percorsoRelativo) throws IOException {
        Path filePath = this.uploadDir.resolve(percorsoRelativo).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new FileNotFoundException(percorsoRelativo);
    }

    /**
     * Rimuove un file dall'archivio, ad esempio quando una versione viene
     * sostituita. L'eventuale errore di I/O non interrompe il flusso chiamante.
     */
    public void elimina(String percorsoRelativo) {
        try {
            Path filePath = this.uploadDir.resolve(percorsoRelativo).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Errore non bloccante: la rimozione del file è best-effort
        }
    }
}
