-- =====================================================================
-- V1: schema iniziale.
--
-- Generato dal DDL che Hibernate produceva con ddl-auto=create-drop, così
-- che ddl-auto=validate lo accetti senza differenze. Da qui in avanti lo
-- schema evolve SOLO tramite nuove migrazioni Flyway (V2, V3, ...): non
-- modificare questo file dopo che è stato applicato a un database.
--
-- Gerarchia utenti JOINED: una riga in "utente" (discriminatore "ruolo") +
-- una riga nella tabella della sottoclasse, con lo stesso id.
-- =====================================================================

CREATE TABLE amministratore (
    id bigint not null,
    ultima_azione_amministrativa datetime(6),
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE cliente (
    reddito_annuo float(53),
    commercialista_id bigint,
    id bigint not null,
    cod_fiscale varchar(255),
    piva varchar(255),
    regime enum ('FORFETTARIO','ORDINARIO'),
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE collaboratore (
    id bigint not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE commercialista (
    id bigint not null,
    numero_albo varchar(255) not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE documento (
    deleted bit not null,
    versione integer not null,
    caricato_da_id bigint not null,
    data_caricamento datetime(6),
    deleted_at datetime(6),
    dimensione bigint not null,
    id bigint not null auto_increment,
    pratica_id bigint not null,
    revisore bigint,
    motivazione_rifiuto varchar(255),
    nome varchar(255) not null,
    percorso_file varchar(255) not null,
    tipo_file varchar(255) not null,
    stato enum ('APPROVATO','IN_REVISIONE','RIFIUTATO') not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE invito_collaborazione (
    collaboratore_id bigint,
    commercialista_id bigint not null,
    creato_il datetime(6) not null,
    id bigint not null auto_increment,
    scade_il datetime(6) not null,
    email_destinatario varchar(255) not null,
    token varchar(255) not null,
    stato enum ('ACCEPTED','DECLINED','EXPIRED','PENDING') not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE messaggio_chat (
    letto bit not null,
    data_invio datetime(6),
    destinatario_id bigint not null,
    id bigint not null auto_increment,
    mittente_id bigint not null,
    testo varchar(255) not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE notifica (
    letta bit not null,
    data_creazione datetime(6),
    id bigint not null auto_increment,
    utente_id bigint not null,
    messaggio varchar(255) not null,
    tipo enum ('ACCOUNT_APPROVATO','ACCOUNT_CREATO','ACCOUNT_DISABILITATO','ACCOUNT_RIFIUTATO','CAMBIO_STATO','DOCUMENTO_APPROVATO','DOCUMENTO_CARICATO','DOCUMENTO_RIFIUTATO','INVITO_COLLABORAZIONE','PASSWORD_MODIFICATA','RICHIESTA_ISCRIZIONE','SCADENZA_IMMINENTE') not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE pratica (
    deleted bit not null,
    scadenza date,
    assegnata_a_id bigint,
    cliente_id bigint not null,
    data_creazione datetime(6) not null,
    deleted_at datetime(6),
    id bigint not null auto_increment,
    stato varchar(255) not null,
    tipo_pratica enum ('DICHIARAZIONE_REDDITI','IMU','IVA') not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE token_reset_password (
    creato_il datetime(6) not null,
    id bigint not null auto_increment,
    scade_il datetime(6) not null,
    usato_il datetime(6),
    utente_id bigint not null,
    token varchar(64) not null,
    primary key (id)
) ENGINE=InnoDB;

CREATE TABLE utente (
    deleted bit not null,
    enabled bit not null,
    data_registrazione datetime(6),
    deleted_at datetime(6),
    id bigint not null auto_increment,
    ruolo varchar(31) not null check ((ruolo in ('AMMINISTRATORE','CLIENTE','COLLABORATORE','COMMERCIALISTA'))),
    cognome varchar(255) not null,
    email varchar(255) not null,
    nome varchar(255) not null,
    password varchar(255) not null,
    stato_registrazione enum ('APPROVATA','IN_ATTESA','RIFIUTATA') not null,
    primary key (id)
) ENGINE=InnoDB;

-- Vincoli di unicità
ALTER TABLE cliente ADD CONSTRAINT UK3ceeqk1sfkgq8wng5rixoa5vo UNIQUE (cod_fiscale);
ALTER TABLE invito_collaborazione ADD CONSTRAINT uk_invito_comm_email_stato UNIQUE (commercialista_id, email_destinatario, stato);
ALTER TABLE invito_collaborazione ADD CONSTRAINT UKrjgpt9dlxtr72mn004f5qs9fl UNIQUE (token);
ALTER TABLE token_reset_password ADD CONSTRAINT UKc9gblcasi9xqj6tac3igsvw7e UNIQUE (token);
ALTER TABLE utente ADD CONSTRAINT UKgxvq4mjswnupehxnp35vawmo2 UNIQUE (email);

-- Chiavi esterne
ALTER TABLE amministratore ADD CONSTRAINT FKg9yb2y9q9ytqtdm0uhrp6je23 FOREIGN KEY (id) REFERENCES utente (id);
ALTER TABLE cliente ADD CONSTRAINT FKhvcard2xdsn8fms3oja2qa0e7 FOREIGN KEY (commercialista_id) REFERENCES commercialista (id);
ALTER TABLE cliente ADD CONSTRAINT FK3aqbp41180b2i651dk3ddhyua FOREIGN KEY (id) REFERENCES utente (id);
ALTER TABLE collaboratore ADD CONSTRAINT FK15gity2c0rm97mmm90x2c5q10 FOREIGN KEY (id) REFERENCES utente (id);
ALTER TABLE commercialista ADD CONSTRAINT FKgglegoao0jbcs46o4mkta7tqi FOREIGN KEY (id) REFERENCES utente (id);
ALTER TABLE documento ADD CONSTRAINT FK5sn3cpcq3yehpeojal5lqojab FOREIGN KEY (caricato_da_id) REFERENCES cliente (id);
ALTER TABLE documento ADD CONSTRAINT FKiv47kf5qhtvyu9flgdktultuq FOREIGN KEY (pratica_id) REFERENCES pratica (id);
ALTER TABLE documento ADD CONSTRAINT FKpfsryp6synbeqox4fk8rkrcj5 FOREIGN KEY (revisore) REFERENCES collaboratore (id);
ALTER TABLE invito_collaborazione ADD CONSTRAINT FK9ucwunljlhdnm48wo4yn6acfu FOREIGN KEY (collaboratore_id) REFERENCES collaboratore (id);
ALTER TABLE invito_collaborazione ADD CONSTRAINT FK3q5x2aw9r80v0s56rud7mm50s FOREIGN KEY (commercialista_id) REFERENCES commercialista (id);
ALTER TABLE messaggio_chat ADD CONSTRAINT FK6nam8ebxu235qx7r2j4f5bug5 FOREIGN KEY (destinatario_id) REFERENCES utente (id);
ALTER TABLE messaggio_chat ADD CONSTRAINT FKdarr3levkcv8195hp64bux3yf FOREIGN KEY (mittente_id) REFERENCES utente (id);
ALTER TABLE notifica ADD CONSTRAINT FKjc7rt8lft5wvqpwa5l9q49ci5 FOREIGN KEY (utente_id) REFERENCES utente (id);
ALTER TABLE pratica ADD CONSTRAINT FK7iqgqij5dm4g1ea00mne3abbe FOREIGN KEY (assegnata_a_id) REFERENCES collaboratore (id);
ALTER TABLE pratica ADD CONSTRAINT FKrurapml2m7gfr06yvtd8ase4w FOREIGN KEY (cliente_id) REFERENCES cliente (id);
ALTER TABLE token_reset_password ADD CONSTRAINT FKl33j2tqv6n8imdt5ov7ykqwtp FOREIGN KEY (utente_id) REFERENCES utente (id);
