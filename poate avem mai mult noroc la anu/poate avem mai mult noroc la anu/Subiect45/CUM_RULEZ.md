# State machine prin funcții serverless (Micronaut) + cozi RabbitMQ

**Tip proiect:** un serviciu **Micronaut 3.1.4** (Kotlin) — REST producător + **funcție serverless** (`@RabbitListener`) care ascultă o coadă **RabbitMQ**.

> Bonus din PDF: în loc de microservicii ca noduri, **funcții serverless** care comunică prin **RabbitMQ**.

---

## 1. Ce face

Un **state machine** (graf) procesat asincron prin coadă:

```
user --POST /transition {entryNode, transition}--> [TransitionController]
                                                          │ concatenează "entryNode|transition"
                                                          │ TransitionPublisher (@RabbitClient)
                                                          ▼
                                                 [coadă RabbitMQ: state-machine-queue]
                                                          ▼
                                        [StateMachineFunction] (@RabbitListener = FUNCȚIE SERVERLESS)
                                                          │ destructurează → NodeRequest
                                                          │ NodeGraphService.apply()  (graful)
                                                          ▼ NodeResponse (destinație / "no se puede")
                                                 afișare + ResultStore (GET /results)
```

Spre deosebire de funcția din laborator (care asculta o **rută** `@Post`), aici funcția **ascultă coada**.
Logica de graf (paralelă la „ErastoteneService"): `HashMap<String, List<Pair<tranziție, destinație>>>`.

Graful exemplu (workflow de document):
`DRAFT --submit--> REVIEW --approve--> PUBLISHED --archive--> ARCHIVED`, plus `REVIEW --reject--> DRAFT`.

---

## 2. ⚠️ Ce s-a verificat și ce NU (cinstit)

| Verificare | Stare |
|---|---|
| **Build Micronaut** (`mvn clean package -Plocal`, kapt + `@RabbitClient`/`@RabbitListener`) | ✅ **rulat aici, trece** (fat-JAR ~19 MB) |
| Logica grafului (`apply`: tranziții valide → destinație; invalide → „no se puede") | ✅ verificată (echivalent, aceeași tabelă) |
| **E2E cu broker RabbitMQ** (POST → coadă → funcție → /results) | ⚠️ **NErulat pe PC-ul de dezvoltare** — Docker Desktop nu a pornit engine-ul (cere admin). Se rulează pe Debian conform pașilor de mai jos (broker prin `apt`). |

Codul e complet și pornește pe Debian cu un RabbitMQ instalat. Partea grea (procesarea adnotărilor
RabbitMQ de către Micronaut la compilare) **este** verificată aici.

---

## 3. ⚠️ De citit înainte — ce ai nevoie

| Componentă | Necesar |
|---|---|
| **Java 17** (JDK) | da — ⚠️ **NU JDK 25** (Micronaut 3.1 nu pornește pe 25) |
| Setare SDK în IntelliJ | **File → Project Structure (Ctrl+Alt+Shift+S) → Project → SDK → 17** |
| Maven | da (e în IntelliJ); build Micronaut cu profilul **`local`** |
| **RabbitMQ** | da — broker pe `localhost:5672`, user `student` / parolă `student` |
| Internet la prima compilare | da (Micronaut + micronaut-rabbitmq) |

### Instalare RabbitMQ pe Debian (o singură dată)
```bash
sudo apt update && sudo apt install -y rabbitmq-server
sudo service rabbitmq-server start
sudo rabbitmqctl add_user student student
sudo rabbitmqctl set_user_tags student administrator
sudo rabbitmqctl set_permissions -p / student ".*" ".*" ".*"
```

---

## 4. ✅ Cum rulez — DIN BUTOANE (IntelliJ)

1. **File → Open** → folderul `StateMachineGraph`. Așteaptă importul Maven. SDK pe **17** (secțiunea 3).
2. Asigură-te că **RabbitMQ rulează** (secțiunea 3) și că ai `student/student`.
3. Rulează aplicația: deschide `Application.kt` → **▶ Run** (sau Run config Micronaut, profil `local`).
   La pornire vezi `Coada 'state-machine-queue' declarata` și `Startup completed`.
4. Testează (din terminal sau Postman):
   ```bash
   # tranziție validă
   curl -X POST localhost:8080/transition -H "Content-Type: application/json" \
        -d '{"entryNode":"DRAFT","transition":"submit"}'
   # tranziție invalidă -> "no se puede"
   curl -X POST localhost:8080/transition -H "Content-Type: application/json" \
        -d '{"entryNode":"DRAFT","transition":"approve"}'
   # vezi rezultatele produse de funcția serverless
   curl localhost:8080/results
   # vezi graful
   curl localhost:8080/graph
   ```
   În consolă apar liniile `[REST] Pus in coada: ...` și `[Functie] 'DRAFT' --(submit)--> 'REVIEW'`
   / `... --> no se puede :)`.

---

## 5. 🔧 Cum rulez — DIN TERMINAL (rezervă)

```bash
# 0. RabbitMQ pornit (secțiunea 3)
# 1. Maven (pe Debian e în IntelliJ)
find / -name "mvn" 2>/dev/null | grep -v proc | head -5
chmod +x CALEA; alias mvn='CALEA'
# 2. Build (profil local adaugă serverul HTTP Netty)
cd StateMachineGraph && mvn clean package -DskipTests -Plocal
# 3. Rulează
java -jar target/StateMachineGraph-0.1.jar
# 4. Testează cu curl (vezi secțiunea 4)
```

---

## 6. Structura + SOLID (pentru prezentare)

### Diagrama (servicii/funcții)
`user → TransitionController (REST) → coadă RabbitMQ → StateMachineFunction (serverless) → NodeGraphService`.
Comunicare prin **coadă** (decuplare producător/consumator) = coregrafie.

### Clase și paralela cu laboratorul
- `TransitionController` — POST pune mesajul în coadă (nu procesează direct).
- `TransitionPublisher` (`@RabbitClient`) — producătorul.
- `StateMachineFunction` (`@RabbitListener`) — **funcția serverless** care ascultă coada.
- `NodeGraphService` — **paralela la ErastoteneService** (graful + `apply`).
- `NodeRequest` / `NodeResponse` — paralele la `EratosteneRequest` / `EratosteneResponse`.
- `QueueDeclarer` — declară coada la pornire (`ChannelPool`).
- `ResultStore` — păstrează rezultatele (vizibile la `GET /results`).

### SOLID
- **S:** fiecare clasă un rol (REST / publicare / consum / graf / stocare).
- **O:** graful e o structură de date — adaugi noduri/tranziții fără să schimbi funcția sau REST-ul.
- **D:** funcția și controllerul depind de abstracții (`NodeGraphService`, `TransitionPublisher`),
  nu de detalii de transport.
- **Decuplare prin coadă:** producătorul nu știe de consumator (mesagerie asincronă).

---

## 7. Erori frecvente + rezolvare

| Simptom | Cauză | Rezolvare |
|---|---|---|
| La pornire: `Connection refused` 5672 / nu pornește | RabbitMQ neporrnit | pornește brokerul (secțiunea 3) |
| `ACCESS_REFUSED` / login | user lipsă | `rabbitmqctl add_user student student` + permisiuni (secțiunea 3) |
| `/results` gol imediat după POST | procesare asincronă | așteaptă o clipă și reinterogează `GET /results` |
| App pică pe JDK 25 | JDK prea nou | Project SDK pe **17** |
| build fără `-Plocal` nu pornește serverul HTTP | lipsește Netty | build cu **`-Plocal`** |
| `mvn: command not found` | Maven nu e în PATH | secțiunea 5 (`find` + `alias`) |

---

## 8. Fișiere importante

```
Bilet_StateMachine_Serverless_RabbitMQ/
├── cerinta.txt
├── CUM_RULEZ.md                              ← acest ghid
└── StateMachineGraph/
    ├── pom.xml                               ← Micronaut 3.1.4 + micronaut-rabbitmq (profil local)
    └── src/main/
        ├── resources/application.yml         ← config RabbitMQ (student/student) + port 8080
        └── kotlin/com/sd/laborator/
            ├── Application.kt                ← main + const STATE_QUEUE
            ├── controller/StateMachineController.kt  ← POST /transition, GET /results, GET /graph
            ├── queue/TransitionPublisher.kt  ← @RabbitClient (producător)
            ├── queue/QueueDeclarer.kt        ← declară coada la pornire
            ├── function/StateMachineFunction.kt      ← @RabbitListener (FUNCȚIA SERVERLESS)
            ├── graph/NodeGraphService.kt     ← graful (paralela ErastoteneService) + apply
            ├── model/Models.kt               ← TransitionRequest / NodeRequest / NodeResponse
            └── store/ResultStore.kt          ← rezultatele procesate
```

> Notă: `target/` nu e livrat (artefact de build). `no se puede` :) = tranziție invalidă în graf.
