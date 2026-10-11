package com.tesi.gestionalec.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token monouso per il recupero della password.
 *
 * Viene generato su richiesta dell'utente, recapitato via email come link e
 * accettato una sola volta entro la scadenza. Ogni nuova richiesta invalida i
 * token precedenti ancora pendenti dello stesso utente; quelli scaduti vengono
 * ripuliti periodicamente.
 */
@Entity
@Table(name = "token_reset_password")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "utente")
public class TokenResetPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String token;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private LocalDateTime creatoIl;

    @Column(name = "scade_il", nullable = false)
    private LocalDateTime scadeIl;

    /** Istante in cui il token è stato consumato; {@code null} finché è pendente. */
    @Column(name = "usato_il")
    private LocalDateTime usatoIl;

    public boolean isScaduto() {
        return LocalDateTime.now().isAfter(scadeIl);
    }

    public boolean isUsato() {
        return usatoIl != null;
    }

    /** Un token è utilizzabile solo se non è già stato consumato e non è scaduto. */
    public boolean isValido() {
        return !isUsato() && !isScaduto();
    }
}
