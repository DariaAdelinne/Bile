# Problema 27 — Lab 8 Student-Teacher + procesor de flux (filtru port) + microserviciu BD (SQLite)

**Tip proiect:** Coregrafie de **4 microservicii** Kotlin TCP (Maven, fat-JAR) — fără broker, BD **SQLite embedded**.

> Pornit de la aplicația de chat Student-Teacher din **Laboratorul 8**.

---

## 1. Ce face

Față de chat-ul din laborator s-au adăugat **două microservicii noi** (cerința problemei):

1. **FilterProcessor** (procesor de flux) — monitorizează toată comunicația și o **filtrează după
   numărul portului** sursă `[min, max]`.
2. **DatabaseService** (microserviciu de adăugare în BD) — primește mesajele **acceptate** și le
   **inserează într-o bază de date SQLite** (`filtered_messages.db`).

```
[Teacher 6001]  --(ASK ...)----> [MessageManager :1500]  (chat Student-Teacher, CORUTINE)
[Student 6005]  --(ANSWER ...)-->       │ broadcast etichetat cu portul sursă:
[Student 6007]  --(ANSWER ...)-->       │   "ASK <portSursă> <text>" / "ANSWER <portSursă> <text>"
[Student 7000]  --(ANSWER ...)-->       ▼
                              [FilterProcessor]  filtrează după port [min,max]
                                        │ pentru ACCEPTATE: "STORE <tip> <port> <text>"
                                        ▼
                              [DatabaseService :1700] ──► INSERT în SQLite (filtered_messages.db)
```

**Verificat (rulat efectiv, filtru 6000-6010):** mesajele de pe 6001 (ASK), 6005 și 6007 (ANSWER)
au fost **acceptate și inserate** în SQLite; cel de pe **7000 a fost respins** și **nu** apare în BD.
Conținutul bazei a fost citit înapoi cu `sqlite3` → 3 rânduri, exact cele acceptate.

---

## 2. ⚠️ De citit înainte — ce ai nevoie

| Componentă | Necesar |
|---|---|
| **Java 17** (JDK) | da — ⚠️ **NU JDK 25** (prea nou pentru Kotlin 1.6 / coroutines 1.6) |
| Setare SDK în IntelliJ | **File → Project Structure (Ctrl+Alt+Shift+S) → Project → SDK → 17** (Language level 17) |
| Maven | da (e în IntelliJ) |
| Internet la prima compilare | da (descarcă Kotlin, coroutines, **sqlite-jdbc**) |
| Bază de date | **niciuna de instalat** — SQLite e embedded (fișier `filtered_messages.db`) |

Sunt **4 procese**: DatabaseService, MessageManager, FilterProcessor și (1+) Participant.
Fiecare microserviciu = proiect Maven separat (= „mașina lui virtuală”).

---

## 3. ✅ Cum rulez — DIN BUTOANE (varianta recomandată)

### Pas A — Deschide proiectele
1. **File → Open** → selectează folderul biletului (cu cele 4 subfoldere `*Microservice`).
2. IntelliJ detectează **4 proiecte Maven**. Așteaptă sincronizarea.
   *(Dacă nu apar toate: click dreapta pe fiecare `pom.xml` → Add as Maven Project.)*
3. Verifică SDK pe **17** (secțiunea 2).

### Pas B — Pornește serviciile cu ▶ (în ordinea asta!)
1. **DatabaseService** — `DatabaseServiceMicroservice.kt` → **▶ Run**
   (`Pornit pe portul 1700`; creează `filtered_messages.db`).
2. **MessageManager** — `MessageManagerMicroservice.kt` → **▶ Run** (`Pornit pe portul 1500`).
3. **FilterProcessor** — `FilterProcessorMicroservice.kt` → **▶ Run** o dată, apoi
   **Run → Edit Configurations…** și pune la **Program arguments** intervalul, ex: `6000 6010`
   (sau lasă gol pentru implicit 6000-6010). Apasă din nou **▶**.

### Pas C — Rulează participanții (clientul Student-Teacher)
`ParticipantMicroservice.kt` → **▶ Run** o dată, apoi **Edit Configurations…** (bifează
**„Allow multiple instances”**). Format argumente: `nume portSursă rol mesaj` (rol = `teacher`/`student`):
- `Profesor 6001 teacher "Care e diferenta TCP/UDP?"`  → în interval → **salvat**
- `Ana 6005 student "TCP e orientat conexiune"`        → în interval → **salvat**
- `Bogdan 6007 student "UDP e fara conexiune"`         → în interval → **salvat**
- `Intrus 7000 student "de pe port nefiltrat"`         → în afara intervalului → **respins**

### Pas D — Vezi rezultatul
- În consola **FilterProcessor**: linii `ACCEPTAT (...)` / `RESPINS (...)`.
- În consola **DatabaseService**: `INSERAT: ... [total randuri = N]`.
- **Baza de date** `filtered_messages.db` (în directorul de lucru al DatabaseService). O poți inspecta
  cu IntelliJ **Database tool** (Data Source → SQLite → fișierul), sau din terminal:
  ```bash
  sqlite3 filtered_messages.db "SELECT * FROM messages;"
  # (dacă nu ai sqlite3:  sudo apt install -y sqlite3)
  ```

---

## 4. 🔧 Cum rulez — DIN TERMINAL (rezervă)

```bash
# 1. Găsește Maven (pe Debian e în IntelliJ)
find / -name "mvn" 2>/dev/null | grep -v proc | head -5
chmod +x CALEA_AFIȘATĂ
alias mvn='CALEA_AFIȘATĂ'

# 2. Compilează fiecare microserviciu (fat-JAR)
cd MessageManagerMicroservice    && mvn clean package -DskipTests && cd ..
cd FilterProcessorMicroservice   && mvn clean package -DskipTests && cd ..
cd DatabaseServiceMicroservice   && mvn clean package -DskipTests && cd ..
cd ParticipantMicroservice       && mvn clean package -DskipTests && cd ..

# 3. Pornește în ordine (fiecare în terminalul lui):
java -jar DatabaseServiceMicroservice/target/DatabaseServiceMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar MessageManagerMicroservice/target/MessageManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar FilterProcessorMicroservice/target/FilterProcessorMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar 6000 6010

# 4. Participanți (nume portSursă rol mesaj)
P=ParticipantMicroservice/target/ParticipantMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar $P Profesor 6001 teacher "Care e diferenta TCP/UDP?"
java -jar $P Ana      6005 student "TCP e orientat conexiune"
java -jar $P Bogdan   6007 student "UDP e fara conexiune"
java -jar $P Intrus   7000 student "de pe port nefiltrat"

# 5. Verifică baza de date
sqlite3 filtered_messages.db "SELECT * FROM messages;"
```

---

## 5. Structura + SOLID (pentru prezentare)

### Diagrama de servicii (coregrafie)
Fără orchestrator central; serviciile colaborează prin mesaje TCP:
`Participant → MessageManager → FilterProcessor → DatabaseService → SQLite`.

### Diagrama de clase (pe serviciu)
- **MessageManager**: `MessageManagerMicroservice` (broker chat, corutine).
- **FilterProcessor**: `FilterProcessorMicroservice` (regula de filtrare) + `DatabaseClient`
  (transportul mesajelor acceptate către serviciul de BD) — **două responsabilități separate**.
- **DatabaseService**: `DatabaseServiceMicroservice` (rețea/protocol) + `MessageRepository`
  (acces SQLite/JDBC) — **rețeaua separată de persistență**.
- **Participant**: `ParticipantMicroservice` (client Student/Teacher).

### SOLID
- **S (Single Responsibility):**
  - microservicii: chat / filtrare / persistență / client — fiecare un singur lucru;
  - clase: `DatabaseClient` separă transportul de regula de filtrare; `MessageRepository` separă
    SQL-ul de logica de rețea din `DatabaseServiceMicroservice`.
- **O (Open/Closed):** intervalul de porturi e parametru (nu e hardcodat); poți schimba criteriul de
  filtrare fără să atingi MessageManager sau DatabaseService.
- **D (Dependency Inversion):** FilterProcessor depinde de abstractizarea `DatabaseClient`, iar
  serviciul de BD de `MessageRepository` — nu de socket/SQL direct.
- **L / I:** colaboratori mici, focalizați; contractul dintre servicii = un protocol de mesaj simplu
  (`ASK/ANSWER ...`, `STORE ...`), nu o interfață grasă.

---

## 6. Erori frecvente + rezolvare

| Simptom | Cauză | Rezolvare |
|---|---|---|
| BD goală | participanții erau toți pe porturi în afara intervalului, sau au rulat înainte de FilterProcessor | pornește FilterProcessor **înainte** de participanți; folosește porturi în interval |
| `DatabaseService indisponibil` în consola FilterProcessor | serviciul de BD nu e pornit | pornește **întâi** `DatabaseServiceMicroservice` (1700) |
| `Connection refused` la FilterProcessor | MessageManager nu e pornit | pornește MessageManager (1500) înainte de FilterProcessor |
| `Address already in use` (port sursă) | ai repornit un Participant pe același port prea repede (TIME_WAIT) | alt port sursă sau așteaptă câteva secunde |
| App pică pe JDK 25 | JDK prea nou | setează Project SDK pe **17** (secțiunea 2) |
| `mvn: command not found` | Maven nu e în PATH | secțiunea 4 (`find` + `alias`) |
| `No suitable driver` la SQLite | rulezi clasa fără fat-JAR-ul cu dependențe | rulează `*-jar-with-dependencies.jar` (din IntelliJ merge direct) |

---

## 7. Fișiere importante

```
Bilet_Prob27_Filtru_Port_BD/
├── cerinta.txt
├── CUM_RULEZ.md                                  ← acest ghid
├── MessageManagerMicroservice/                   (chat Student-Teacher, corutine)
│   └── src/main/kotlin/com/sd/laborator/
│       └── MessageManagerMicroservice.kt         ← broadcast etichetat cu portul sursă
├── FilterProcessorMicroservice/                  (procesorul de flux)
│   └── src/main/kotlin/com/sd/laborator/
│       ├── FilterProcessorMicroservice.kt        ← filtru după gama de porturi
│       └── DatabaseClient.kt                      ← trimite mesajele acceptate la serviciul de BD
├── DatabaseServiceMicroservice/                  (microserviciul de adăugare în BD)
│   └── src/main/kotlin/com/sd/laborator/
│       ├── DatabaseServiceMicroservice.kt        ← server TCP, primește "STORE ..."
│       └── MessageRepository.kt                  ← INSERT/SELECT în SQLite (sqlite-jdbc)
└── ParticipantMicroservice/                      (client Student/Teacher)
    └── src/main/kotlin/com/sd/laborator/
        └── ParticipantMicroservice.kt            ← rol teacher (ASK) / student (ANSWER), port sursă fix
```

> Notă: `filtered_messages.db` se creează la rulare în directorul de lucru al DatabaseService și **nu**
> e livrat (e artefact generat). Apare automat la prima inserare.
