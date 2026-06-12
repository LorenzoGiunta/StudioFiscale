package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.request.DocumentoRequest;
import com.tesi.gestionalec.dto.request.PraticaRequest;
import com.tesi.gestionalec.dto.request.RegistrazioneRequest;
import com.tesi.gestionalec.dto.response.*;
import com.tesi.gestionalec.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitari per tutti i Mapper.
 * Test puri: nessuna dipendenza Spring o DB — solo logica di mapping.
 */
@DisplayName("Mappers – Unit Tests")
class MapperTest {

    // ═══════════════════════════════════════════════════════════════════════
    // PraticaMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PraticaMapper.toResponse: mappa tutti i campi base correttamente")
    void praticaMapper_toResponse_campiBase() {
        Cliente cliente = new Cliente();
        cliente.setNome("Mario");
        cliente.setCognome("Rossi");

        Pratica pratica = new Pratica();
        pratica.setId(1L);
        pratica.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        pratica.setStato(StatoPratica.BOZZA);
        pratica.setCliente(cliente);
        pratica.setScadenza(LocalDate.of(2024, 12, 31));

        PraticaResponse dto = PraticaMapper.toResponse(pratica);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTipoPratica()).isEqualTo(TipoPratica.DICHIARAZIONE_REDDITI);
        assertThat(dto.getStato()).isEqualTo(StatoPratica.BOZZA);
        assertThat(dto.getNomeCliente()).isEqualTo("Mario Rossi");
        assertThat(dto.getScadenza()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test
    @DisplayName("PraticaMapper.toResponse: collaboratore null → nomeCollaboratore null")
    void praticaMapper_toResponse_senzaCollaboratore() {
        Cliente c = clienteCon("Anna", "Verdi");
        Pratica pratica = praticaCon(c, null);

        PraticaResponse dto = PraticaMapper.toResponse(pratica);

        assertThat(dto.getNomeCollaboratore()).isNull();
    }

    @Test
    @DisplayName("PraticaMapper.toResponse: collaboratore presente → nomeCollaboratore settato")
    void praticaMapper_toResponse_conCollaboratore() {
        Cliente c = clienteCon("Anna", "Verdi");
        Collaboratore col = new Collaboratore();
        col.setNome("Luca");
        col.setCognome("Bianchi");

        Pratica pratica = praticaCon(c, col);
        PraticaResponse dto = PraticaMapper.toResponse(pratica);

        assertThat(dto.getNomeCollaboratore()).isEqualTo("Luca Bianchi");
    }

    @Test
    @DisplayName("PraticaMapper.toModel: mappa tipoPratica e cliente correttamente")
    void praticaMapper_toModel() {
        Cliente c = clienteCon("Mario", "Rossi");
        PraticaRequest request = new PraticaRequest();
        request.setTipoPratica(TipoPratica.IVA);
        request.setScadenza(LocalDate.of(2024, 6, 30));

        Pratica pratica = PraticaMapper.toModel(request, c);

        assertThat(pratica.getCliente()).isEqualTo(c);
        assertThat(pratica.getTipoPratica()).isEqualTo(TipoPratica.IVA);
        assertThat(pratica.getScadenza()).isEqualTo(LocalDate.of(2024, 6, 30));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DocumentoMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DocumentoMapper.toResponse: mappa tutti i campi correttamente")
    void documentoMapper_toResponse_campiBase() {
        Cliente cliente = clienteCon("Mario", "Rossi");
        Documento doc = new Documento();
        doc.setId(10L);
        doc.setNome("CUD_2024.pdf");
        doc.setTipoFile("CUD");
        doc.setDimensione(102400L);
        doc.setStato(StatoDocumento.IN_REVISIONE);
        doc.setVersione(1);
        doc.setCaricatoDa(cliente);

        DocumentoResponse dto = DocumentoMapper.toResponse(doc);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getNome()).isEqualTo("CUD_2024.pdf");
        assertThat(dto.getTipoFile()).isEqualTo("CUD");
        assertThat(dto.getDimensione()).isEqualTo(102400L);
        assertThat(dto.getStato()).isEqualTo(StatoDocumento.IN_REVISIONE);
        assertThat(dto.getVersione()).isEqualTo(1);
        assertThat(dto.getNomeCliente()).isEqualTo("Mario Rossi");
    }

    @Test
    @DisplayName("DocumentoMapper.toResponse: revisore null → nomeRevisore null")
    void documentoMapper_toResponse_senzaRevisore() {
        Documento doc = documentoBase();
        doc.setRevisore(null);

        DocumentoResponse dto = DocumentoMapper.toResponse(doc);

        assertThat(dto.getNomeRevisore()).isNull();
    }

    @Test
    @DisplayName("DocumentoMapper.toResponse: revisore presente → nomeRevisore settato")
    void documentoMapper_toResponse_conRevisore() {
        Collaboratore rev = new Collaboratore();
        rev.setNome("Luca");
        rev.setCognome("Bianchi");

        Documento doc = documentoBase();
        doc.setRevisore(rev);

        DocumentoResponse dto = DocumentoMapper.toResponse(doc);

        assertThat(dto.getNomeRevisore()).isEqualTo("Luca Bianchi");
    }

    @Test
    @DisplayName("DocumentoMapper.toModel: mappa nome, tipoFile, percorso e cliente")
    void documentoMapper_toModel() {
        DocumentoRequest req = new DocumentoRequest();
        req.setNome("Fattura.pdf");
        req.setTipoFile("FATTURA");
        req.setPercorsoFile("uploads/uuid_Fattura.pdf");
        req.setDimensione(50000L);

        Pratica pratica = new Pratica();
        pratica.setId(5L);
        Cliente cliente = clienteCon("Anna", "Verdi");

        Documento doc = DocumentoMapper.toModel(req, pratica, cliente);

        assertThat(doc.getNome()).isEqualTo("Fattura.pdf");
        assertThat(doc.getTipoFile()).isEqualTo("FATTURA");
        assertThat(doc.getPercorsoFile()).isEqualTo("uploads/uuid_Fattura.pdf");
        assertThat(doc.getPratica()).isEqualTo(pratica);
        assertThat(doc.getCaricatoDa()).isEqualTo(cliente);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UtenteMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UtenteMapper.toResponse: mappa tutti i campi correttamente")
    void utenteMapper_toResponse() {
        // Cliente.getRuolo() restituisce sempre Ruolo.CLIENTE (hardcoded nella sottoclasse)
        Cliente utente = new Cliente();
        utente.setId(99L);
        utente.setNome("Sara");
        utente.setCognome("Neri");
        utente.setEmail("sara@studio.it");
        utente.setEnabled(true);

        UtenteResponse dto = UtenteMapper.toResponse(utente);

        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getNome()).isEqualTo("Sara");
        assertThat(dto.getCognome()).isEqualTo("Neri");
        assertThat(dto.getEmail()).isEqualTo("sara@studio.it");
        assertThat(dto.getRuolo()).isEqualTo(Ruolo.CLIENTE);
        assertThat(dto.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UtenteMapper.toResponse: utente disabilitato → enabled false")
    void utenteMapper_toResponse_disabilitato() {
        Cliente utente = new Cliente();
        utente.setId(1L);
        utente.setNome("X");
        utente.setCognome("Y");
        utente.setEmail("x@y.it");
        utente.setEnabled(false);

        assertThat(UtenteMapper.toResponse(utente).isEnabled()).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NotificaMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("NotificaMapper.toResponse: mappa tutti i campi correttamente")
    void notificaMapper_toResponse() {
        LocalDateTime ora = LocalDateTime.now();
        Notifica n = new Notifica();
        n.setId(5L);
        n.setMessaggio("Pratica aggiornata");
        n.setTipo(TipoNotifica.CAMBIO_STATO);
        n.setLetta(false);
        n.setDataCreazione(ora);

        NotificaResponse dto = NotificaMapper.toResponse(n);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getMessaggio()).isEqualTo("Pratica aggiornata");
        assertThat(dto.getTipo()).isEqualTo(TipoNotifica.CAMBIO_STATO);
        assertThat(dto.isLetta()).isFalse();
        assertThat(dto.getDataCreazione()).isEqualTo(ora);
    }

    @Test
    @DisplayName("NotificaMapper.toResponse: notifica letta → letta=true nel DTO")
    void notificaMapper_toResponse_letta() {
        Notifica n = new Notifica();
        n.setId(6L);
        n.setLetta(true);
        n.setMessaggio("test");

        assertThat(NotificaMapper.toResponse(n).isLetta()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MessaggioChatMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("MessaggioChatMapper.toResponse: mappa mittente, destinatario, testo e flag letto")
    void messaggioMapper_toResponse() {
        Cliente mittente = new Cliente();
        mittente.setId(1L);
        mittente.setNome("Mario");
        mittente.setCognome("Rossi");

        Commercialista dest = new Commercialista();
        dest.setId(2L);
        dest.setNome("Giulia");
        dest.setCognome("Bianchi");

        MessaggioChat m = new MessaggioChat();
        m.setId(100L);
        m.setMittente(mittente);
        m.setDestinatario(dest);
        m.setTesto("Ciao, ho una domanda");
        m.setLetto(false);

        MessaggioChatResponse dto = MessaggioChatMapper.toResponse(m);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getMittenteId()).isEqualTo(1L);
        assertThat(dto.getMittenteNome()).isEqualTo("Mario Rossi");
        assertThat(dto.getDestinatarioId()).isEqualTo(2L);
        assertThat(dto.getDestinatarioNome()).isEqualTo("Giulia Bianchi");
        assertThat(dto.getTesto()).isEqualTo("Ciao, ho una domanda");
        assertThat(dto.isLetto()).isFalse();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Cliente clienteCon(String nome, String cognome) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setCognome(cognome);
        return c;
    }

    private Pratica praticaCon(Cliente cliente, Collaboratore collaboratore) {
        Pratica p = new Pratica();
        p.setId(1L);
        p.setTipoPratica(TipoPratica.DICHIARAZIONE_REDDITI);
        p.setStato(StatoPratica.BOZZA);
        p.setCliente(cliente);
        p.setAssegnataA(collaboratore);
        return p;
    }

    private Documento documentoBase() {
        Cliente c = clienteCon("Mario", "Rossi");
        Documento d = new Documento();
        d.setId(1L);
        d.setNome("test.pdf");
        d.setTipoFile("PDF");
        d.setDimensione(1000L);
        d.setStato(StatoDocumento.IN_REVISIONE);
        d.setVersione(1);
        d.setCaricatoDa(c);
        return d;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // InvitoMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("InvitoMapper.toResponse: mappa tutti i campi con collaboratore presente")
    void invitoMapper_toResponse_conCollaboratore() {
        Commercialista comm = new Commercialista();
        comm.setId(1L);
        comm.setNome("Giovanni");
        comm.setCognome("Verdi");
        comm.setNumeroAlbo("ALB001");

        Collaboratore collab = new Collaboratore();
        collab.setId(2L);
        collab.setNome("Luca");
        collab.setCognome("Bianchi");

        InvitoCollaborazione invito = new InvitoCollaborazione();
        invito.setId(10L);
        invito.setToken("tok-abc");
        invito.setEmailDestinatario("luca@studio.it");
        invito.setCommercialista(comm);
        invito.setCollaboratore(collab);
        invito.setStato(StatoInvito.PENDING);
        LocalDateTime ora = LocalDateTime.now();
        invito.setCreatoIl(ora);
        invito.setScadeIl(ora.plusDays(7));

        InvitoResponse dto = InvitoMapper.toResponse(invito);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getToken()).isEqualTo("tok-abc");
        assertThat(dto.getEmailDestinatario()).isEqualTo("luca@studio.it");
        assertThat(dto.getCommercialistaId()).isEqualTo(1L);
        assertThat(dto.getNomeCommercialista()).isEqualTo("Giovanni Verdi");
        assertThat(dto.getStudioCommercialista()).isEqualTo("ALB001");
        assertThat(dto.getCollaboratoreId()).isEqualTo(2L);
        assertThat(dto.getNomeCollaboratore()).isEqualTo("Luca Bianchi");
        assertThat(dto.getStato()).isEqualTo(StatoInvito.PENDING);
        assertThat(dto.getCreatoIl()).isEqualTo(ora);
    }

    @Test
    @DisplayName("InvitoMapper.toResponse: collaboratore null → campi collaboratore null")
    void invitoMapper_toResponse_senzaCollaboratore() {
        Commercialista comm = new Commercialista();
        comm.setId(1L);
        comm.setNome("Giovanni");
        comm.setCognome("Verdi");
        comm.setNumeroAlbo("ALB001");

        InvitoCollaborazione invito = new InvitoCollaborazione();
        invito.setId(11L);
        invito.setToken("tok-xyz");
        invito.setEmailDestinatario("nuovo@test.it");
        invito.setCommercialista(comm);
        invito.setCollaboratore(null);
        invito.setStato(StatoInvito.PENDING);
        invito.setCreatoIl(LocalDateTime.now());
        invito.setScadeIl(LocalDateTime.now().plusDays(7));

        InvitoResponse dto = InvitoMapper.toResponse(invito);

        assertThat(dto.getCollaboratoreId()).isNull();
        assertThat(dto.getNomeCollaboratore()).isNull();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RegistrazioneMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("RegistrazioneMapper.toModel: ruolo CLIENTE → Cliente con campi specifici")
    void registrazioneMapper_toModel_cliente() {
        RegistrazioneRequest req = new RegistrazioneRequest();
        req.setNome("Mario");
        req.setCognome("Rossi");
        req.setEmail("mario@test.it");
        req.setPassword("Password1!");
        req.setRuolo(Ruolo.CLIENTE);
        req.setCodFiscale("RSSMRO80A01H501Z");
        req.setPartitaIva("12345678901");
        req.setRedditoAnnuo(50000.0);
        req.setRegime("ORDINARIO");

        Utente utente = RegistrazioneMapper.toModel(req);

        assertThat(utente).isInstanceOf(Cliente.class);
        assertThat(utente.getNome()).isEqualTo("Mario");
        assertThat(utente.getEmail()).isEqualTo("mario@test.it");
        Cliente cliente = (Cliente) utente;
        assertThat(cliente.getCodFiscale()).isEqualTo("RSSMRO80A01H501Z");
        assertThat(cliente.getRegime()).isEqualTo(RegimeFiscale.ORDINARIO);
        assertThat(cliente.getRedditoAnnuo()).isEqualTo(50000.0);
    }

    @Test
    @DisplayName("RegistrazioneMapper.toModel: ruolo COMMERCIALISTA → Commercialista con numeroAlbo")
    void registrazioneMapper_toModel_commercialista() {
        RegistrazioneRequest req = new RegistrazioneRequest();
        req.setNome("Giulia");
        req.setCognome("Bianchi");
        req.setEmail("giulia@studio.it");
        req.setPassword("Password1!");
        req.setRuolo(Ruolo.COMMERCIALISTA);
        req.setNumeroAlbo("ALB9999");

        Utente utente = RegistrazioneMapper.toModel(req);

        assertThat(utente).isInstanceOf(Commercialista.class);
        Commercialista comm = (Commercialista) utente;
        assertThat(comm.getNumeroAlbo()).isEqualTo("ALB9999");
        assertThat(comm.getNome()).isEqualTo("Giulia");
    }

    @Test
    @DisplayName("RegistrazioneMapper.toModel: ruolo COLLABORATORE → Collaboratore")
    void registrazioneMapper_toModel_collaboratore() {
        RegistrazioneRequest req = new RegistrazioneRequest();
        req.setNome("Luca");
        req.setCognome("Verdi");
        req.setEmail("luca@test.it");
        req.setPassword("Password1!");
        req.setRuolo(Ruolo.COLLABORATORE);

        Utente utente = RegistrazioneMapper.toModel(req);

        assertThat(utente).isInstanceOf(Collaboratore.class);
        assertThat(utente.getNome()).isEqualTo("Luca");
    }

    @Test
    @DisplayName("RegistrazioneMapper.toModel: ruolo AMMINISTRATORE → Amministratore")
    void registrazioneMapper_toModel_amministratore() {
        RegistrazioneRequest req = new RegistrazioneRequest();
        req.setNome("Admin");
        req.setCognome("System");
        req.setEmail("admin@studio.it");
        req.setPassword("Password1!");
        req.setRuolo(Ruolo.AMMINISTRATORE);

        Utente utente = RegistrazioneMapper.toModel(req);

        assertThat(utente).isInstanceOf(Amministratore.class);
        assertThat(utente.getEmail()).isEqualTo("admin@studio.it");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ClienteMapper
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ClienteMapper.toResponse: mappa anagrafica + dati fiscali")
    void clienteMapper_toResponse_completo() {
        Cliente c = new Cliente();
        c.setId(42L);
        c.setNome("Mario");
        c.setCognome("Rossi");
        c.setEmail("mario@test.it");
        c.setEnabled(true);
        c.setCodFiscale("RSSMRO80A01H501Z");
        c.setPIVA("12345678901");
        c.setRegime(RegimeFiscale.FORFETTARIO);
        c.setRedditoAnnuo(35000.0);

        ClienteResponse dto = ClienteMapper.toResponse(c);

        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getNome()).isEqualTo("Mario");
        assertThat(dto.getCognome()).isEqualTo("Rossi");
        assertThat(dto.getEmail()).isEqualTo("mario@test.it");
        assertThat(dto.isEnabled()).isTrue();
        assertThat(dto.getCodFiscale()).isEqualTo("RSSMRO80A01H501Z");
        assertThat(dto.getPartitaIva()).isEqualTo("12345678901");
        assertThat(dto.getRegime()).isEqualTo(RegimeFiscale.FORFETTARIO);
        assertThat(dto.getRedditoAnnuo()).isEqualTo(35000.0);
    }

    @Test
    @DisplayName("ClienteMapper.toResponse: campi fiscali null → DTO con null")
    void clienteMapper_toResponse_senzaDatiFiscali() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Anna");
        c.setCognome("Verdi");
        c.setEmail("anna@test.it");
        c.setEnabled(false);

        ClienteResponse dto = ClienteMapper.toResponse(c);

        assertThat(dto.isEnabled()).isFalse();
        assertThat(dto.getCodFiscale()).isNull();
        assertThat(dto.getPartitaIva()).isNull();
        assertThat(dto.getRegime()).isNull();
        assertThat(dto.getRedditoAnnuo()).isNull();
    }

    @Test
    @DisplayName("RegistrazioneMapper.toModel: CLIENTE senza regime → regime null")
    void registrazioneMapper_toModel_clienteSenzaRegime() {
        RegistrazioneRequest req = new RegistrazioneRequest();
        req.setNome("Sara");
        req.setCognome("Neri");
        req.setEmail("sara@test.it");
        req.setPassword("Password1!");
        req.setRuolo(Ruolo.CLIENTE);
        req.setRegime(null); // nessun regime

        Utente utente = RegistrazioneMapper.toModel(req);

        assertThat(utente).isInstanceOf(Cliente.class);
        assertThat(((Cliente) utente).getRegime()).isNull();
    }
}
