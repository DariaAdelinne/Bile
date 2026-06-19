# Explicatie cod — bilet_47 (SPSD-64)

## Ce face aplicatia

Implementeaza un **automat cu stari finit** (FSM) ca microservicii serverless:
- Fiecare stare a automatului este o **functie serverless separata** (`@Component` Spring)
- Datele circula prin **Kafka** — fiecare stare are un **topic Kafka dedicat**
- Utilizatorul introduce un sir de biti via **REST**, automatul il proceseaza asincron
- Rezultatul (output=0 sau output=1) este disponibil via REST dupa procesare

## Automatul — tabel de tranzitii

Automatul detecteaza secventa "11" (doi de unu consecutivi):

| Stare curenta | Input | Stare urmatoare | Output |
|---------------|-------|-----------------|--------|
| S00           | 0     | S00             | 0      |
| S00           | 1     | S01             | 0      |
| S01           | 0     | S10             | 0      |
| S01           | 1     | S11             | **1**  |
| S10           | 0     | S00             | 0      |
| S10           | 1     | S01             | 0      |
| S11           | 0     | S00             | 1      |
| S11           | 1     | S11             | 1      |

## Arhitectura microserviciilor

```
[Utilizator]
     |
     | POST /automaton/submit {"bits":"0110"}
     ↓
[AutomatonController] ──────────────────────────────────────────────
     |                                                              |
     | publish JSON → topic "automaton-s00"                        | GET /result/{id}
     ↓                                                             ↑
[Kafka Broker]                                               [ResultStore]
     |                                                             ↑
     ↓ consume                                                     |
[State00Function] ──bit=0──→ topic "automaton-s00" ──────────────  |
     |                                                             |
     └──bit=1──→ topic "automaton-s01"                            |
                    ↓                                             |
              [State01Function] ──bit=0──→ topic "automaton-s10"  |
                    |                                             |
                    └──bit=1──→ topic "automaton-s11"            |
                                    ↓                            |
                              [State11Function] ──output=1──→ save()
```

## Fisiere si clase

| Fisier | Rol |
|--------|-----|
| `AutomatonApp.kt` | Entry point Spring Boot, `@EnableKafka` |
| `model/AutomatonContext.kt` | DTO transmis in Kafka (JSON) — contine sirul de biti, indexul curent, starea, istoricul |
| `interfaces/IStateFunction.kt` | Interfata serverless — DIP + ISP |
| `functions/KafkaTopics.kt` | Constante pentru numele topic-urilor |
| `functions/State00Function.kt` | Functie serverless stare S00, `@KafkaListener("automaton-s00")` |
| `functions/State01Function.kt` | Functie serverless stare S01 |
| `functions/State10Function.kt` | Functie serverless stare S10 |
| `functions/State11Function.kt` | Functie serverless stare S11 — **output=1** |
| `rest/ResultStore.kt` | Stocare in-memory a rezultatelor finale (`ConcurrentHashMap`) |
| `rest/AutomatonController.kt` | Serviciu REST: submit + interogare rezultate |
| `docker-compose.yml` | Kafka + Zookeeper |

## Principii SOLID

| Principiu | Cum e respectat |
|-----------|-----------------|
| **S** (SRP) | Fiecare functie serverless gestioneaza exact O stare. Controller-ul REST doar publica in Kafka. `ResultStore` doar stocheaza. |
| **O** (OCP) | Adaugarea unei noi stari = nou `@Component IStateFunction` + nou topic Kafka. Zero modificari in codul existent. |
| **L** (LSP) | `State00Function`, `State01Function`, etc. substituie complet `IStateFunction`. |
| **I** (ISP) | `IStateFunction` are o singura metoda `process()` — interfata mica si specializata. |
| **D** (DIP) | `AutomatonController` depinde de `KafkaTemplate` (abstractizare). Functiile serverless depind de `ResultStore` (abstractizare). |

## Modelul serverless

Fiecare functie respecta modelul **Function<Input, Output>**:
- **Input**: mesaj JSON din Kafka (`String` → `AutomatonContext`)
- **Procesare**: citeste bitul curent, calculeaza starea urmatoare
- **Output**: publica noul context pe topic-ul starii urmatoare (sau salveaza in `ResultStore`)

Acesta este echivalentul unui **AWS Lambda** sau **Micronaut FunctionBean** — fiecare functie e independenta, stateless, si reactioneaza la un eveniment (mesaj Kafka).

## Fluxul pentru bits="0110"

```
REST POST {"bits":"0110"}
  → Kafka: automaton-s00  {"id":"x","bits":"0110","bitIndex":0,"currentState":"S00"}
  
State00: bit[0]='0' → S00 → Kafka: automaton-s00  (index=1)
State00: bit[1]='1' → S01 → Kafka: automaton-s01  (index=2)
State01: bit[2]='1' → S11 → Kafka: automaton-s11  (index=3)
State11: bit[3]='0' → S00 + output=1 → Kafka: automaton-s00  (index=4)
State00: bit[4]=null → DONE → ResultStore.save(output=1)

REST GET /result/x → {"output":1,"done":true,"finalState":"S00"}
```
