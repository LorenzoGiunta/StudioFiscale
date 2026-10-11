package com.tesi.gestionalec;

import com.tesi.gestionalec.model.Amministratore;
import com.tesi.gestionalec.model.Commercialista;
import com.tesi.gestionalec.model.StatoRegistrazione;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.AmministratoreService;
import com.tesi.gestionalec.service.interfaces.UtenteService;
import com.tesi.gestionalec.state.BozzaState;
import com.tesi.gestionalec.state.InLavorazioneState;
import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.TipoPratica;
import com.tesi.gestionalec.repository.PraticaRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'integrazione su un MySQL reale avviato in container da Testcontainers.
 *
 * Copre ciò che i test unitari (tutti su mock) non possono raggiungere:
 * l'avvio del contesto Spring, le migrazioni Flyway, la validazione dello
 * schema da parte di Hibernate e le mappature JPA (ereditarietà JOINED, enum,
 * converter dello stato pratica) esercitate su query vere.
 *
 * Richiede Docker in esecuzione; in sua assenza il test viene saltato, così la
 * suite unitaria resta eseguibile ovunque.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        // Le variabili che in produzione arrivano dall'ambiente
        "DB_PASSWORD=test",
        "JWT_SECRET=segreto-di-test-lungo-almeno-trentadue-caratteri-ok",
        "app.email.enabled=false"
})
class GestionalecApplicationTests {

    @Container
    @ServiceConnection   // Spring Boot ricava url/username/password dal container
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired UtenteRepo utenteRepo;
    @Autowired PraticaRepo praticaRepo;
    @Autowired UtenteService utenteService;
    @Autowired AmministratoreService amministratoreService;

    @Test
    @DisplayName("Il contesto parte, Flyway applica schema e seed, Hibernate valida le entità")
    void contextLoads_eFlywayHaPopolatoIlDatabase() {
        // V2__dati_iniziali.sql inserisce i 5 account di demo
        assertThat(utenteRepo.count()).isEqualTo(5);
        assertThat(utenteRepo.findByEmail("lorenzo.giunta@studiofiscale.it"))
                .isPresent()
                .get().isInstanceOf(Amministratore.class);
    }

    @Test
    @DisplayName("Iscrizione commercialista: nasce IN_ATTESA, l'admin la approva, il DB riflette entrambi gli stati")
    void iscrizioneCommercialista_approvataDallAdmin() {
        Commercialista comm = new Commercialista();
        comm.setNome("Giulia");
        comm.setCognome("Integrazione");
        comm.setEmail("giulia.integrazione@test.it");
        comm.setPassword("Password1!");
        comm.setNumeroAlbo("77777/I");

        Utente salvato = utenteService.registra(comm);

        Utente riletto = utenteRepo.findById(salvato.getId()).orElseThrow();
        assertThat(riletto.getStatoRegistrazione()).isEqualTo(StatoRegistrazione.IN_ATTESA);
        assertThat(riletto.isEnabled()).isFalse();
        assertThat(utenteRepo.findByStatoRegistrazioneOrderByDataRegistrazioneAsc(StatoRegistrazione.IN_ATTESA))
                .extracting(Utente::getEmail).contains("giulia.integrazione@test.it");

        Utente admin = utenteRepo.findByEmail("lorenzo.giunta@studiofiscale.it").orElseThrow();
        amministratoreService.approvaIscrizione(salvato.getId(), admin);

        Utente approvato = utenteRepo.findById(salvato.getId()).orElseThrow();
        assertThat(approvato.getStatoRegistrazione()).isEqualTo(StatoRegistrazione.APPROVATA);
        assertThat(approvato.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Pattern State su MySQL: lo stato è persistito come codice e riletto come oggetto")
    void statoPratica_persistitoERiletto() {
        Cliente cliente = (Cliente) utenteRepo.findByEmail("mario.verdi@gmail.com").orElseThrow();

        Pratica pratica = new Pratica();
        pratica.setCliente(cliente);
        pratica.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        Long id = praticaRepo.save(pratica).getId();

        assertThat(praticaRepo.findById(id).orElseThrow().getStatoCorrente()).isSameAs(BozzaState.INSTANCE);

        Pratica daAvanzare = praticaRepo.findById(id).orElseThrow();
        daAvanzare.avanza();
        praticaRepo.save(daAvanzare);

        assertThat(praticaRepo.findById(id).orElseThrow().getStatoCorrente()).isSameAs(InLavorazioneState.INSTANCE);
        assertThat(praticaRepo.findByStatoCorrente(InLavorazioneState.INSTANCE))
                .extracting(Pratica::getId).contains(id);
    }
}
