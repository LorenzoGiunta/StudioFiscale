-- =====================================================================
-- V2: dati iniziali (account di sviluppo/demo).
--
-- Eseguita da Flyway UNA SOLA VOLTA, alla prima creazione del database:
-- non viene più ripetuta a ogni avvio come faceva data.sql. Per aggiungere
-- o cambiare dati di seed si scrive una nuova migrazione (V3, ...).
--
-- Gli account nascono già APPROVATI: la verifica manuale del numero di albo
-- riguarda solo i commercialisti che si registrano dall'applicazione.
-- Le password sono hash BCrypt (le password in chiaro sono nel file note
-- fornito a parte).
-- =====================================================================

-- ===== Super Admin: Lorenzo Giunta =====
INSERT INTO utente (id, nome, cognome, email, password, enabled, deleted, ruolo, stato_registrazione, data_registrazione)
VALUES (1, 'Lorenzo', 'Giunta', 'lorenzo.giunta@studiofiscale.it',
        '$2a$10$i1W65uwnvrTB6xE/4IubrOblWyFNT00qzc0nSH3Civc.9ll2oNVB6', true, false, 'AMMINISTRATORE', 'APPROVATA', NOW());
INSERT INTO amministratore (id, ultima_azione_amministrativa) VALUES (1, NULL);

-- ===== Commercialista: Giulia Bianchi =====
INSERT INTO utente (id, nome, cognome, email, password, enabled, deleted, ruolo, stato_registrazione, data_registrazione)
VALUES (2, 'Giulia', 'Bianchi', 'giulia.bianchi@studiofiscale.it',
        '$2a$10$Vloegt4wPUDoHoP6l2iwou7EXKJHjeNopbjyL8ZS2BbEuekBCf86W', true, false, 'COMMERCIALISTA', 'APPROVATA', NOW());
INSERT INTO commercialista (id, numero_albo) VALUES (2, '12345/A');

-- ===== Collaboratore: Marco Rossi =====
INSERT INTO utente (id, nome, cognome, email, password, enabled, deleted, ruolo, stato_registrazione, data_registrazione)
VALUES (3, 'Marco', 'Rossi', 'marco.rossi@studiofiscale.it',
        '$2a$10$Vp73PTVruBTl.SUJUIAqmO.f85iU7HoRD1JWmhOG8Pd9kGXEVf6lq', true, false, 'COLLABORATORE', 'APPROVATA', NOW());
INSERT INTO collaboratore (id) VALUES (3);

-- ===== Cliente: Mario Verdi (seguito da Giulia Bianchi, id=2) =====
INSERT INTO utente (id, nome, cognome, email, password, enabled, deleted, ruolo, stato_registrazione, data_registrazione)
VALUES (4, 'Mario', 'Verdi', 'mario.verdi@gmail.com',
        '$2a$10$uUwSlwDnuxaEvd74NbJxA.qwtY3aJv7OoKT7Jp20lpZTpH92r3jX2', true, false, 'CLIENTE', 'APPROVATA', NOW());
INSERT INTO cliente (id, cod_fiscale, piva, regime, reddito_annuo, commercialista_id)
VALUES (4, 'VRDMRA85T10A562S', NULL, 'ORDINARIO', 35000, 2);

-- ===== Collaborazione attiva: Marco Rossi (collab) collabora con Giulia Bianchi (comm) =====
-- Invito ACCEPTED: serve a popolare i contatti chat (commercialista <-> collaboratore)
-- e la vista "collaboratori" del commercialista.
INSERT INTO invito_collaborazione
    (id, commercialista_id, collaboratore_id, email_destinatario, token, stato, creato_il, scade_il)
VALUES
    (1, 2, 3, 'marco.rossi@studiofiscale.it', 'seed-token-marco-giulia', 'ACCEPTED',
     NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- ===== Cliente: Antonio Cangiano (seguito da Giulia Bianchi, id=2) =====
INSERT INTO utente (id, nome, cognome, email, password, enabled, deleted, ruolo, stato_registrazione, data_registrazione)
VALUES (5, 'Antonio', 'Cangiano', 'a.cangiano04@gmail.com',
        '$2a$10$uUwSlwDnuxaEvd74NbJxA.qwtY3aJv7OoKT7Jp20lpZTpH92r3jX2', true, false, 'CLIENTE', 'APPROVATA', NOW());
INSERT INTO cliente (id, cod_fiscale, piva, regime, reddito_annuo, commercialista_id)
VALUES (5, 'CNGNTN04T10A562S', NULL, 'FORFETTARIO', 25000, 2);
