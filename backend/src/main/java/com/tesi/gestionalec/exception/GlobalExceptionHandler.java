package com.tesi.gestionalec.exception;


import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestore centralizzato delle eccezioni dell'applicazione.
 *
 * Raccoglie in un unico punto le eccezioni sollevate da controller e servizi e
 * le converte in risposte JSON uniformi ({@link ApiError}), associando a
 * ciascuna il codice di stato HTTP appropriato. In questo modo il client riceve
 * sempre un formato d'errore coerente e i dettagli interni (stacktrace, messaggi
 * SQL) non vengono mai esposti all'esterno.
 *
 * Gli handler sono ordinati dal caso più specifico a quello più generico, che
 * funge da fallback per qualunque eccezione non prevista.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Errori di validazione dei dati in ingresso (400)

    /**
     * Gestisce il fallimento della validazione dei DTO annotati per la
     * convalida. Restituisce l'elenco completo dei campi non validi con il
     * relativo messaggio, così che il client possa segnalarli puntualmente.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Trasforma ogni errore di campo in una coppia nome-campo / messaggio
        Map<String, String> campiInvalidi = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Valore non valido",
                        // a parità di campo si conserva il primo messaggio
                        (msg1, msg2) -> msg1
                ));

        log.warn("[VALIDAZIONE] Richiesta non valida su {}: {}", request.getRequestURI(), campiInvalidi);

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Dati non validi",
                "La richiesta contiene " + campiInvalidi.size() + " campo/i non valido/i.",
                request.getRequestURI()
        );
        error.setCampiInvalidi(campiInvalidi);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Argomenti non validi a livello applicativo (400)

    /**
     * Gestisce gli {@link IllegalArgumentException} sollevati dalla logica
     * applicativa, ad esempio il controllo contro il path traversal nel
     * servizio di archiviazione dei file.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("[BAD REQUEST] {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Richiesta non valida",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Risorsa di dominio inesistente (404)

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("[NOT FOUND] {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Risorsa non trovata",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // File non presente nello storage (404)

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ApiError> handleFileNotFoundException(
            FileNotFoundException ex,
            HttpServletRequest request) {

        log.warn("[NOT FOUND - FILE] {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "File non trovato",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Email già registrata (409)

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("[CONFLITTO] Email duplicata su {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Conflitto dati",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Invito di collaborazione duplicato (409)

    @ExceptionHandler(DuplicateInviteException.class)
    public ResponseEntity<ApiError> handleDuplicateInviteException(
            DuplicateInviteException ex,
            HttpServletRequest request) {

        log.warn("[CONFLITTO] Invito duplicato su {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Invito duplicato",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Transizione di stato non ammessa (409)

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiError> handleInvalidStateException(
            InvalidStateException ex,
            HttpServletRequest request) {

        log.warn("[STATO INVALIDO] {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Operazione non consentita",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Operazione su risorsa non di propria competenza (403)

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiError> handleForbiddenOperationException(
            ForbiddenOperationException ex,
            HttpServletRequest request) {

        log.warn("[FORBIDDEN] Tentativo di operazione non autorizzata su {}: {}",
                request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "Operazione non autorizzata",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Accesso negato da Spring Security per ruolo insufficiente (403)

    /**
     * Intercetta l'{@link AccessDeniedException} sollevata dal controllo di
     * autorizzazione quando l'utente è autenticato ma privo del ruolo richiesto.
     * Perché l'eccezione giunga qui, la configurazione di sicurezza non deve
     * interromperne il flusso con un gestore dedicato.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("[ACCESSO NEGATO] Tentativo di accesso non autorizzato su {} ({})",
                request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "Accesso negato",
                "Non hai i permessi necessari per eseguire questa operazione.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Entità non trovata a livello di persistenza JPA (404)

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        log.warn("[NOT FOUND - JPA] {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Risorsa non trovata",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Credenziali errate in fase di login (401)

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {

        log.warn("[AUTH] Tentativo di login fallito su {}", request.getRequestURI());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Credenziali non valide",
                "Email o password non corretti. Riprova.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Account disabilitato (401)

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request) {

        log.warn("[AUTH] Tentativo di accesso con account disabilitato su {}", request.getRequestURI());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Account disabilitato",
                "Il tuo account è stato disabilitato. Contatta l'amministratore.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Violazione di un vincolo di integrità del database (409)

    /**
     * Rete di sicurezza per le violazioni di integrità del database
     * (tipicamente vincoli di unicità) che dovessero sfuggire ai controlli
     * applicativi: restituisce un conflitto anziché un errore generico, senza
     * rivelare dettagli SQL interni.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("[CONFLITTO] Violazione di vincolo DB su {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "Conflitto dati",
                "L'operazione viola un vincolo di unicità o integrità dei dati.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Fallback per qualunque eccezione non gestita dagli handler specifici.
     * La traccia completa viene registrata solo nel log del server, mentre al
     * client viene restituito un messaggio generico privo di dettagli interni.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        // Livello ERROR per conservare la traccia completa lato server
        log.error("[ERRORE INTERNO] Eccezione non gestita su {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Errore interno del server",
                "Si è verificato un errore imprevisto. Riprova più tardi o contatta il supporto.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
