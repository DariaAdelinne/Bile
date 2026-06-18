# Problema 08 — Vreme (Lab 3) încapsulat + serviciu de replicare (×N) + vizualizare cu GUI PyQt (TCP)

**Tip proiect:** 3 microservicii **Kotlin TCP** (Maven, fat-JAR) + 1 **GUI PyQt5** (Python). Toate comunică prin **TCP**. Fără broker, fără cheie API.

> Pornit de la aplicația de monitorizare a vremei din **Laboratorul 3** (metaweather.com e închis → **Open-Meteo**).

---

## 1. Ce face

Aplicația arată vremea pentru un oraș, dar serviciile sunt **separate** (fiecare „în mașina lui”) și comunică prin **TCP**:

```
[PyQt GUI]
   │  TCP: GET <oras> / HISTORY
   ▼
[VisualizationService :1700]   backend pentru GUI; ține ISTORICUL elementelor citite
   │  TCP: GET <oras>
   ▼
[ReplicationService :1500]     MODELUL DE REPLICARE: un singur punct de acces peste cele N replici;
   │  TCP: GET <oras>          distribuie cererile round-robin + FAILOVER dacă o replică pică
   ▼
[WeatherAppController × N : 1601, 1602, 1603]   serviciul principal Lab 3, încapsulat, în N replici
   ▼
Open-Meteo (geocodare + vreme curentă, gratuit, fără cheie)   [+ fallback local dacă nu e internet]
```

Răspunsul conține **ID-ul replicii** care a răspuns, ca să se vadă distribuirea (round-robin) și failover-ul.

**Verificat (rulat efectiv E2E, cu Open-Meteo live):**
- 3 replici + ReplicationService + VisualizationService porniți; interogări prin `weather_client.py`
  (exact codul de rețea folosit de GUI) pentru Bucuresti/Cluj/London/Paris → date meteo reale,
  **răspuns rotit pe replica #1 → #2 → #3 → #1** (round-robin); istoricul (HISTORY) corect.
- **Failover:** cu doar 2 replici pornite (1603 oprit), când round-robin a nimerit 1603 →
  `connect timed out` → `FAILOVER -> raspuns servit de localhost:1601` (cererea a reușit oricum).
- GUI PyQt: verificat sintactic (`py_compile`); logica de rețea testată headless prin `weather_client.py`.

---

## 2. ⚠️ De citit înainte — ce ai nevoie

| Componentă | Necesar |
|---|---|
| **Java 17** (JDK) | da — ⚠️ **NU JDK 25** (prea nou pentru Kotlin 1.6 / coroutines 1.6) |
| Setare SDK în IntelliJ | **File → Project Structure (Ctrl+Alt+Shift+S) → Project → SDK → 17** |
| Maven | da (e în IntelliJ) |
| **Python 3 + PyQt5** | da, pentru GUI: `pip install PyQt5` (pe Debian, dacă lipsește: `sudo apt install python3-pyqt5`) |
| Internet | recomandat (Open-Meteo). **Fără internet** merge oricum, cu fallback local (date demo) |

Sunt **4 procese**: WeatherAppController (rulează N replici), ReplicationService, VisualizationService, GUI PyQt.
Fiecare microserviciu = proiect Maven separat (= „mașina lui virtuală”).

---

## 3. ✅ Cum rulez — DIN BUTOANE (varianta recomandată)

### Pas A — Deschide proiectele Kotlin
1. **File → Open** → selectează folderul biletului (cele 3 subfoldere `*Service` / `*Controller`).
2. IntelliJ detectează **3 proiecte Maven**. Așteaptă sincronizarea. Verifică SDK pe **17** (secțiunea 2).

### Pas B — Pornește serviciile cu ▶ (în ordinea asta!)
1. **Replicile** — `WeatherAppController.kt` → **▶ Run**. Implicit pornește **3 replici** pe porturile
   1601, 1602, 1603 (mesajul `Pornesc 3 replici ...`).
   *(Opțional: Edit Configurations → Program arguments `1601 3` ca să schimbi base-port/număr.)*
2. **ReplicationService** — `ReplicationService.kt` → **▶ Run** (`Pornit pe portul 1500 (peste 3 replici)`).
3. **VisualizationService** — `VisualizationService.kt` → **▶ Run** (`Pornit pe portul 1700`).

### Pas C — Pornește GUI-ul PyQt (în PyCharm)
1. **PyCharm → Open** folderul `PyQtGui`.
2. Instalează PyQt5: **File → Settings → Project → Python Interpreter → +** → `PyQt5` (sau terminal: `pip install PyQt5`).
3. Deschide `weather_gui.py` → **▶ Run**.
4. În fereastră: scrie un oraș (ex. `Cluj`) → **Vezi vremea**. Apar: locație, temperatură, vânt, condiții,
   ora și **„Răspuns de la: Replica #N”**. Jos se vede **istoricul** elementelor citite.
   Rulează mai multe orașe ca să vezi cum se rotește replica (round-robin).

> Important: GUI-ul trebuie pornit **după** cele 3 servicii Kotlin (se conectează la `localhost:1700`).

### Demonstrația de FAILOVER (opțional, pentru prezentare)
Pornește `WeatherAppController` cu argumente `1601 2` (doar 2 replici). ReplicationService caută implicit
și 1603 (care lipsește): când îi vine rândul, dă timeout și **face failover** pe replica următoare —
cererea reușește oricum. (Vezi în consola ReplicationService liniile `indisponibila` + `FAILOVER`.)

---

## 4. 🔧 Cum rulez — DIN TERMINAL (rezervă)

```bash
# 1. Găsește Maven (pe Debian e în IntelliJ)
find / -name "mvn" 2>/dev/null | grep -v proc | head -5
chmod +x CALEA_AFIȘATĂ
alias mvn='CALEA_AFIȘATĂ'

# 2. Compilează cele 3 microservicii Kotlin (fat-JAR)
cd WeatherAppController  && mvn clean package -DskipTests && cd ..
cd ReplicationService    && mvn clean package -DskipTests && cd ..
cd VisualizationService  && mvn clean package -DskipTests && cd ..

# 3. Pornește în ordine (fiecare în terminalul lui):
java -jar WeatherAppController/target/WeatherAppController-1.0-SNAPSHOT-jar-with-dependencies.jar 1601 3
java -jar ReplicationService/target/ReplicationService-1.0-SNAPSHOT-jar-with-dependencies.jar
java -jar VisualizationService/target/VisualizationService-1.0-SNAPSHOT-jar-with-dependencies.jar

# 4. GUI PyQt
cd PyQtGui
pip install PyQt5            # o singură dată
python weather_gui.py
# test rapid fără GUI (același cod de rețea):
python weather_client.py Cluj
```

**Replici în mașini/procese separate** (modelul de replicare „real”): rulează `WeatherAppController`
de N ori cu `<basePort> 1` pe porturi diferite (sau pe VM-uri diferite), apoi pornește
ReplicationService cu `REPLICA_ENDPOINTS=host1:1601,host2:1601,...` (variabilă de mediu).

---

## 5. Structura + SOLID (pentru prezentare)

### Diagrama de servicii
`PyQt GUI → VisualizationService → ReplicationService → WeatherAppController × N → Open-Meteo`,
toate prin **TCP**, fiecare serviciu independent (în mașina lui virtuală).

### Diagrama de clase (pe serviciu)
- **WeatherAppController**: `WeatherAppController` (serverul TCP al unei replici) + `OpenMeteoWeatherService`
  (obținerea datelor meteo + fallback) — **rețea separată de logica meteo**.
- **ReplicationService**: `ReplicationService` (serverul TCP, punctul de acces) + `ReplicaPool`
  (selecția replicii round-robin + failover) — **modelul de replicare izolat într-o clasă**.
- **VisualizationService**: `VisualizationService` (backend GUI + istoric).
- **GUI**: `weather_gui.py` (PyQt) + `weather_client.py` (rețeaua, testabilă separat).

### SOLID
- **S (Single Responsibility):** fiecare microserviciu un singur rol (date meteo / replicare / vizualizare / GUI);
  la nivel de clasă: `OpenMeteoWeatherService` separă API-ul de server; `ReplicaPool` separă politica de
  replicare de serverul TCP; `weather_client.py` separă rețeaua de interfața grafică.
- **O (Open/Closed):** numărul de replici și endpoint-urile sunt configurabile (argumente / env), fără a
  schimba codul; poți adăuga replici fără să atingi VisualizationService sau GUI.
- **L / I:** contractul dintre servicii e un **protocol de mesaj** simplu (`GET`, `OK ...|...`, `HISTORY`).
- **D (Dependency Inversion):** ReplicationService depinde de abstractizarea `ReplicaPool` (nu de o replică
  anume); GUI depinde de `weather_client` (nu de detaliile socket).

### Model de proiectare: **Replicare** (replication)
`ReplicaPool` face cele N replici identice să apară ca un singur serviciu fiabil: **distribuie încărcarea**
(round-robin) și **tolerează căderea** unei replici (failover). E exact „modelul de replicare” cerut.

---

## 6. Erori frecvente + rezolvare

| Simptom | Cauză | Rezolvare |
|---|---|---|
| GUI: „Nu mă pot conecta la VisualizationService” | serviciile Kotlin nu sunt pornite | pornește întâi cele 3 servicii (replici → replicare → vizualizare) |
| `toate replicile au esuat` | niciun WeatherAppController pornit | pornește `WeatherAppController` (1601 3) |
| `ModuleNotFoundError: PyQt5` | PyQt5 neinstalat | `pip install PyQt5` (Debian: `sudo apt install python3-pyqt5`) |
| temperaturi „demo”, source=`local-fallback` | nu e internet | normal — fallback local; cu internet apar date reale (source=`open-meteo`) |
| oraș greșit găsit (ex. „Bucureștii Noi”) | geocodarea ia prima potrivire | scrie mai specific (ex. `Bucharest`) |
| App pică pe JDK 25 | JDK prea nou | setează Project SDK pe **17** (secțiunea 2) |
| `mvn: command not found` | Maven nu e în PATH | secțiunea 4 (`find` + `alias`) |

---

## 7. Fișiere importante

```
Bilet_Prob08_Weather_Replicare_PyQt/
├── cerinta.txt
├── CUM_RULEZ.md                                  ← acest ghid
├── WeatherAppController/                          (serviciul principal Lab 3, încapsulat; rulează N replici)
│   └── src/main/kotlin/com/sd/laborator/
│       ├── WeatherAppController.kt               ← server TCP per replică + main(basePort, count)
│       └── OpenMeteoWeatherService.kt            ← geocodare + vreme (Open-Meteo) + fallback local
├── ReplicationService/                            (modelul de replicare)
│   └── src/main/kotlin/com/sd/laborator/
│       ├── ReplicationService.kt                 ← punct unic de acces (TCP :1500)
│       └── ReplicaPool.kt                        ← round-robin + failover peste replici
├── VisualizationService/                          (backend pentru GUI)
│   └── src/main/kotlin/com/sd/laborator/
│       └── VisualizationService.kt               ← TCP :1700, istoricul elementelor citite
└── PyQtGui/                                        (interfața grafică)
    ├── weather_gui.py                            ← fereastra PyQt5
    └── weather_client.py                         ← clientul TCP (rețea, testabil headless)
```

> Notă: nu se livrează artefacte generate (`target/`, `__pycache__/`). Datele meteo vin live de la
> Open-Meteo; fără internet se folosesc date demo locale.
