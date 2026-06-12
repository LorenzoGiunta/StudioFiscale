package com.tesi.gestionalec.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Utente con ruolo di collaboratore, che affianca uno o più commercialisti nella
 * revisione di documenti e nella lavorazione delle pratiche assegnate. La
 * relazione con i commercialisti è mediata dagli inviti di collaborazione.
 */
@Entity
@DiscriminatorValue("COLLABORATORE")
@Getter
@Setter
@ToString(exclude = {"praticheAssegnate" , "documentiInRevisione"})
@AllArgsConstructor
@NoArgsConstructor
public class Collaboratore extends Utente {

    @OneToMany(mappedBy = "assegnataA", cascade = CascadeType.ALL)
    private List<Pratica> praticheAssegnate;

    @OneToMany(mappedBy = "revisore", cascade = CascadeType.ALL)
    private List<Documento> documentiInRevisione;

    @OneToMany(mappedBy = "collaboratore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvitoCollaborazione> associazioni = new ArrayList<>();

    /** Commercialisti con cui esiste una collaborazione accettata. */
    public List<Commercialista> getCommercialisti() {
        return associazioni.stream()
                .filter(i -> i.getStato() == StatoInvito.ACCEPTED)
                .map(InvitoCollaborazione::getCommercialista)
                .toList();
    }

    @Override
    public Ruolo getRuolo() {
        return Ruolo.COLLABORATORE;
    }
}
