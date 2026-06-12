package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Cliente;
import com.tesi.gestionalec.model.Documento;
import com.tesi.gestionalec.model.Pratica;

import java.util.List;

/**
 * Contratto specifico per i clienti: ricerca per codice fiscale, accesso a
 * pratiche e documenti e aggiornamento dei dati personali, in aggiunta alle
 * funzionalità comuni ereditate dal contratto utente.
 */
public interface ClienteService extends UtenteService{
    Cliente trovaPerCodFiscale(String codFiscale);
    List<Pratica> trovaPratiche(Long clienteId);
    List<Documento> trovaDocumenti(Long clienteId);
    Cliente aggiorna(Long id, Cliente clienteAggiornato);

    /**
     * Restituisce il cliente con l'id indicato o solleva un'eccezione se non
     * esiste o se l'utente corrispondente non è un cliente. Evita il ricorso a
     * un cast di tipo non sicuro sul risultato della ricerca generica.
     */
    Cliente trovaClientePerId(Long id);
}
