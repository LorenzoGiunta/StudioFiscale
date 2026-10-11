package com.tesi.gestionalec.service;

import com.tesi.gestionalec.exception.TokenResetNonValidoException;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.TokenResetPassword;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.TokenResetPasswordRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.impl.EmailService;
import com.tesi.gestionalec.service.impl.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per PasswordResetServiceImpl: generazione e invio del token,
 * reimpostazione con token valido, rifiuto di token inesistenti/usati/scaduti.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService – Unit Tests")
class PasswordResetServiceImplTest {

    @Mock UtenteRepo utenteRepo;
    @Mock TokenResetPasswordRepo tokenRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock GestoreNotifiche gestoreNotifiche;

    @InjectMocks
    PasswordResetServiceImpl service;

    private Cliente utente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "durataMinuti", 60L);

        utente = new Cliente();
        utente.setId(4L);
        utente.setNome("Mario");
        utente.setCognome("Verdi");
        utente.setEmail("mario.verdi@gmail.com");
        utente.setPassword("vecchio-hash");
    }

    private TokenResetPassword tokenPer(Cliente u, LocalDateTime scadeIl) {
        TokenResetPassword t = new TokenResetPassword();
        t.setToken("tok-123");
        t.setUtente(u);
        t.setScadeIl(scadeIl);
        return t;
    }

    // ─── richiediReset ────────────────────────────────────────────────────────

    @Test
    @DisplayName("richiediReset: invalida i token pendenti, ne salva uno nuovo e invia l'email col link")
    void richiediReset_generaTokenEInviaEmail() {
        when(utenteRepo.findByEmail("mario.verdi@gmail.com")).thenReturn(Optional.of(utente));

        service.richiediReset("mario.verdi@gmail.com");

        verify(tokenRepo).deleteByUtenteAndUsatoIlIsNull(utente);

        ArgumentCaptor<TokenResetPassword> salvato = ArgumentCaptor.forClass(TokenResetPassword.class);
        verify(tokenRepo).save(salvato.capture());
        TokenResetPassword t = salvato.getValue();
        assertThat(t.getUtente()).isSameAs(utente);
        assertThat(t.getToken()).isNotBlank();
        assertThat(t.getScadeIl()).isAfter(LocalDateTime.now().plusMinutes(59));
        assertThat(t.isUsato()).isFalse();

        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(emailService).inviaEmail(eq("mario.verdi@gmail.com"), contains("Recupero password"), corpo.capture());
        assertThat(corpo.getValue()).contains("http://localhost:5173/reset-password/" + t.getToken());
    }

    @Test
    @DisplayName("richiediReset: email non registrata → nessun errore, nessun token, nessuna email")
    void richiediReset_emailSconosciuta_silenzioso() {
        when(utenteRepo.findByEmail("nessuno@test.it")).thenReturn(Optional.empty());

        assertThatCode(() -> service.richiediReset("nessuno@test.it")).doesNotThrowAnyException();

        verify(tokenRepo, never()).save(any());
        verify(emailService, never()).inviaEmail(any(), any(), any());
    }

    // ─── reimpostaPassword ────────────────────────────────────────────────────

    @Test
    @DisplayName("reimpostaPassword: token valido → nuova password cifrata, token consumato, utente avvisato")
    void reimpostaPassword_tokenValido() {
        TokenResetPassword t = tokenPer(utente, LocalDateTime.now().plusMinutes(30));
        when(tokenRepo.findByToken("tok-123")).thenReturn(Optional.of(t));
        when(passwordEncoder.encode("NuovaPass1!")).thenReturn("nuovo-hash");

        service.reimpostaPassword("tok-123", "NuovaPass1!");

        assertThat(utente.getPassword()).isEqualTo("nuovo-hash");
        verify(utenteRepo).save(utente);
        assertThat(t.isUsato()).isTrue();
        verify(tokenRepo).save(t);

        ArgumentCaptor<Notifica> notifica = ArgumentCaptor.forClass(Notifica.class);
        verify(gestoreNotifiche).notificaTutti(notifica.capture());
        assertThat(notifica.getValue().getTipo()).isEqualTo(TipoNotifica.PASSWORD_MODIFICATA);
        assertThat(notifica.getValue().getDestinatario()).isSameAs(utente);
    }

    @Test
    @DisplayName("reimpostaPassword: token inesistente → TokenResetNonValidoException")
    void reimpostaPassword_tokenInesistente() {
        when(tokenRepo.findByToken("boh")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reimpostaPassword("boh", "NuovaPass1!"))
                .isInstanceOf(TokenResetNonValidoException.class);
        verify(utenteRepo, never()).save(any());
    }

    @Test
    @DisplayName("reimpostaPassword: token scaduto → respinto, password invariata")
    void reimpostaPassword_tokenScaduto() {
        TokenResetPassword t = tokenPer(utente, LocalDateTime.now().minusMinutes(1));
        when(tokenRepo.findByToken("tok-123")).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.reimpostaPassword("tok-123", "NuovaPass1!"))
                .isInstanceOf(TokenResetNonValidoException.class);
        assertThat(utente.getPassword()).isEqualTo("vecchio-hash");
        verify(utenteRepo, never()).save(any());
        verify(gestoreNotifiche, never()).notificaTutti(any());
    }

    @Test
    @DisplayName("reimpostaPassword: token già usato → respinto (monouso)")
    void reimpostaPassword_tokenGiaUsato() {
        TokenResetPassword t = tokenPer(utente, LocalDateTime.now().plusMinutes(30));
        t.setUsatoIl(LocalDateTime.now().minusMinutes(5));
        when(tokenRepo.findByToken("tok-123")).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.reimpostaPassword("tok-123", "NuovaPass1!"))
                .isInstanceOf(TokenResetNonValidoException.class);
        verify(utenteRepo, never()).save(any());
    }

    // ─── pulizia ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminaTokenScaduti: delega al repository la cancellazione di quelli scaduti")
    void eliminaTokenScaduti_delegaAlRepo() {
        when(tokenRepo.deleteByScadeIlBefore(any())).thenReturn(3L);

        service.eliminaTokenScaduti();

        verify(tokenRepo).deleteByScadeIlBefore(any(LocalDateTime.class));
    }
}
