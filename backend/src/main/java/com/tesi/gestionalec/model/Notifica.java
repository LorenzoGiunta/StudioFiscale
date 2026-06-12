package com.tesi.gestionalec.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notifica destinata a un utente, generata dagli eventi rilevanti del sistema
 * tramite il pattern Observer. Ne conserva messaggio, tipo, stato di lettura e
 * destinatario.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"destinatario"})
public class Notifica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il messaggio è obbligatorio")
    @Column(nullable = false)
    private String messaggio;

    @NotNull(message = "Il tipo di notifica è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotifica tipo;      // CAMBIO_STATO, DOCUMENTO_CARICATO, SCADENZA

    private boolean letta;          // false di default — l'utente non l'ha ancora letta

    @NotNull(message = "Il destinatario è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente destinatario;    // chi riceve la notifica

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dataCreazione;
}
