# Bilet (PDF) — Lab 8: Chat + procesor de flux care filtrează după gama de porturi → fișier

**Tip proiect:** Kotlin TCP sockets — **3 microservicii separate** (Maven, fat-JAR) — fără broker

> Pornit de la chat-ul din Laboratorul 8.

---

## 1. Ce face

Față de chat-ul din laborator s-a adăugat un **procesor de flux** (`FilterProcessor`) care
**monitorizează toată comunicația** și o **filtrează după gama de porturi** `[min, max]`:
mesajele venite de pe un port sursă din interval sunt **salvate într-un fișier local**
(`filtered_messages.log`), restul sunt respinse.

```
[Student] --(MSG)--> [MessageManager :1500] --(MSG <portSursa> text)--> abonați
   (port sursă fix)        broadcast etichetat cu portul sursă            │
                                                                          ▼
                                              [FilterProcessor]  acceptă dacă port ∈ [min,max]
                                                      └──▶ filtered_messages.log
```

Verificat: filtru `6000-6010` → mesajele de pe 6001 și 6005 sunt acceptate (scrise în fișier),
cel de pe 7000 e respins.

---

## 2. ⚠️ De citit înainte — ce ai nevoie

| Componentă | Necesar |
|---|---|
| **Java 17** (JDK) | da — ⚠️ NU **JDK 25** (prea nou pentru versiunile din proiect: Kotlin 1.6 / coroutines 1.6) |
| Setare SDK în IntelliJ | **File → Project Structure (Ctrl+Alt+Shift+S) → Project → SDK → 17** (și Language level 17) |
| Maven | da (e în IntelliJ) |
| Internet la prima compilare | da (descarcă Kotlin + coroutines) |

Fără bază de date, fără broker. Sunt **3 procese**: MessageManager, FilterProcessor și (1+) Student.

---

## 3. ✅ Cum rulez — DIN BUTOANE (varianta recomandată)

În IntelliJ ai nevoie de mai multe ferestre Run.

### Pas A — Deschide proiectul
1. **File → Open** → selectează folderul biletului (cu cele 3 subfoldere `*Microservice`).
2. IntelliJ detectează **3 proiecte Maven**. Așteaptă sincronizarea (jos-dreapta).
   *(Dacă nu apar toate: click dreapta pe fiecare `pom.xml` → Add as Maven Project.)*

### Pas B — Pornește serviciile cu ▶ (în ordine)
1. **MessageManager** — `MessageManagerMicroservice.kt` → **▶ Run** (`Pornit pe portul 1500`).
2. **FilterProcessor** — `FilterProcessorMicroservice.kt` → **▶ Run** o dată, apoi
   **Run → Edit Configurations…** și pune la **Program arguments** intervalul, ex: `6000 6010`
   (sau lasă gol pentru implicit 6000-6010). Apasă din nou **▶**.
3. **Studenți** — `StudentMicroservice.kt` → **▶ Run** o dată, apoi **Edit Configurations…**:
   - copiază configul pentru fiecare student și pune **Program arguments**:
     - `Andrei 6001 Salut`  (port în interval → acceptat)
     - `Bianca 6005 Mesaj`  (port în interval → acceptat)
     - `Cosmin 7000 Test`   (port în afara intervalului → respins)
   - (bifează „Allow multiple instances" dacă vrei mai mulți simultan)

### Pas C — Vezi rezultatul
- În consola **FilterProcessor** apar liniile `ACCEPTAT (port ...)` / `RESPINS (port ...)`.
- Fișierul **`filtered_messages.log`** (în directorul de lucru al FilterProcessor) conține doar mesajele acceptate:
```
port=6001 | Salut
port=6005 | Mesaj
```

---

## 4. 🔧 Cum rulez — DIN TERMINAL (rezervă, dacă nu merge din butoane)

```bash
# 1. Găsește Maven
find / -name "mvn" 2>/dev/null | grep -v proc | head -5
chmod +x CALEA_AFIȘATĂ
alias mvn='CALEA_AFIȘATĂ'

# 2. Compilează fiecare microserviciu (fat-JAR)
cd CALE/MessageManagerMicroservice && mvn clean package -DskipTests
cd ../FilterProcessorMicroservice  && mvn clean package -DskipTests
cd ../StudentMicroservice          && mvn clean package -DskipTests

# 3. Pornește în ordine:
# T1 — MessageManager
java -jar MessageManagerMicroservice/target/MessageManagerMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar
# T2 — FilterProcessor cu intervalul [6000, 6010]
java -jar FilterProcessorMicroservice/target/FilterProcessorMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar 6000 6010
# T3..T5 — studenti (nume, port sursa, mesaj)
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Andrei 6001 "Salut"
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Bianca 6005 "Mesaj"
java -jar StudentMicroservice/target/StudentMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar Cosmin 7000 "Test"

# 4. Vezi fisierul (langa locul de unde ai rulat FilterProcessor)
cat filtered_messages.log
```

---

## 5. Structura + SOLID (pentru prezentare)

- **MessageManager** — broker de chat (broadcast), etichetează fiecare mesaj cu portul sursă; folosește **corutine** (o corutină per client).
- **FilterProcessor** (NOU) — procesorul de flux: filtrează după gama de porturi și salvează în fișier.
- **Student** — client care se conectează de pe un **port sursă fix** (ca filtrarea să fie deterministă).
- **SOLID:**
  - **S** — fiecare microserviciu o singură responsabilitate (broadcast / filtrare / client).
  - **O** — regula de filtrare (intervalul) e configurabilă fără a modifica codul.
  - **D** — FilterProcessor și Student depind de protocolul MessageManager (port + format mesaj), nu de implementarea lui.

---

## 6. Erori frecvente + rezolvare

| Simptom | Cauză | Rezolvare |
|---|---|---|
| `Connection refused` | MessageManager nu e pornit | pornește **întâi** MessageManager (1500) |
| `filtered_messages.log` gol | FilterProcessor pornit DUPĂ ce au trimis studenții | pornește FilterProcessor înainte de studenți |
| `Address already in use` (port sursă) | ai repornit un Student pe același port prea repede (TIME_WAIT) | folosește alt port sursă sau așteaptă câteva secunde |
| App pică pe JDK 25 | JDK prea nou | setează Project SDK pe **17** (secțiunea 2) |
| `mvn: command not found` | Maven nu e în PATH | secțiunea 4 (`find` + `alias`) |

---

## 7. Fișiere importante

```
Bilet_Lab8_Filtru_Porturi/
├── cerinta.txt
├── CUM_RULEZ.md                                   ← acest ghid
├── MessageManagerMicroservice/
│   └── src/main/kotlin/com/sd/laborator/MessageManagerMicroservice.kt  ← broadcast + etichetare port sursă (corutine)
├── FilterProcessorMicroservice/
│   └── src/main/kotlin/com/sd/laborator/FilterProcessorMicroservice.kt ← NOU: filtru gama porturi → fișier
└── StudentMicroservice/
    └── src/main/kotlin/com/sd/laborator/StudentMicroservice.kt         ← client (port sursă fix)
```
