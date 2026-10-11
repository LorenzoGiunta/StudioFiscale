# StudioFiscale — Gestionale Commercialisti

Applicazione web per uno studio commercialista: **commercialisti**, **collaboratori** e **clienti** si scambiano pratiche e documenti, si parlano in chat in tempo reale e ricevono notifiche in-app ed email. Un **amministratore** supervisiona gli account e verifica le iscrizioni dei professionisti.

- **Backend**: Java 25, Spring Boot 4 (Web, Data JPA, Security/JWT, WebSocket/STOMP, Mail, Flyway), MySQL 8.
- **Frontend**: React 19 + Vite 8, Tailwind, SockJS/STOMP per la chat.

---

## Avvio rapido con Docker (consigliato)

Serve solo [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
cp backend/.env.example backend/.env      # poi apri backend/.env e imposta DB_PASSWORD e JWT_SECRET
docker compose up -d --build
```

La prima build impiega qualche minuto (scarica Maven, npm e le immagini). Poi:

| Cosa | Dove |
|---|---|
| Applicazione (frontend + API) | http://localhost:8081 |
| Backend diretto (curl/Postman) | http://localhost:8080 |

Comandi utili:

```bash
docker compose logs -f backend    # log del backend
docker compose down               # ferma tutto; database e documenti restano nei volumi
docker compose down -v            # ferma tutto e CANCELLA database e documenti
```

`backend/.env` è l'unico file di configurazione: la password che scrivi in `DB_PASSWORD` viene usata sia per creare il MySQL sia dal backend per collegarsi.

---

## Avvio in locale (sviluppo)

Prerequisiti: **JDK 25**, **Node.js 22**, **MySQL 8** in ascolto su `localhost:3306`.

### Backend

1. Copia `backend/.env.example` in `backend/.env` e compila almeno `DB_PASSWORD` e `JWT_SECRET`.
2. Rendi disponibili quelle variabili al processo: come variabili d'ambiente utente di Windows, oppure nella Run Configuration di IntelliJ (*Environment variables*), oppure con `export` nel terminale.
3. Avvia:

   ```bash
   cd backend
   ./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
   ```

Al primo avvio Flyway crea lo schema e inserisce gli account di demo (vedi sotto). Il backend risponde su **:8080**, profilo `dev` (log SQL attivo).

> **Se avevi già il database creato dalla vecchia versione** (quella con `ddl-auto=create-drop`), la prima volta va svuotato: `DROP DATABASE gestionalec;` — al riavvio viene ricreato vuoto e popolato da Flyway.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite serve su **http://localhost:5173** e inoltra `/api` e `/ws` al backend su `:8080` (nessuna configurazione CORS da toccare). Avvia il backend **prima** del frontend.

---

## Account di demo

Creati dalla migrazione `V2__dati_iniziali.sql`. Le password in chiaro sono nel file di note fornito a parte (nel database ci sono solo gli hash BCrypt).

| Ruolo | Email |
|---|---|
| Amministratore | lorenzo.giunta@studiofiscale.it |
| Commercialista | giulia.bianchi@studiofiscale.it |
| Collaboratore | marco.rossi@studiofiscale.it |
| Cliente | mario.verdi@gmail.com |
| Cliente | a.cangiano04@gmail.com |

Gli **amministratori si creano solo da migrazione SQL**: l'endpoint pubblico di registrazione rifiuta il ruolo `AMMINISTRATORE`.

---

## Funzionalità principali

- **Registrazione con verifica**: clienti e collaboratori accedono subito; un **commercialista** resta *in attesa* finché un amministratore non verifica a mano il numero di albo e approva (o rifiuta, con motivazione). L'utente viene avvisato via email in entrambi i casi.
- **Recupero password** via email: link con token monouso, valido 60 minuti.
- **Pratiche** con ciclo di vita `BOZZA → IN_LAVORAZIONE → IN_ATTESA_DOCUMENTI → COMPLETATA` (pattern **State**); il passaggio a COMPLETATA richiede che tutti i documenti siano approvati.
- **Documenti** con versionamento, revisione (approva/rifiuta) da parte di commercialista o collaboratore; formati ammessi: pdf, doc/docx, xls/xlsx, jpg, png.
- **Collaborazioni**: il commercialista invita un collaboratore via email; l'invito scade in 7 giorni.
- **Chat** in tempo reale (WebSocket/STOMP) tra utenti collegati.
- **Notifiche** in-app ed email (pattern **Observer**), consegnate solo dopo il commit della transazione che le ha generate.
- **Calcolo imposte** per regime fiscale (pattern **Strategy**).
- **Scadenze fiscali**: job notturno che avvisa i clienti delle pratiche in scadenza entro 7 giorni.
- **Area amministratore**: gestione utenti (abilita/disabilita/elimina/ripristina), richieste di iscrizione, statistiche.

---

## Configurazione

Tutte le variabili sono documentate in [`backend/.env.example`](backend/.env.example). Le principali:

| Variabile | Obbligatoria | Note |
|---|---|---|
| `DB_PASSWORD` | sì | nessun default: senza, l'app non parte |
| `JWT_SECRET` | sì | almeno 32 caratteri casuali |
| `DB_URL`, `DB_USER` | no | default: MySQL locale, utente `root` |
| `EMAIL_ENABLED` | no | `false` per non spedire email (consigliato in sviluppo) |
| `MAIL_*` | se email attive | SMTP; per Gmail serve una *App Password* |
| `CORS_ALLOWED_ORIGINS` | no | origini del frontend, separate da virgola |
| `FRONTEND_URL` | no | usato nei link delle email |
| `UPLOAD_DIR` | no | cartella dei documenti caricati |

Profili Spring: `dev` (default) e `docker` (attivato dal compose). Vedi `application-dev.properties` e `application-docker.properties`.

---

## Test

```bash
cd backend
./mvnw test
```

- **~360 test unitari** (JUnit 5 + Mockito), senza database.
- **Test d'integrazione** (`GestionalecApplicationTests`) su un **MySQL vero avviato da Testcontainers**: verificano avvio del contesto, migrazioni Flyway e mappature JPA. Richiedono Docker in esecuzione; senza Docker vengono **saltati**, non falliscono.

---

## Struttura del progetto

```
backend/
  src/main/java/com/tesi/gestionalec/
    controller/   endpoint REST e STOMP
    service/      logica di dominio (interfaces + impl)
    facade/       orchestrazione di operazioni composte
    model/        entità JPA
    state/        pattern State (ciclo di vita pratica)
    strategy/     pattern Strategy (calcolo imposte)
    observer/     pattern Observer (notifiche in-app + email)
    security/     JWT, filtro, interceptor WebSocket
    scheduler/    job notturni
    config/       async, scheduling, CORS, WebSocket, sicurezza
  src/main/resources/db/migration/   migrazioni Flyway (V1 schema, V2 seed)
  Dockerfile
frontend/
  src/pages/      una cartella per ruolo (admin, cliente, collaboratore, commercialista, auth)
  src/services/   client HTTP per area funzionale
  src/models/     modelli di dominio lato client
  Dockerfile, nginx.conf
docker-compose.yml
```

## Evoluzione dello schema

Lo schema del database **non** viene più generato da Hibernate (`ddl-auto=validate`). Per cambiarlo si aggiunge un file `V3__descrizione.sql` in `backend/src/main/resources/db/migration`: Flyway lo applica al prossimo avvio e Hibernate verifica che le entità corrispondano. Non modificare mai una migrazione già applicata.
