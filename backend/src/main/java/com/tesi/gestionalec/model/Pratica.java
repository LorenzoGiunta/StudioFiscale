package com.tesi.gestionalec.model;


import com.tesi.gestionalec.state.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Pratica gestita per conto di un cliente.
 *
 * Possiede un tipo, una scadenza e uno stato che evolve secondo il pattern State
 * tramite l'oggetto di stato che ne incapsula il comportamento. È collegata al
 * cliente, all'eventuale collaboratore assegnatario e ai documenti. È soggetta
 * a cancellazione logica: un filtro a livello di entità ne esclude le occorrenze eliminate, conservando
 * lo storico per l'audit fiscale.
 */
@Entity
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"listaDocumenti", "cliente", "assegnataA"})
public class Pratica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Il cliente è obbligatorio")
    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotNull(message = "Il tipo di pratica è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPratica tipoPratica;

    // Pattern State: lo stato È l'oggetto che ne incapsula il comportamento.
    // Non esiste un codice affiancato da tenere in sincronia: il converter
    // traduce l'oggetto da e verso la colonna 'stato' del database.
    @NotNull(message = "Lo stato della pratica è obbligatorio")
    @Convert(converter = StatoPraticaConverter.class)
    @Column(name = "stato", nullable = false)
    private StatoPraticaState statoCorrente = BozzaState.INSTANCE;

    @ManyToOne
    @JoinColumn(name = "assegnata_a_id")
    private Collaboratore assegnataA;

    private LocalDate scadenza;

    @OneToMany(mappedBy = "pratica", cascade = CascadeType.ALL)
    private List<Documento> listaDocumenti;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Context del pattern State: la richiesta di avanzamento è delegata allo
     * stato corrente, che decide se la transizione è ammessa e verso quale
     * stato portare la pratica. I chiamanti si rivolgono solo a questo metodo,
     * senza conoscere le classi di stato.
     */
    public void avanza() {
        statoCorrente.avanza(this);
    }
}
