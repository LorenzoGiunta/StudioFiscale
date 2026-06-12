# Gestionale Commercialisti

Questo è il repository completo del gestionale. Trovi tutto diviso in due cartelle principali: il **backend** (il cervello dell'app) e il **frontend** (l'interfaccia visiva). 
L'idea è di avere un'app dove commercialisti, clienti e collaboratori possano scambiarsi pratiche, caricare documenti e chattare in tempo reale.

## Struttura del Progetto

- **`backend/`**: sviluppato in Java con **Spring Boot**. Si occupa di gestire i dati nel database (MySQL), rilasciare i token JWT per l'autenticazione e smistare i messaggi della chat usando i WebSocket.
- **`frontend/`**: costruito con **React** e impacchettato con **Vite**. È una Single Page Application (SPA) che si interfaccia direttamente con le API del backend.

---

## Come avviare tutto in locale

Per far girare l'app sul tuo computer, ti serve avere queste cose già installate:
- **Java 17** (o una versione più recente)
- **Node.js** (consigliata la versione 18 in poi)
- **MySQL** (acceso e funzionante in locale)

### 1. Far partire il Backend
1. Vai nella cartella del backend.
2. Controlla il file `src/main/resources/application.properties` per assicurarti che la connessione al database e la porta siano giuste. 
   *Nota bene*: Il backend ha bisogno di una chiave segreta per il JWT. Puoi passarla come variabile d'ambiente `JWT_SECRET`, oppure, se stai solo facendo test al volo, puoi scriverla direttamente nel file properties (ma ricordati di toglierla in produzione!).
3. Avvia il server usando il wrapper di Maven integrato (così non devi installarlo a parte):
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
   *(Se usi Windows e il prompt dei comandi normale, esegui `mvnw.cmd spring-boot:run`)*

Di default il server risponde sulla porta **8080**.

### 2. Far partire il Frontend
1. Apri un'altra finestra del terminale e vai nella cartella del frontend.
2. Installa le librerie (ti basta farlo solo la prima volta):
   ```bash
   cd frontend
   npm install
   ```
3. Fai partire il server di sviluppo:
   ```bash
   npm run dev
   ```

Vite ti stamperà l'indirizzo a cui collegarti, di solito è `http://localhost:5173`. 
Se tocchi il codice React, la pagina si aggiornerà da sola senza farti ricaricare nulla.

---

## Note
- Accendi il backend **prima** di fare operazioni dal frontend, sennò le chiamate API andranno a vuoto.
- Se l'invio delle email ti dà fastidio in fase di test, puoi disattivarlo settando `app.email.enabled=false` nel properties.
