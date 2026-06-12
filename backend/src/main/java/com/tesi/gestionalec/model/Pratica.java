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
 * tramite lo stato corrente associato. È collegata al cliente, all'eventuale
 * collaboratore assegnatario e ai documenti. È soggetta a cancellazione logica:
 * un filtro a livello di entità ne esclude le occorrenze eliminate, conservando
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

    @NotNull(message = "Lo stato della pratica è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoPratica stato;

    @ManyToOne
    @JoinColumn(name = "assegnata_a_id")
    private Collaboratore assegnataA;

    private LocalDate scadenza;

    @OneToMany(mappedBy = "pratica", cascade = CascadeType.ALL)
    private List<Documento> listaDocumenti;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    // State Pattern: oggetto in memoria, non persistito (ricostruito in @PostLoad)
    @Transient
    private StatoPraticaState statoCorrente;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PostLoad
    private void inizializzaStato() {
        this.statoCorrente = switch (this.stato) {
            case BOZZA               -> new BozzaState();
            case IN_LAVORAZIONE      -> new InLavorazioneState();
            case IN_ATTESA_DOCUMENTI -> new InAttesaDocumentiState();
            case COMPLETATA          -> new CompletataState();
        };
    }
}
