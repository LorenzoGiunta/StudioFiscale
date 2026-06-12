package com.tesi.gestionalec.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import java.time.LocalDateTime;


/**
 * Utente con ruolo di amministratore, responsabile della supervisione del
 * sistema. Conserva l'istante dell'ultima operazione amministrativa svolta.
 */
@Entity
@DiscriminatorValue("AMMINISTRATORE")
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Amministratore extends Utente {

    private LocalDateTime ultimaAzioneAmministrativa;

    @Override
    public Ruolo getRuolo() {
        return Ruolo.AMMINISTRATORE;
    }
}
