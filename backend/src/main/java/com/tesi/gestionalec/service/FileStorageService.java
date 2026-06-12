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
import java.util.UUID;

/**
 * Servizio di archiviazione dei file su filesystem.
 *
 * Gestisce salvataggio, lettura ed eliminazione dei documenti caricati,
 * memorizzandoli in una directory configurabile. Per evitare collisioni i file
 * vengono rinominati con un identificativo univoco e, in fase di salvataggio,
 * si verifica che il nome non consenta di uscire dalla cartella consentita
 * (protezione contro il path traversal).
 */
@Service
public class FileStorageService {

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
        String nomeFisico = UUID.randomUUID() + "_" + nomeOriginale;
        Path destinazione = this.uploadDir.resolve(nomeFisico);
        Files.copy(file.getInputStream(), destinazione, StandardCopyOption.REPLACE_EXISTING);
        return uploadDir.relativize(destinazione.toAbsolutePath()).toString();
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
