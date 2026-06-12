package com.tesi.gestionalec.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entità di associazione tra commercialista e collaboratore.
 *
 * Rappresenta l'invito e il relativo stato lungo il suo ciclo di vita (in attesa,
 * accettato, rifiutato o scaduto). Il riferimento al collaboratore resta vuoto
 * finché il destinatario non risulta registrato. Un vincolo di unicità impedisce
 * inviti duplicati per la stessa terna commercialista, email e stato.
 */
@Entity
@Table(
    name = "invito_collaborazione",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_invito_comm_email_stato",
        columnNames = {"commercialista_id", "email_destinatario", "stato"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"commercialista", "collaboratore"})
public class InvitoCollaborazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercialista_id", nullable = false)
    private Commercialista commercialista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collaboratore_id")
    private Collaboratore collaboratore;

    @Column(name = "email_destinatario", nullable = false)
    private String emailDestinatario;

    // UUID per il link in email (accettazione/rifiuto)
    @Column(unique = true, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoInvito stato;

    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private LocalDateTime creatoIl;

    // Scadenza: +7 giorni dalla creazione (settata nel service)
    @Column(name = "scade_il", nullable = false)
    private LocalDateTime scadeIl;
}
