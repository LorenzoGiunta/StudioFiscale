package com.tesi.gestionalec.service.impl;

import com.tesi.gestionalec.dto.response.StatisticheResponse;
import com.tesi.gestionalec.exception.ForbiddenOperationException;
import com.tesi.gestionalec.exception.ResourceNotFoundException;
import com.tesi.gestionalec.model.Amministratore;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Notifica;
import com.tesi.gestionalec.model.Pratica;
import com.tesi.gestionalec.model.Ruolo;
import com.tesi.gestionalec.model.TipoNotifica;
import com.tesi.gestionalec.model.Utente;
import com.tesi.gestionalec.observer.GestoreNotifiche;
import com.tesi.gestionalec.repository.AmministratoreRepo;
import com.tesi.gestionalec.repository.CommercialistaRepo;
import com.tesi.gestionalec.repository.DocumentoRepo;
import com.tesi.gestionalec.repository.PraticaRepo;
import com.tesi.gestionalec.repository.UtenteRepo;
import com.tesi.gestionalec.service.interfaces.AmministratoreService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servizio specifico per l'amministratore, estensione del servizio utenti.
 *
 * Fornisce le funzioni di supervisione del sistema: tracciamento dell'ultima
 * azione amministrativa e produzione di statistiche aggregate su utenti,
 * pratiche e documenti.
 */
@Service
public class AmministratoreServiceImpl extends UtenteServiceImpl implements AmministratoreService {

    private final AmministratoreRepo amministratoreRepository;
    private final PraticaRepo praticaRepository;
    private final DocumentoRepo documentoRepository;
    private final GestoreNotifiche gestoreNotifiche;

    public AmministratoreServiceImpl(
            UtenteRepo utenteRepository,
            PasswordEncoder passwordEncoder,
            AmministratoreRepo amministratoreRepository,
            PraticaRepo praticaRepository,
            DocumentoRepo documentoRepository,
            CommercialistaRepo commercialistaRepo,
            GestoreNotifiche gestoreNotifiche) {
        super(utenteRepository, passwordEncoder, commercialistaRepo);
        this.amministratoreRepository = amministratoreRepository;
        this.praticaRepository = praticaRepository;
        this.documentoRepository = documentoRepository;
        this.gestoreNotifiche = gestoreNotifiche;
    }

    @Override
    public void aggiornaUltimaAzione(Long amministratoreId) {
        Amministratore admin = amministratoreRepository.findById(amministratoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Amministratore", "id", amministratoreId));
        admin.setUltimaAzioneAmministrativa(LocalDateTime.now());
        amministratoreRepository.save(admin);
    }

    // Operazioni amministrative sul ciclo di vita degli account: riusano la
    // logica del servizio utenti (super.*) aggiungendo, per chi le esegue,
    // l'aggiornamento dell'ultima azione amministrativa e, per le operazioni
    // distruttive, la verifica sul bersaglio consentito.

    @Override
    public void abilitaUtente(Long id, Utente richiedente) {
        super.abilitaUtente(id);
        aggiornaUltimaAzione(richiedente.getId());
    }

    @Override
    public void disabilitaUtente(Long id, Utente richiedente) {
        verificaBersaglioConsentito(id, richiedente, "disabilitare");
        super.disabilitaUtente(id);
        notificaDisabilitazione(id);
        aggiornaUltimaAzione(richiedente.getId());
    }

    /**
     * Avvisa l'utente che il suo account è stato disabilitato. La notifica è
     * propagata tramite il pattern Observer: viene persistita e, se l'invio
     * email è abilitato, anche spedita all'indirizzo dell'utente (oggetto
     * "Account disabilitato"). L'invio è asincrono e non bloccante, quindi un
     * eventuale errore SMTP non compromette l'operazione di disabilitazione.
     */
    private void notificaDisabilitazione(Long id) {
        Utente utente = trovaPerId(id);
        Notifica notifica = new Notifica();
        notifica.setDestinatario(utente);
        notifica.setMessaggio("Il tuo account è stato disabilitato da un amministratore "
                + "e non potrai più accedere alla piattaforma. "
                + "Per maggiori informazioni contatta l'amministratore.");
        notifica.setTipo(TipoNotifica.ACCOUNT_DISABILITATO);
        notifica.setLetta(false);
        gestoreNotifiche.notificaTutti(notifica);
    }

    @Override
    public void eliminaUtente(Long id, Utente richiedente) {
        verificaBersaglioConsentito(id, richiedente, "eliminare");
        super.eliminaUtente(id);
        aggiornaUltimaAzione(richiedente.getId());
    }

    @Override
    public void ripristinaUtente(Long id, Utente richiedente) {
        super.ripristinaUtente(id);
        aggiornaUltimaAzione(richiedente.getId());
    }

    /**
     * Impedisce a un amministratore di disabilitare o eliminare il proprio
     * account o quello di un altro amministratore. In questo modo l'accesso al
     * sistema con i privilegi più elevati non può mai essere revocato
     * dall'interno dell'area di amministrazione, né per errore né per abuso.
     */
    private void verificaBersaglioConsentito(Long bersaglioId, Utente richiedente, String azione) {
        if (bersaglioId.equals(richiedente.getId())) {
            throw new ForbiddenOperationException(
                    "Non puoi " + azione + " il tuo stesso account.");
        }
        Utente bersaglio = trovaPerId(bersaglioId);
        if (bersaglio.getRuolo() == Ruolo.AMMINISTRATORE) {
            throw new ForbiddenOperationException(
                    "Non è consentito " + azione + " un altro amministratore.");
        }
    }

    @Override
    public StatisticheResponse calcolaStatistiche() {
        // Le query ordinarie escludono i record cancellati: si contano i soli attivi
        List<Utente> utenti = repo.findAll();
        List<Pratica> pratiche = praticaRepository.findAll();
        List<Documento> documenti = documentoRepository.findAll();

        StatisticheResponse stat = new StatisticheResponse();

        stat.setUtentiTotali(utenti.size());
        stat.setUtentiAbilitati(utenti.stream().filter(Utente::isEnabled).count());
        stat.setUtentiDisabilitati(utenti.stream().filter(u -> !u.isEnabled()).count());
        stat.setUtentiEliminati(repo.findAllDeleted().size());
        stat.setUtentiPerRuolo(raggruppa(utenti, u -> u.getRuolo().name()));

        stat.setPraticheTotali(pratiche.size());
        stat.setPratichePerStato(raggruppa(pratiche, p -> p.getStato().name()));

        stat.setDocumentiTotali(documenti.size());
        stat.setDocumentiPerStato(raggruppa(documenti, d -> d.getStato().name()));

        return stat;
    }

    // Conta gli elementi raggruppandoli per la chiave indicata, preservando
    // l'ordine
    private <T> Map<String, Long> raggruppa(List<T> lista, Function<T, String> chiave) {
        return lista.stream().collect(Collectors.groupingBy(
                chiave, LinkedHashMap::new, Collectors.counting()));
    }
}
