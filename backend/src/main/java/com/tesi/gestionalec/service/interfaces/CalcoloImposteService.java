package com.tesi.gestionalec.service.interfaces;

import com.tesi.gestionalec.model.Cliente;

/**
 * Contratto per il calcolo delle imposte di un cliente, indipendente dal regime
 * fiscale applicato: la scelta dell'algoritmo concreto è demandata al pattern
 * Strategy.
 */
public interface CalcoloImposteService {
    double CalcolaPerCliente(Cliente cliente);
}
