package com.tesi.gestionalec.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Documento associato a una pratica.
 *
 * Ne traccia metadati, stato di revisione, versione, autore del caricamento ed
 * eventuale revisore. È soggetto a cancellazione logica: un filtro a livello di
 * entità ne esclude le occorrenze eliminate, preservandone lo storico fiscale.
 */
@Entity
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"pratica", "caricatoDa", "revisore"})
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome del documento è obbligatorio")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Il tipo di file è obbligatorio")
    @Column(nullable = false)
    private String tipoFile;

    @NotBlank(message = "Il percorso del file è obbligatorio")
    @Column(nullable = false)
    private String percorsoFile;

    @NotNull(message = "La dimensione del file è obbligatoria")
    @Positive(message = "La dimensione deve essere positiva")
    private Long dimensione;

    @NotNull(message = "Lo stato del documento è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoDocumento stato;

    private String motivazioneRifiuto;

    @NotNull(message = "La versione del documento è obbligatoria")
    @Positive(message = "La versione deve essere positiva")
    @Column(nullable = false)
    private Integer versione;

    @NotNull(message = "La pratica di riferimento è obbligatoria")
    @ManyToOne
    @JoinColumn(name = "pratica_id", nullable = false)
    private Pratica pratica;

    @NotNull(message = "L'utente che ha caricato il documento è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "caricato_da_id", nullable = false)
    private Cliente caricatoDa;

    @ManyToOne
    @JoinColumn(name = "revisore")
    private Collaboratore revisore;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dataCaricamento;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
