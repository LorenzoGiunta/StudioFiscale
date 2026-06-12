package com.tesi.gestionalec.mapper;

import com.tesi.gestionalec.dto.request.RegistrazioneRequest;
import com.tesi.gestionalec.model.*;

/**
 * Costruisce l'entità utente a partire dalla richiesta di registrazione,
 * istanziando la sottoclasse corrispondente al ruolo e valorizzandone i campi
 * specifici oltre a quelli comuni.
 */
public class RegistrazioneMapper {

    public static Utente toModel(RegistrazioneRequest request) {
        return switch (request.getRuolo()) {
            case CLIENTE         -> toCliente(request);
            case COMMERCIALISTA  -> toCommercialista(request);
            case COLLABORATORE   -> toCollaboratore(request);
            case AMMINISTRATORE  -> toAmministratore(request);
        };
    }

    // Costruttori specifici per ciascun ruolo
    private static Cliente toCliente(RegistrazioneRequest request) {

        Cliente cliente = new Cliente();
        compilaCampiComuni(cliente, request);

        cliente.setCodFiscale(request.getCodFiscale());
        cliente.setPIVA(request.getPartitaIva());
        cliente.setRedditoAnnuo(request.getRedditoAnnuo());

        if (request.getRegime() != null) {
            cliente.setRegime(RegimeFiscale.valueOf(request.getRegime()));
        }

        return cliente;
    }

    private static Commercialista toCommercialista(RegistrazioneRequest request) {
        Commercialista commercialista = new Commercialista();
        compilaCampiComuni(commercialista, request);
        commercialista.setNumeroAlbo(request.getNumeroAlbo());
        return commercialista;
    }

    private static Collaboratore toCollaboratore(RegistrazioneRequest request) {
        Collaboratore collaboratore = new Collaboratore();
        compilaCampiComuni(collaboratore, request);
        return collaboratore;
    }

    private static Amministratore toAmministratore(RegistrazioneRequest request) {
        Amministratore amministratore = new Amministratore();
        compilaCampiComuni(amministratore, request);
        return amministratore;
    }

    // Valorizzazione dei campi comuni a tutti i ruoli
    private static void compilaCampiComuni(Utente utente, RegistrazioneRequest request) {
        utente.setNome(request.getNome());
        utente.setCognome(request.getCognome());
        utente.setEmail(request.getEmail());
        utente.setPassword(request.getPassword());  // la cifratura è a carico del servizio
    }
}