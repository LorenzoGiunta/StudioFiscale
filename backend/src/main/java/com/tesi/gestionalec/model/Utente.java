package com.tesi.gestionalec.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entità base della gerarchia degli utenti.
 *
 * Le diverse tipologie (cliente, commercialista, collaboratore, amministratore)
 * sono modellate come sottoclassi mappate con strategia di ereditarietà su
 * tabelle distinte e distinte da una colonna discriminante. Implementa il
 * contratto di Spring Security per integrarsi con l'autenticazione. Adotta la
 * cancellazione logica: un filtro a livello di entità esclude automaticamente
 * dalle query i record marcati come eliminati, preservandone lo storico.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "ruolo", discriminatorType = DiscriminatorType.STRING)
@SQLRestriction("deleted = false")
@Getter
@Setter
@ToString
public abstract class Utente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    @NotBlank(message = "Il cognome è obbligatorio")
    private String cognome;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Column(nullable = false)
    @ToString.Exclude   // l'hash non deve finire nei log
    private String password;

    private boolean enabled;

    // Esito della verifica amministrativa sull'iscrizione. Per i ruoli che non
    // la richiedono è APPROVATA fin dalla nascita; per gli altri resta IN_ATTESA
    // (con enabled=false) finché un amministratore non decide.
    @Enumerated(EnumType.STRING)
    @Column(name = "stato_registrazione", nullable = false)
    private StatoRegistrazione statoRegistrazione = StatoRegistrazione.APPROVATA;

    @CreationTimestamp
    @Column(name = "data_registrazione", updatable = false)
    private LocalDateTime dataRegistrazione;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "ruolo", insertable = false, updatable = false)
    private String ruoloDiscriminante;

    public abstract Ruolo getRuolo();

    /**
     * Indica se l'iscrizione con questo ruolo deve essere verificata da un
     * amministratore prima che l'account possa accedere. Le sottoclassi che
     * lo richiedono sovrascrivono questo metodo.
     */
    public boolean richiedeApprovazione() {
        return false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Ruolo r = getRuolo();
        if (r == null) return List.of();   // r può essere null prima che la subclass sia idratata
        return List.of(new SimpleGrantedAuthority("ROLE_" + r.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }
}