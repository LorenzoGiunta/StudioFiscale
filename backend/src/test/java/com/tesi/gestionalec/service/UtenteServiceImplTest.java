package com.tesi.gestionalec.service;

import com.tesi.gestionalec.model.*;
import com.tesi.gestionalec.repository.*;
import com.tesi.gestionalec.service.impl.UtenteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per UtenteServiceImpl.
 * Copre: registrazione (hash password + enabled), ricerca, abilitazione,
 * disabilitazione, eliminazione e loadUserByUsername (Spring Security).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UtenteService – Unit Tests")
class UtenteServiceImplTest {

    @Mock UtenteRepo repo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CommercialistaRepo commercialistaRepo;

    @InjectMocks
    UtenteServiceImpl utenteService;

    private Cliente utente;

    @BeforeEach
    void setUp() {
        utente = new Cliente();
        utente.setId(1L);
        utente.setNome("Mario");
        utente.setCognome("Rossi");
        utente.setEmail("mario@studio.it");
        utente.setPassword("plaintextPassword");
        utente.setEnabled(true);
    }

    // ─── registra ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("registra: cifra la password prima di salvare")
    void registra_cifraPassword() {
        when(passwordEncoder.encode("plaintextPassword")).thenReturn("hashedPwd");
        when(repo.save(any())).thenReturn(utente);

        utenteService.registra(utente);

        assertThat(utente.getPassword()).isEqualTo("hashedPwd");
        verify(passwordEncoder).encode("plaintextPassword");
    }

    @Test
    @DisplayName("registra: imposta enabled=true automaticamente")
    void registra_impostaEnabledTrue() {
        utente.setEnabled(false);  // anche se arriva false, deve diventare true
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(repo.save(any())).thenReturn(utente);

        utenteService.registra(utente);

        assertThat(utente.isEnabled()).isTrue();
        verify(repo).save(utente);
    }

    @Test
    @DisplayName("registra(utente, commId): associa il commercialista al cliente")
    void registra_conCommercialista_associaCommercialista() {
        Commercialista comm = new Commercialista();
        comm.setId(10L);
        when(repo.countByEmailIncludeDeleted(utente.getEmail())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(commercialistaRepo.findById(10L)).thenReturn(Optional.of(comm));
        when(repo.save(any())).thenReturn(utente);

        utenteService.registra(utente, 10L);

        assertThat(utente.getCommercialista()).isEqualTo(comm);
        verify(commercialistaRepo).findById(10L);
    }

    @Test
    @DisplayName("registra(utente, commId): commercialista non trovato → ResourceNotFoundException")
    void registra_conCommercialista_nonTrovato_lanciaEccezione() {
        when(repo.countByEmailIncludeDeleted(utente.getEmail())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(commercialistaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utenteService.registra(utente, 99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("registra: email già presente (include deleted) → EmailAlreadyExistsException")
    void registra_emailDuplicata_lanciaEccezione() {
        when(repo.countByEmailIncludeDeleted(utente.getEmail())).thenReturn(1L);

        assertThatThrownBy(() -> utenteService.registra(utente, null))
                .isInstanceOf(com.tesi.gestionalec.exception.EmailAlreadyExistsException.class);
        verify(repo, never()).save(any());
    }

    // ─── validaPerRuolo ───────────────────────────────────────────────────────

    @Test
    @DisplayName("validaPerRuolo: CLIENTE senza codFiscale → IllegalArgumentException")
    void validaPerRuolo_cliente_senzaCodFiscale_lanciaEccezione() {
        com.tesi.gestionalec.dto.request.RegistrazioneRequest req = new com.tesi.gestionalec.dto.request.RegistrazioneRequest();
        req.setRuolo(Ruolo.CLIENTE);
        req.setCodFiscale(null);

        assertThatThrownBy(() -> utenteService.validaPerRuolo(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codice fiscale");
    }

    @Test
    @DisplayName("validaPerRuolo: CLIENTE senza regime → IllegalArgumentException")
    void validaPerRuolo_cliente_senzaRegime_lanciaEccezione() {
        com.tesi.gestionalec.dto.request.RegistrazioneRequest req = new com.tesi.gestionalec.dto.request.RegistrazioneRequest();
        req.setRuolo(Ruolo.CLIENTE);
        req.setCodFiscale("RSSMRO80A01H501Z");
        req.setRegime(null);

        assertThatThrownBy(() -> utenteService.validaPerRuolo(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regime");
    }

    @Test
    @DisplayName("validaPerRuolo: COMMERCIALISTA senza numeroAlbo → IllegalArgumentException")
    void validaPerRuolo_commercialista_senzaAlbo_lanciaEccezione() {
        com.tesi.gestionalec.dto.request.RegistrazioneRequest req = new com.tesi.gestionalec.dto.request.RegistrazioneRequest();
        req.setRuolo(Ruolo.COMMERCIALISTA);
        req.setNumeroAlbo(null);

        assertThatThrownBy(() -> utenteService.validaPerRuolo(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("albo");
    }

    @Test
    @DisplayName("validaPerRuolo: CLIENTE con tutti i campi → nessuna eccezione")
    void validaPerRuolo_cliente_ok_nessuna_eccezione() {
        com.tesi.gestionalec.dto.request.RegistrazioneRequest req = new com.tesi.gestionalec.dto.request.RegistrazioneRequest();
        req.setRuolo(Ruolo.CLIENTE);
        req.setCodFiscale("RSSMRO80A01H501Z");
        req.setRegime("ORDINARIO");

        assertThatCode(() -> utenteService.validaPerRuolo(req)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validaPerRuolo: COLLABORATORE → nessuna eccezione")
    void validaPerRuolo_collaboratore_nessuna_eccezione() {
        com.tesi.gestionalec.dto.request.RegistrazioneRequest req = new com.tesi.gestionalec.dto.request.RegistrazioneRequest();
        req.setRuolo(Ruolo.COLLABORATORE);

        assertThatCode(() -> utenteService.validaPerRuolo(req)).doesNotThrowAnyException();
    }

    // ─── trovaPerId ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerId: restituisce l'utente se esiste")
    void trovaPerId_esistente() {
        when(repo.findById(1L)).thenReturn(Optional.of(utente));

        Utente trovato = utenteService.trovaPerId(1L);

        assertThat(trovato.getEmail()).isEqualTo("mario@studio.it");
    }

    @Test
    @DisplayName("trovaPerId: lancia RuntimeException se non esiste")
    void trovaPerId_nonEsistente_lanciaEccezione() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utenteService.trovaPerId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── trovaPerEmail ────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaPerEmail: restituisce l'utente se esiste")
    void trovaPerEmail_esistente() {
        when(repo.findByEmail("mario@studio.it")).thenReturn(Optional.of(utente));

        Utente trovato = utenteService.trovaPerEmail("mario@studio.it");

        assertThat(trovato.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("trovaPerEmail: lancia RuntimeException se non esiste")
    void trovaPerEmail_nonEsistente_lanciaEccezione() {
        when(repo.findByEmail("ghost@x.it")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utenteService.trovaPerEmail("ghost@x.it"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ghost@x.it");
    }

    // ─── trovaTutti ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaTutti: delega a repo.findAll()")
    void trovaTutti_delegaAlRepo() {
        when(repo.findAll()).thenReturn(List.of(utente));

        List<Utente> result = utenteService.trovaTutti();

        assertThat(result).hasSize(1);
        verify(repo).findAll();
    }

    @Test
    @DisplayName("trovaTutti(Pageable): delega a repo.findAll(pageable)")
    void trovaTutti_pageable_delegaAlRepo() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Utente> pagina =
                new org.springframework.data.domain.PageImpl<>(List.of(utente));
        when(repo.findAll(pageable)).thenReturn(pagina);

        org.springframework.data.domain.Page<Utente> result = utenteService.trovaTutti(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(repo).findAll(pageable);
    }

    // ─── trovaEliminati ───────────────────────────────────────────────────────

    @Test
    @DisplayName("trovaEliminati: delega a repo.findAllDeleted()")
    void trovaEliminati_delegaAlRepo() {
        Cliente eliminato = new Cliente();
        eliminato.setDeleted(true);
        when(repo.findAllDeleted()).thenReturn(List.of(eliminato));

        List<Utente> result = utenteService.trovaEliminati();

        assertThat(result).hasSize(1);
        verify(repo).findAllDeleted();
    }

    // ─── abilitaUtente ────────────────────────────────────────────────────────

    @Test
    @DisplayName("abilitaUtente: imposta enabled=true e salva")
    void abilitaUtente_impostaEnabledTrueESalva() {
        utente.setEnabled(false);
        when(repo.findById(1L)).thenReturn(Optional.of(utente));

        utenteService.abilitaUtente(1L);

        assertThat(utente.isEnabled()).isTrue();
        verify(repo).save(utente);
    }

    // ─── disabilitaUtente ─────────────────────────────────────────────────────

    @Test
    @DisplayName("disabilitaUtente: imposta enabled=false e salva")
    void disabilitaUtente_impostaEnabledFalseESalva() {
        when(repo.findById(1L)).thenReturn(Optional.of(utente));

        utenteService.disabilitaUtente(1L);

        assertThat(utente.isEnabled()).isFalse();
        verify(repo).save(utente);
    }

    // ─── eliminaUtente ────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminaUtente: esegue soft-delete (deleted=true, enabled=false) e salva")
    void eliminaUtente_chiamaDeleteById() {
        when(repo.findById(1L)).thenReturn(Optional.of(utente));

        utenteService.eliminaUtente(1L);

        assertThat(utente.isDeleted()).isTrue();
        assertThat(utente.isEnabled()).isFalse();
        verify(repo).save(utente);
    }

    @Test
    @DisplayName("eliminaUtente: lancia eccezione se utente non trovato")
    void eliminaUtente_nonEsistente_lanciaEccezione() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utenteService.eliminaUtente(99L))
                .isInstanceOf(RuntimeException.class);
        verify(repo, never()).deleteById(any());
    }

    // ─── ripristinaUtente ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ripristinaUtente: reimposta deleted=false, enabled=true e salva")
    void ripristina_reimpostaStatoESalva() {
        utente.setDeleted(true);
        utente.setEnabled(false);
        when(repo.findByIdIncludeDeleted(1L)).thenReturn(Optional.of(utente));

        utenteService.ripristinaUtente(1L);

        assertThat(utente.isDeleted()).isFalse();
        assertThat(utente.isEnabled()).isTrue();
        assertThat(utente.getDeletedAt()).isNull();
        verify(repo).save(utente);
    }

    @Test
    @DisplayName("ripristinaUtente: utente non trovato → ResourceNotFoundException")
    void ripristina_nonTrovato_lanciaEccezione() {
        when(repo.findByIdIncludeDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utenteService.ripristinaUtente(99L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── loadUserByUsername ───────────────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername: restituisce UserDetails se email esiste")
    void loadUserByUsername_esistente() {
        when(repo.findByEmail("mario@studio.it")).thenReturn(Optional.of(utente));

        var userDetails = utenteService.loadUserByUsername("mario@studio.it");

        assertThat(userDetails.getUsername()).isEqualTo("mario@studio.it");
    }

    @Test
    @DisplayName("loadUserByUsername: lancia UsernameNotFoundException se non esiste")
    void loadUserByUsername_nonEsistente_lanciaEccezione() {
        when(repo.findByEmail("ghost@x.it")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utenteService.loadUserByUsername("ghost@x.it"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
