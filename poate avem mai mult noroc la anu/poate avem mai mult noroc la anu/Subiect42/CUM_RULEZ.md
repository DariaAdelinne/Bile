# Bilet (PDF) — Lab 8: Chat cu corutine + procesor de flux statistici erori TCP → serviciu REST (XML + browser)

**Tip proiect:** Coregrafie de **4 microservicii** — 3 Kotlin TCP cu corutine (Maven, fat-JAR) + 1 Spring Boot REST.

> Pornit de la aplicația de chat din **Laboratorul 8** (Student-Teacher / MessageManager).

---

## 1. Ce face

Față de chat-ul din laborator s-au adăugat (folosind **corutine**):

1. **MessageManager** (broker chat) — pe lângă broadcast, **detectează erorile de comunicare specifice TCP**
   care apar în dialogul cu clienții și le **raportează** procesorului de flux.
2. **ErrorStatsProcessor** (procesorul de flux NOU) — primește erorile, ține **statistici pe tipuri**
   (câte de fiecare fel) și le **trimite unui serviciu REST** prin HTTP.
3. **ErrorReportRestService** (serviciul REST) — **adaugă fiecare eroare într-un fișier XML local**
   (`errors.xml`) și, la cererea unui navigator, **afișează pagina completă** cu erorile + statisticile.
4. **Student** — client de chat care, în funcție de `mod`, se comportă corect sau **defect** (ca să producă
   fiecare tip de eroare TCP).

```
[Student]x N --(MSG/QUIT, unii defecți)--> [MessageManager :1500]  (chat, CORUTINE)
                                                  │ detectează erorile TCP
                                                  │  "ERR <tip> <portSursă> <detaliu>"
                                                  ▼
                                     [ErrorStatsProcessor :1600]  (procesor de flux, CORUTINE)
                                                  │ agregă STATISTICI pe tipuri
                                                  │  HTTP POST /errors
                                                  ▼
                                     [ErrorReportRestService :8080]  (Spring Boot REST)
                                                  ├──► adaugă în errors.xml  (fișier XML local)
                                                  └──► GET /  : pagină HTML completă în browser
```

**Tipuri de erori TCP detectate** (fiecare are un mod de Student care îl produce):

| Tip eroare | Cauză | Modul de Student care îl produce |
|---|---|---|
| `CONNECTION_RESET`  | clientul rupe brusc conexiunea (RST) → `SocketException: Connection reset` | `abrupt` |
| `EOF_UNEXPECTED`    | clientul închide conexiunea **fără** `QUIT` (EOF neașteptat) | `eof` |
| `MALFORMED_MESSAGE` | mesaj care nu respectă protocolul (nu e `MSG`/`QUIT`) | `malformed` |
| `BROKEN_PIPE`       | broadcast către un socket deja închis | apare oportunist (timing) |

**Verificat (rulat efectiv):** 5 studenți (`ok`, `malformed`, `eof`, `abrupt`, `malformed`) →
MessageManager a detectat `MALFORMED_MESSAGE`×2, `EOF_UNEXPECTED`×1, `CONNECTION_RESET`×1;
procesorul a agregat statisticile și le-a trimis la REST (HTTP 200); `errors.xml` a primit 4 noduri
`<error>`; pagina `GET /` afișează tabelul de statistici + tabelul de detalii. Studentul `ok` **nu** a
produs nicio eroare (corect).

---

## 2. ⚠️ De citit înainte — ce ai nevoie

| Componentă | Necesar |
|---|---|
| **Java 17** (JDK) | da — ⚠️ **NU JDK 25** (prea nou pentru Kotlin 1.6 / Spring Boot 2.3 / coroutines 1.6) |
| Setare SDK în IntelliJ | **File → Project Structure (Ctrl+Alt+Shift+S) → Project → SDK → 17** (Language level 17) |
| Maven | da (e în IntelliJ) |
| Internet la prima compilare | da (descarcă Kotlin, coroutines, Spring Boot) |
| Browser | da (pentru pagina de la `http://localhost:8080`) |

Fără bază de date, fără broker. Sunt **4 procese**: REST, ErrorStatsProcessor, MessageManager și (1+) Student.
Fiecare microserviciu = proiect Maven separat (= „mașina lui virtuală”).

---

## 3. ✅ Cum rulez — DIN BUTOANE (varianta recomandată)

Ai nevoie de mai multe ferestre **Run** în IntelliJ.

### Pas A — Deschide proiectele
1. **File → Open** → selectează folderul biletului (cu cele 4 subfoldere).
2. IntelliJ detectează **4 proiecte Maven**. Așteaptă sincronizarea (jos-dreapta).
   *(Dacă nu apar toate: click dreapta pe fiecare `pom.xml` → Add as Maven Project.)*
3. Verifică SDK-ul pe **17** (secțiunea 2).

### Pas B — Pornește serviciile cu ▶ (în ordinea asta!)
1. **REST** — `ErrorReportApplication.kt` → **▶ Run**. Așteaptă `Started ErrorReportApplicationKt`
   (pornește pe `http://localhost:8080`).
2. **Procesorul de flux** — `ErrorStatsProcessorMicroservice.kt` → **▶ Run**
   (`Procesor de flux pornit pe portul 1600`).
3. **MessageManager** — `MessageManagerMicroservice.kt` → **▶ Run**
   (`Pornit pe portul 1500 (corutine)`).

### Pas C — Rulează studenții (clientul, cu moduri diferite)
`StudentMicroservice.kt` → **▶ Run** o dată, apoi **Run → Edit Configurations…** și pune la
**Program arguments** (format: `nume portSursă mod mesaj`). Bifează **„Allow multiple instances”**
ca să poți rula mai mulți:
- `Andrei 6001 ok "Salut tuturor"`   → **fără** eroare (trimite MSG + QUIT)
- `Bianca 6002 malformed x`          → `MALFORMED_MESSAGE`
- `Cosmin 6003 eof "plec"`           → `EOF_UNEXPECTED`
- `Dan 6004 abrupt "rup"`            → `CONNECTION_RESET`

Apasă **▶** pentru fiecare (la câteva secunde distanță, ca să vezi clar în loguri).

### Pas D — Vezi rezultatul
- **Browser:** deschide **http://localhost:8080/** → pagină cu **tabel de statistici** (câte erori de
  fiecare tip) + **tabel de detalii** (timestamp, tip, port sursă, detaliu). Se reîmprospătează la 5s.
- **XML brut:** **http://localhost:8080/errors.xml** (sau deschide fișierul `errors.xml` din directorul
  de lucru al serviciului REST).
- În consola **ErrorStatsProcessor** apar liniile `=== STATISTICI erori: ... ===`.

---

## 4. 🔧 Cum rulez — DIN TERMINAL (rezervă, dacă nu merge din butoane)

```bash
# 1. Găsește Maven (pe Debian e în IntelliJ)
find / -name "mvn" 2>/dev/null | grep -v proc | head -5
chmod +x CALEA_AFIȘATĂ
alias mvn='CALEA_AFIȘATĂ'

# 2. Compilează fiecare microserviciu
cd MessageManagerMicroservice      && mvn clean package -DskipTests && cd ..
cd ErrorStatsProcessorMicroservice && mvn clean package -DskipTests && cd ..
cd StudentMicroservice             && mvn clean package -DskipTests && cd ..
cd ErrorReportRestService          && mvn clean package -DskipTests && cd ..

# 3. Pornește în ordine (fiecare în terminalul lui):
# T1 — REST (8080); errors.xml se creează în directorul de unde pornești
java -jar ErrorReportRestService/target/ErrorReportRestService-1.0.0.jar
# T2 — procesorul de flux (1600)
java -jar ErrorStatsProcessorMicroservice/target/ErrorStatsProcessorMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar
# T3 — MessageManager (1500)
java -jar MessageManagerMicroservice/target/MessageManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar

# T4 — studenți (nume portSursă mod mesaj)
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Andrei 6001 ok        "Salut"
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Bianca 6002 malformed x
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Cosmin 6003 eof       "plec"
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Dan    6004 abrupt    "rup"

# 4. Verifică
curl http://localhost:8080/            # pagina HTML
curl http://localhost:8080/errors.xml  # XML brut
cat errors.xml
```

---

## 5. Structura + SOLID (pentru prezentare)

### Diagrama de servicii (coregrafie)
Fiecare microserviciu are **o singură responsabilitate** și comunică prin **mesaje** (TCP între chat și
procesor, HTTP între procesor și REST) — **fără orchestrator central** = coregrafie.

- **MessageManager** — broker de chat + detector de erori TCP (corutine, o corutină per client).
- **ErrorStatsProcessor** — procesorul de flux: agregă statistici pe tipuri + forward la REST (corutine).
- **ErrorReportRestService** — REST: stochează în XML + prezintă în browser.
- **Student** — client de chat (corect sau defect).

### Diagrama de clase (pe serviciu)
- MessageManager: `MessageManagerMicroservice` (logica de chat + detecția erorilor) + `ErrorReporter`
  (transportul erorilor către procesor) — **două clase**, două responsabilități.
- REST: `ErrorReportController` (web) + `XmlErrorStore` (persistența XML) + `ErrorReportApplication` (bootstrap).

### SOLID
- **S (Single Responsibility):**
  - la nivel de microserviciu: chat / statistici / REST+XML / client — fiecare face un singur lucru;
  - la nivel de clasă: `ErrorReporter` separă transportul erorilor de logica de chat; `XmlErrorStore`
    separă scrierea XML de prezentarea web din `ErrorReportController`.
- **O (Open/Closed):** procesorul numără **orice** tip de eroare nou fără să-i modifici codul
  (`HashMap<tip, count>`); poți adăuga un tip de eroare în MessageManager fără să atingi procesorul/REST.
- **D (Dependency Inversion):** MessageManager depinde de abstractizarea `ErrorReporter` (nu de socket
  direct); `ErrorReportController` depinde de `XmlErrorStore` injectat (nu de detaliile DOM/fișier).
- **L / I:** colaboratorii sunt mici și focalizați; contractul dintre servicii e un **protocol de mesaj**
  simplu (`ERR ...`, `type/sourcePort/detail`), nu o interfață grasă.

---

## 6. Erori frecvente + rezolvare

| Simptom | Cauză | Rezolvare |
|---|---|---|
| Pagina e goală / „Nicio eroare” | studenții erau toți pe `ok`, sau n-au rulat încă | rulează un Student pe `malformed`/`eof`/`abrupt` |
| `Connection refused` la procesor | ai pornit MessageManager înainte de procesor | pornește întâi REST, apoi procesor, apoi MessageManager |
| `Serviciul REST indisponibil` în consola procesorului | REST nu e pornit | pornește întâi `ErrorReportApplication` (8080) |
| `Address already in use` (port sursă student) | ai repornit un Student pe același port prea repede (TIME_WAIT) | alt port sursă sau așteaptă câteva secunde |
| App pică pe JDK 25 | JDK prea nou | setează Project SDK pe **17** (secțiunea 2) |
| `mvn: command not found` | Maven nu e în PATH | secțiunea 4 (`find` + `alias`) |
| pagina nu se schimbă | cache browser | se reîmprospătează la 5s; sau Ctrl+F5 |

---

## 7. Fișiere importante

```
Bilet_Lab8_Statistici_Erori_TCP/
├── cerinta.txt
├── CUM_RULEZ.md                                  ← acest ghid
├── MessageManagerMicroservice/                   (chat broker, corutine)
│   └── src/main/kotlin/com/sd/laborator/
│       ├── MessageManagerMicroservice.kt         ← broadcast + DETECTAREA erorilor TCP
│       └── ErrorReporter.kt                      ← transportă erorile la procesor (TCP)
├── ErrorStatsProcessorMicroservice/              (procesorul de flux, corutine)
│   └── src/main/kotlin/com/sd/laborator/
│       └── ErrorStatsProcessorMicroservice.kt    ← STATISTICI pe tipuri + forward HTTP la REST
├── ErrorReportRestService/                       (Spring Boot REST)
│   └── src/main/kotlin/com/sd/laborator/
│       ├── ErrorReportApplication.kt             ← bootstrap (port 8080)
│       ├── ErrorReportController.kt              ← POST /errors, GET /, GET /errors.xml
│       └── XmlErrorStore.kt                      ← adăugare/citire errors.xml (DOM)
└── StudentMicroservice/                          (client de chat)
    └── src/main/kotlin/com/sd/laborator/
        └── StudentMicroservice.kt                ← moduri: ok | malformed | eof | abrupt
```

> Notă: `errors.xml` se creează la rulare în directorul de lucru al serviciului REST și **nu** e livrat
> (e artefact generat). La examen apare automat după primele erori.
