package com.tesi.gestionalec.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Messaggio scambiato nella chat interna tra due utenti. Conserva mittente,
 * destinatario, testo, stato di lettura e istante di invio.
 */
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MessaggioChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Il mittente è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "mittente_id", nullable = false)
    private Utente mittente;

    @NotNull(message = "Il destinatario è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Utente destinatario;

    @NotBlank(message = "Il testo del messaggio è obbligatorio")
    @Column(nullable = false)
    private String testo;

    private boolean letto = false;

    @CreationTimestamp
    private LocalDateTime dataInvio;
}