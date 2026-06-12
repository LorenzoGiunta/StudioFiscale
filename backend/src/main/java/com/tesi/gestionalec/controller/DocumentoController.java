package com.tesi.gestionalec.controller;

import com.tesi.gestionalec.dto.request.DocumentoRequest;
import com.tesi.gestionalec.dto.response.DocumentoResponse;
import com.tesi.gestionalec.facade.DocumentoFacade;
import com.tesi.gestionalec.mapper.DocumentoMapper;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.service.FileStorageService;
import com.tesi.gestionalec.service.interfaces.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controller per la gestione dei documenti.
 *
 * Espone gli endpoint per il caricamento da parte del cliente, la creazione di
 * nuove versioni, il download con il tipo di contenuto appropriato,
 * l'assegnazione del revisore e la cancellazione logica. L'accesso ai singoli
 * endpoint è regolato in base al ruolo dell'utente.
 *
 * Tutti gli endpoint sensibili verificano l'ownership in base al ruolo prima
 * di restituire o modificare i dati, prevenendo accessi IDOR.
 */
@RestController
@RequestMapping("/api/documenti")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final FileStorageService fileStorageService;
    private final DocumentoFacade documentoFacade;

    /**
     * Carica un nuovo documento associato alla pratica specificata.
     * Salvato il file su disco, l'orchestrazione (ownership sulla pratica,
     * mapping e persistenza) è delegata al Facade.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<DocumentoResponse> carica(
            @RequestPart("file") MultipartFile file,
            @RequestPart("nome") String nome,
            @RequestPart("tipoFile") String tipoFile,
            @RequestPart("praticaId") String praticaId,
            @AuthenticationPrincipal Utente utente) throws IOException {

        String percorsoFile = fileStorageService.salva(file);

        DocumentoRequest request = new DocumentoRequest();
        request.setNome(nome.isBlank() ? file.getOriginalFilename() : nome);
        request.setTipoFile(tipoFile);
        request.setPercorsoFile(percorsoFile);
        request.setDimensione(file.getSize());
        request.setPraticaId(Long.parseLong(praticaId));
        request.setCaricatoDaId(utente.getId());

        return ResponseEntity.ok(documentoFacade.caricaEAssegna(request, null, utente));
    }

    /**
     * Recupera un documento per id verificando l'ownership in base al ruolo
     * (CLIENTE→autore, COLLABORATORE→revisore assegnato, COMMERCIALISTA→studio).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_COMMERCIALISTA', 'ROLE_COLLABORATORE', 'ROLE_CLIENTE')")
    public ResponseEntity<DocumentoResponse> trovaPerId(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(DocumentoMapper.toResponse(documentoService.trovaPerId(id, utente)));
    }

    /**
     * Carica una nuova versione di un documento esistente.
     * Verifica che il richiedente sia il cliente che ha caricato la versione
     * originale.
     */
    @PostMapping(value = "/{id}/nuova-versione", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<DocumentoResponse> nuovaVersione(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "nome", required = false) String nome,
            @RequestPart(value = "tipoFile", required = false) String tipoFile,
            @AuthenticationPrincipal Utente utente) throws IOException {

        String percorsoFile = fileStorageService.salva(file);

        Documento nuovoDocumento = new Documento();
        nuovoDocumento.setNome((nome == null || nome.isBlank()) ? file.getOriginalFilename() : nome);
        nuovoDocumento.setTipoFile(tipoFile);
        nuovoDocumento.setPercorsoFile(percorsoFile);
        nuovoDocumento.setDimensione(file.getSize());

        // Ownership check: solo l'autore originale può caricare una nuova versione
        return ResponseEntity.ok(
                DocumentoMapper.toResponse(documentoService.nuovaVersione(id, nuovoDocumento, utente)));
    }

    /**
     * Download del documento con ownership check (stessa logica di trovaPerId).
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_COMMERCIALISTA', 'ROLE_COLLABORATORE', 'ROLE_CLIENTE')")
    public ResponseEntity<Resource> download(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) throws IOException {
        Documento doc = documentoService.trovaPerId(id, utente);
        Resource resource = fileStorageService.carica(doc.getPercorsoFile());

        String contentType = "application/octet-stream";
        String nome = doc.getNome().toLowerCase();
        if (nome.endsWith(".pdf"))
            contentType = "application/pdf";
        else if (nome.endsWith(".doc") || nome.endsWith(".docx"))
            contentType = "application/msword";
        else if (nome.endsWith(".xls") || nome.endsWith(".xlsx"))
            contentType = "application/vnd.ms-excel";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getNome() + "\"")
                .body(resource);
    }

    @PutMapping("/{id}/assegna-revisore/{collaboratoreId}")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> assegnaRevisore(@PathVariable Long id,
            @PathVariable Long collaboratoreId,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: documento del proprio studio + collaboratore del proprio
        // studio
        documentoService.assegnaRevisore(id, collaboratoreId, utente);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approva")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> approva(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: solo i documenti dello studio del richiedente
        documentoService.approvaDocumento(id, utente);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/rifiuta")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> rifiuta(@PathVariable Long id,
            @RequestParam String motivazione,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: solo i documenti dello studio del richiedente
        documentoService.rifiutaDocumento(id, motivazione, utente);
        return ResponseEntity.ok().build();
    }

    // Cancellazione logica: il file fisico resta conservato per l'audit fiscale
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_COMMERCIALISTA')")
    public ResponseEntity<Void> elimina(@PathVariable Long id,
            @AuthenticationPrincipal Utente utente) {
        // Ownership check: solo i documenti dello studio del richiedente
        documentoService.eliminaDocumento(id, utente);
        return ResponseEntity.noContent().build();
    }
}
