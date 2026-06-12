package com.tesi.gestionalec.model;


import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Utente con ruolo di commercialista, titolare dello studio. È identificato dal
 * numero di albo e gestisce clienti, pratiche e gli inviti con cui coinvolge i
 * collaboratori.
 */
@Entity
@DiscriminatorValue("COMMERCIALISTA")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Commercialista extends Utente {

    @NotBlank(message = "Il numero di albo è obbligatorio")
    private String numeroAlbo;

    @OneToMany(mappedBy = "commercialista", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvitoCollaborazione> inviti = new ArrayList<>();

    /** Collaboratori con cui esiste una collaborazione accettata. */
    public List<Collaboratore> getCollaboratori() {
        return inviti.stream()
                .filter(i -> i.getStato() == StatoInvito.ACCEPTED)
                .map(InvitoCollaborazione::getCollaboratore)
                .toList();
    }

    @Override
    public Ruolo getRuolo() {
        return Ruolo.COMMERCIALISTA;
    }
}
