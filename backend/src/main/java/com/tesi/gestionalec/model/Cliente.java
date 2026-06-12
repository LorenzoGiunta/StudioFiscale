package com.tesi.gestionalec.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.util.List;

/**
 * Utente con ruolo di cliente, destinatario dei servizi dello studio. Conserva i
 * dati fiscali (codice fiscale, partita IVA, regime e reddito) necessari al
 * calcolo delle imposte ed è collegato al commercialista di riferimento, alle
 * proprie pratiche e ai documenti caricati.
 */
@Entity
@DiscriminatorValue("CLIENTE")
@Getter
@Setter
@ToString(exclude = {"pratiche" , "documenti"})
@AllArgsConstructor
@NoArgsConstructor
public class Cliente extends Utente {

    @Pattern(regexp = "^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$", message = "Codice fiscale non valido")
    @Column(unique = true)
    private String codFiscale;

    @Pattern(regexp = "^[0-9]{11}$", message = "Partita IVA non valida")
    private String pIVA;

    @Enumerated(EnumType.STRING)
    private RegimeFiscale regime;

    @PositiveOrZero(message = "Il reddito annuo non può essere negativo")
    private Double redditoAnnuo;

    /** Il commercialista scelto dal cliente in fase di registrazione. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercialista_id")
    private Commercialista commercialista;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Pratica> pratiche;

    @OneToMany(mappedBy = "caricatoDa", cascade = CascadeType.ALL)
    private List<Documento> documenti;

    @Override
    public Ruolo getRuolo() {
        return Ruolo.CLIENTE;
    }
}
